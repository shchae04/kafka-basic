package com.example.kafkabasic.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j 설정 클래스
 * 재시도, 서킷 브레이커, 벌크헤드 패턴을 구성합니다.
 */
@Configuration
public class Resilience4jConfig {
    
    private static final Logger log = LoggerFactory.getLogger(Resilience4jConfig.class);
    
    /**
     * Kafka 메시지 처리를 위한 재시도 설정
     * - 최대 3번 재시도
     * - 1초 간격으로 시작하여 2배씩 증가 (지수 백오프)
     * - 최대 대기 시간 10초
     * - 특정 예외는 재시도하지 않음
     */
    @Bean
    public RetryRegistry kafkaRetryRegistry() {
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .retryExceptions(RuntimeException.class)
                .ignoreExceptions(IllegalArgumentException.class)
                .intervalFunction(attempt -> Math.min(
                        Duration.ofMillis(1000).toMillis() * (long) Math.pow(2, attempt - 1),
                        Duration.ofMillis(10000).toMillis()))
                .failAfterMaxAttempts(true)
                .build();
        
        return RetryRegistry.of(retryConfig);
    }
    
    /**
     * Kafka 메시지 처리를 위한 서킷 브레이커 설정
     * - 50% 실패율에 서킷 오픈
     * - 최소 10번의 호출 후 실패율 계산
     * - 30초 동안 서킷 오픈 상태 유지
     * - 서킷 하프 오픈 상태에서 5번의 호출 허용
     */
    @Bean
    public CircuitBreakerRegistry kafkaCircuitBreakerRegistry() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .minimumNumberOfCalls(10)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(5)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .build();
        
        return CircuitBreakerRegistry.of(circuitBreakerConfig);
    }
    
    /**
     * Kafka 메시지 처리를 위한 벌크헤드 설정
     * - 최대 20개의 동시 호출 허용
     * - 1초의 대기 시간
     */
    @Bean
    public BulkheadRegistry kafkaBulkheadRegistry() {
        BulkheadConfig bulkheadConfig = BulkheadConfig.custom()
                .maxConcurrentCalls(20)
                .maxWaitDuration(Duration.ofSeconds(1))
                .build();
        
        return BulkheadRegistry.of(bulkheadConfig);
    }
}