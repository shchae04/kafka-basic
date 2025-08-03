package com.example.kafkabasic.streams;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Branched;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 트랜잭션 필터링 예제 - 금액에 따라 트랜잭션을 필터링하여 다른 토픽으로 라우팅
 * 입력 토픽에서 트랜잭션 데이터를 읽어 금액에 따라 대/중/소 금액 토픽으로 분류
 */
@Component
public class TransactionFilterStream {

    // 입력 및 출력 토픽 이름
    private static final String INPUT_TOPIC = "transaction-input";
    private static final String HIGH_AMOUNT_TOPIC = "high-amount-transactions";
    private static final String MEDIUM_AMOUNT_TOPIC = "medium-amount-transactions";
    private static final String LOW_AMOUNT_TOPIC = "low-amount-transactions";
    
    // 금액 기준
    private static final double HIGH_AMOUNT_THRESHOLD = 1000000.0; // 100만원 이상
    private static final double MEDIUM_AMOUNT_THRESHOLD = 100000.0; // 10만원 이상
    
    // JSON 처리를 위한 ObjectMapper
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Kafka Streams 토폴로지 정의
     * StreamsBuilder는 Spring에 의해 자동으로 주입됨
     */
    @Autowired
    public void buildPipeline(StreamsBuilder streamsBuilder) {
        // 입력 토픽에서 KStream 생성
        KStream<String, String> transactionStream = streamsBuilder.stream(
                INPUT_TOPIC,
                Consumed.with(Serdes.String(), Serdes.String())
        );

        // 트랜잭션 금액에 따라 스트림 분기
        transactionStream
            .split()
            .branch((key, value) -> isHighAmountTransaction(value), 
                    Branched.withConsumer(ks -> ks.to(HIGH_AMOUNT_TOPIC, 
                            Produced.with(Serdes.String(), Serdes.String()))))
            .branch((key, value) -> isMediumAmountTransaction(value), 
                    Branched.withConsumer(ks -> ks.to(MEDIUM_AMOUNT_TOPIC, 
                            Produced.with(Serdes.String(), Serdes.String()))))
            .defaultBranch(
                    Branched.withConsumer(ks -> ks.to(LOW_AMOUNT_TOPIC, 
                            Produced.with(Serdes.String(), Serdes.String()))));
    }

    /**
     * 고액 트랜잭션 여부 확인 (100만원 이상)
     */
    private boolean isHighAmountTransaction(String transactionJson) {
        return getTransactionAmount(transactionJson) >= HIGH_AMOUNT_THRESHOLD;
    }

    /**
     * 중간 금액 트랜잭션 여부 확인 (10만원 이상 100만원 미만)
     */
    private boolean isMediumAmountTransaction(String transactionJson) {
        double amount = getTransactionAmount(transactionJson);
        return amount >= MEDIUM_AMOUNT_THRESHOLD && amount < HIGH_AMOUNT_THRESHOLD;
    }

    /**
     * 트랜잭션 JSON에서 금액 추출
     * 입력 JSON 예시: {"transactionId": "tx123", "amount": 500000, "timestamp": "2025-08-02T10:30:00", "userId": "user456"}
     */
    private double getTransactionAmount(String transactionJson) {
        try {
            JsonNode transactionNode = objectMapper.readTree(transactionJson);
            return transactionNode.path("amount").asDouble(0.0);
        } catch (JsonProcessingException e) {
            return 0.0; // 파싱 오류 시 0으로 처리
        }
    }
}