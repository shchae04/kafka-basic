package com.example.kafkabasic.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture;

@Service
public class ProducerService {
    private final KafkaTemplate<String, String> kafkaTemplate;

    public ProducerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(String topic, String message) {
        // Spring Boot 3.x에서는 CompletableFuture를 반환
        CompletableFuture<SendResult<String, String>> future =
                kafkaTemplate.send(topic, message);

        // CompletableFuture의 whenComplete 메서드 사용
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                System.out.println("Sent: " + message +
                        " with offset: " + result.getRecordMetadata().offset());
            } else {
                System.err.println("Failed to send message: " + ex.getMessage());
            }
        });
    }
}