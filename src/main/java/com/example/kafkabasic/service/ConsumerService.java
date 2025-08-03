package com.example.kafkabasic.service;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Kafka 메시지 소비자 서비스
 * 다양한 토픽의 메시지를 처리하고 오류 처리 및 재시도 메커니즘을 시연합니다.
 * Resilience4j를 사용하여 재시도, 서킷 브레이커, 벌크헤드 패턴을 적용합니다.
 */
@Service
public class ConsumerService {
    
    private static final Logger log = LoggerFactory.getLogger(ConsumerService.class);

    /**
     * 기본 토픽 리스너
     * 모든 메시지를 정상적으로 처리합니다.
     */
    @KafkaListener(topics = "basic-topic", groupId = "basic-group")
    public void listen(String message) {
        log.info("기본 토픽에서 메시지 수신: {}", message);
    }

    /**
     * 트랜잭션 토픽 리스너
     * 모든 메시지를 정상적으로 처리합니다.
     */
    @KafkaListener(topics = "transaction-topic", groupId = "basic-group")
    public void listenTransaction(String message) {
        log.info("트랜잭션 토픽에서 메시지 수신: {}", message);
    }
    
    /**
     * 오류 시연용 리스너 (Resilience4j 적용)
     * "error"를 포함하는 메시지를 처리할 때 예외를 발생시켜 재시도 및 DLQ 메커니즘을 시연합니다.
     * 
     * 이 리스너는 다음과 같이 동작합니다:
     * 1. "error"를 포함하는 메시지: RuntimeException 발생 -> Resilience4j 재시도 -> DLQ로 전송
     * 2. "invalid"를 포함하는 메시지: IllegalArgumentException 발생 -> 재시도 없이 바로 DLQ로 전송
     * 3. 그 외 메시지: 정상 처리
     * 
     * Resilience4j 패턴 적용:
     * - @Retry: 최대 3번 재시도, 지수 백오프 적용
     * - @CircuitBreaker: 50% 실패율에 서킷 오픈
     * - @Bulkhead: 최대 20개의 동시 호출 허용
     */
    @Retry(name = "kafkaConsumer", fallbackMethod = "fallbackForKafkaConsumer")
    @CircuitBreaker(name = "kafkaConsumer", fallbackMethod = "fallbackForKafkaConsumer")
    @Bulkhead(name = "kafkaConsumer", fallbackMethod = "fallbackForKafkaConsumer")
    @KafkaListener(topics = "user-data-input", groupId = "error-demo-group")
    public void listenWithErrorHandling(String message) {
        log.info("사용자 데이터 토픽에서 메시지 수신: {}", message);
        
        // "error"를 포함하는 메시지는 RuntimeException 발생 (재시도 후 DLQ로)
        if (message.contains("error")) {
            log.error("오류 메시지 감지: {}", message);
            throw new RuntimeException("메시지 처리 중 오류 발생: " + message);
        }
        
        // "invalid"를 포함하는 메시지는 IllegalArgumentException 발생 (재시도 없이 바로 DLQ로)
        if (message.contains("invalid")) {
            log.error("유효하지 않은 메시지 감지: {}", message);
            throw new IllegalArgumentException("유효하지 않은 메시지: " + message);
        }
        
        // 정상 메시지 처리
        log.info("메시지 정상 처리 완료: {}", message);
    }
    
    /**
     * Resilience4j 폴백 메서드
     * 모든 재시도가 실패하거나 서킷이 열렸을 때 호출됩니다.
     * 이 메서드는 DLQ로 메시지를 전송하는 로직을 포함할 수 있습니다.
     */
    public void fallbackForKafkaConsumer(String message, Exception ex) {
        log.error("Resilience4j 폴백 처리: 메시지={}, 예외={}", message, ex.getMessage());
        // 여기서 DLQ로 메시지를 직접 전송하는 로직을 추가할 수 있습니다.
        // 하지만 현재는 KafkaConfig의 errorHandler가 이 역할을 담당합니다.
    }
}
