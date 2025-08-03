package com.example.kafkabasic.health;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.streams.KafkaStreams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Kafka 브로커 및 Streams 상태를 모니터링하는 커스텀 헬스 인디케이터
 * Spring Actuator의 /actuator/health 엔드포인트에서 사용됩니다.
 */
@Component
public class KafkaHealthIndicator implements HealthIndicator {

    private final KafkaAdmin kafkaAdmin;
    private final StreamsBuilderFactoryBean streamsBuilderFactoryBean;

    @Autowired
    public KafkaHealthIndicator(KafkaAdmin kafkaAdmin, StreamsBuilderFactoryBean streamsBuilderFactoryBean) {
        this.kafkaAdmin = kafkaAdmin;
        this.streamsBuilderFactoryBean = streamsBuilderFactoryBean;
    }

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        
        // Kafka 브로커 연결 상태 확인
        boolean brokerHealthy = checkBrokerHealth(details);
        
        // Kafka Streams 상태 확인
        boolean streamsHealthy = checkStreamsHealth(details);
        
        // 전체 상태 결정
        if (brokerHealthy && streamsHealthy) {
            return Health.up().withDetails(details).build();
        } else {
            return Health.down().withDetails(details).build();
        }
    }

    /**
     * Kafka 브로커 연결 상태 확인
     * 
     * @param details 상태 세부 정보를 담을 맵
     * @return 브로커 연결 상태 (true: 정상, false: 비정상)
     */
    private boolean checkBrokerHealth(Map<String, Object> details) {
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            // 토픽 목록 조회 시도 (5초 타임아웃)
            ListTopicsResult topics = adminClient.listTopics();
            topics.names().get(5, TimeUnit.SECONDS);
            
            details.put("broker.status", "UP");
            details.put("broker.connection", "Connected to Kafka broker");
            return true;
        } catch (Exception e) {
            details.put("broker.status", "DOWN");
            details.put("broker.error", e.getMessage());
            return false;
        }
    }

    /**
     * Kafka Streams 상태 확인
     * 
     * @param details 상태 세부 정보를 담을 맵
     * @return Streams 상태 (true: 정상, false: 비정상)
     */
    private boolean checkStreamsHealth(Map<String, Object> details) {
        try {
            KafkaStreams kafkaStreams = streamsBuilderFactoryBean.getKafkaStreams();
            
            if (kafkaStreams == null) {
                details.put("streams.status", "NOT_INITIALIZED");
                return false;
            }
            
            KafkaStreams.State state = kafkaStreams.state();
            details.put("streams.status", state.name());
            
            // RUNNING 상태인 경우만 정상으로 간주
            return state == KafkaStreams.State.RUNNING;
        } catch (Exception e) {
            details.put("streams.status", "ERROR");
            details.put("streams.error", e.getMessage());
            return false;
        }
    }
}