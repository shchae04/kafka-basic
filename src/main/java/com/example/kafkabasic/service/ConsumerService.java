package com.example.kafkabasic.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class ConsumerService {

    @KafkaListener(topics = "basic-topic", groupId = "basic-group")
    public void listen(String message) {
        System.out.println("Received: " + message);
    }

    @KafkaListener(topics = "transaction-topic", groupId = "basic-group")
    public void listenTransaction(String message) {
        System.out.println("Received from transaction-topic: " + message);
    }
}
