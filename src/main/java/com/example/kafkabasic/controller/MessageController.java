package com.example.kafkabasic.controller;

import com.example.kafkabasic.service.ProducerService;
import com.example.kafkabasic.service.TransactionalProducerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api")
public class MessageController {

    private final ProducerService producerService;
    private final TransactionalProducerService transactionalProducerService;

    public MessageController(ProducerService producerService, 
                            TransactionalProducerService transactionalProducerService) {
        this.producerService = producerService;
        this.transactionalProducerService = transactionalProducerService;
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendMessage(@RequestParam String msg) {
        producerService.sendMessage("basic-topic", msg);
        return ResponseEntity.ok("Message sent: " + msg);
    }
    
    /**
     * 여러 메시지를 하나의 트랜잭션으로 전송하는 엔드포인트
     * 
     * @param messages 쉼표로 구분된 메시지 목록
     * @return 트랜잭션 성공/실패 결과
     */
    @PostMapping("/send-transaction")
    public ResponseEntity<String> sendMessagesInTransaction(@RequestParam String messages) {
        List<String> messageList = Arrays.asList(messages.split(","));
        boolean success = transactionalProducerService.sendMessagesInTransaction("transaction-topic", messageList);
        
        if (success) {
            return ResponseEntity.ok("Transaction successful. All " + messageList.size() + " messages sent.");
        } else {
            return ResponseEntity.internalServerError().body("Transaction failed. No messages were sent.");
        }
    }
    
    /**
     * 트랜잭션 실패를 시뮬레이션하는 엔드포인트
     * 
     * @param messages 쉼표로 구분된 메시지 목록
     * @param failAfter 몇 번째 메시지 후에 실패할지 지정
     * @return 트랜잭션 실패 결과
     */
    @PostMapping("/simulate-transaction-failure")
    public ResponseEntity<String> simulateTransactionFailure(
            @RequestParam String messages,
            @RequestParam(defaultValue = "1") int failAfter) {
        
        List<String> messageList = Arrays.asList(messages.split(","));
        transactionalProducerService.simulateTransactionFailure("transaction-topic", messageList, failAfter);
        
        return ResponseEntity.ok("Transaction simulation completed. " +
                "Transaction was rolled back after " + failAfter + " messages.");
    }
}
