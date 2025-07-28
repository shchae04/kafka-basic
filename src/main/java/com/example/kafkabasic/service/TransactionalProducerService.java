
package com.example.kafkabasic.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class TransactionalProducerService {

    private final KafkaTemplate<String, String> transactionalKafkaTemplate;

    public TransactionalProducerService(@Qualifier("transactionalKafkaTemplate") KafkaTemplate<String, String> transactionalKafkaTemplate) {
        this.transactionalKafkaTemplate = transactionalKafkaTemplate;
    }

    @Transactional("kafkaTransactionManager")
    public boolean sendMessagesInTransaction(String topic, List<String> messages) {
        try {
            for (String message : messages) {
                CompletableFuture<SendResult<String, String>> future =
                        transactionalKafkaTemplate.send(topic, message);

                future.whenComplete((result, ex) -> {
                    if (ex == null) {
                        System.out.println("Sent in transaction: " + message +
                                " with offset: " + result.getRecordMetadata().offset());
                    } else {
                        System.err.println("Failed to send message in transaction: " + ex.getMessage());
                        throw new RuntimeException("Message sending failed", ex);
                    }
                });
            }
            System.out.println("Transaction committed successfully for " + messages.size() + " messages");
            return true;
        } catch (Exception e) {
            System.err.println("Transaction failed and rolled back: " + e.getMessage());
            throw e; // 트랜잭션 롤백을 위해 예외 재발생
        }
    }

    @Transactional("kafkaTransactionManager")
    public boolean simulateTransactionFailure(String topic, List<String> messages, int failAfter) {
        try {
            for (int i = 0; i < messages.size(); i++) {
                if (i == failAfter) {
                    System.out.println("Simulating failure after " + failAfter + " messages");
                    throw new RuntimeException("Simulated failure");
                }

                String message = messages.get(i);
                transactionalKafkaTemplate.send(topic, message);
                System.out.println("Sent message " + (i + 1) + ": " + message);
            }
            return true;
        } catch (Exception e) {
            System.err.println("Transaction failed and rolled back as expected: " + e.getMessage());
            throw e; // 트랜잭션 롤백
        }
    }
}