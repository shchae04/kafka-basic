package com.example.kafkabasic.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Kafka 토픽 자동 생성 컴포넌트
 * 애플리케이션 시작 시 필요한 토픽들이 존재하는지 확인하고, 없는 경우 자동으로 생성합니다.
 */
@Configuration
public class TopicInitializer {

    private static final Logger logger = LoggerFactory.getLogger(TopicInitializer.class);

    // 기본 파티션 수와 복제 팩터 설정
    private static final int DEFAULT_PARTITIONS = 1;
    private static final short DEFAULT_REPLICATION_FACTOR = 1;
    
    // 데드 레터 큐(DLQ) 접미사
    private static final String DLQ_SUFFIX = ".DLQ";

    // 기본 토픽 목록
    private static final String[] REQUIRED_TOPICS = {
            "word-count-input",
            "user-data-input",
            "events-input",
            "transaction-input",
            "basic-topic"  // ConsumerService에서 사용하는 토픽 추가
    };

    private final KafkaAdmin kafkaAdmin;

    @Autowired
    public TopicInitializer(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    /**
     * 애플리케이션 시작 시 필요한 토픽들을 생성합니다.
     * 각 토픽에 대해 데드 레터 큐(DLQ) 토픽도 함께 생성합니다.
     */
    @PostConstruct
    public void initializeTopics() {
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            // 현재 존재하는 토픽 목록 조회
            Set<String> existingTopics = adminClient.listTopics().names().get();
            logger.info("현재 존재하는 토픽: {}", existingTopics);

            // 생성해야 할 토픽 목록 생성
            List<NewTopic> topicsToCreate = new ArrayList<>();
            
            // 기본 토픽 생성
            for (String topicName : REQUIRED_TOPICS) {
                if (!existingTopics.contains(topicName)) {
                    logger.info("토픽 '{}' 생성 예정", topicName);
                    topicsToCreate.add(
                            TopicBuilder.name(topicName)
                                    .partitions(DEFAULT_PARTITIONS)
                                    .replicas(DEFAULT_REPLICATION_FACTOR)
                                    .build()
                    );
                } else {
                    logger.info("토픽 '{}' 이미 존재함", topicName);
                }
                
                // 각 토픽에 대한 DLQ 토픽 생성
                String dlqTopicName = topicName + DLQ_SUFFIX;
                if (!existingTopics.contains(dlqTopicName)) {
                    logger.info("DLQ 토픽 '{}' 생성 예정", dlqTopicName);
                    topicsToCreate.add(
                            TopicBuilder.name(dlqTopicName)
                                    .partitions(DEFAULT_PARTITIONS)
                                    .replicas(DEFAULT_REPLICATION_FACTOR)
                                    .build()
                    );
                } else {
                    logger.info("DLQ 토픽 '{}' 이미 존재함", dlqTopicName);
                }
            }

            // 필요한 토픽 생성
            if (!topicsToCreate.isEmpty()) {
                adminClient.createTopics(topicsToCreate).all().get();
                logger.info("토픽 생성 완료: {}", topicsToCreate.stream().map(NewTopic::name).toList());
            } else {
                logger.info("생성할 토픽이 없습니다. 모든 필요한 토픽이 이미 존재합니다.");
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("토픽 생성 중 오류 발생", e);
            // 애플리케이션 시작에 치명적인 오류이므로 예외를 다시 던짐
            throw new RuntimeException("토픽 생성 중 오류 발생", e);
        }
    }
}