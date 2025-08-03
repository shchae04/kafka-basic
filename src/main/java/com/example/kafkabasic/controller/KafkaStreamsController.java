package com.example.kafkabasic.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Streams 예제와 상호작용하기 위한 REST 컨트롤러
 * 단어 수 세기, 데이터 변환, 트랜잭션 필터링 등의 예제를 위한 API를 제공합니다.
 */
@Tag(name = "Kafka Streams", description = "Kafka Streams 예제 API")
@RestController
@RequestMapping("/api/streams")
public class KafkaStreamsController {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final StreamsBuilderFactoryBean streamsBuilderFactoryBean;

    @Autowired
    public KafkaStreamsController(KafkaTemplate<String, String> kafkaTemplate,
                                 StreamsBuilderFactoryBean streamsBuilderFactoryBean) {
        this.kafkaTemplate = kafkaTemplate;
        this.streamsBuilderFactoryBean = streamsBuilderFactoryBean;
    }

    /**
     * 단어 수 세기 예제에 메시지 전송
     */
    @Operation(
        summary = "단어 수 세기 예제에 메시지 전송",
        description = "텍스트 메시지를 word-count-input 토픽으로 전송하여 단어 수 세기 처리를 시작합니다."
    )
    @PostMapping("/word-count")
    public ResponseEntity<Map<String, String>> sendWordCountMessage(
            @Parameter(description = "처리할 텍스트 메시지") @RequestBody String message) {
        kafkaTemplate.send("word-count-input", message);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "메시지가 word-count-input 토픽으로 전송되었습니다.");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 사용자 데이터 변환 예제에 메시지 전송
     */
    @Operation(
        summary = "사용자 데이터 변환 예제에 메시지 전송",
        description = "JSON 형식의 사용자 데이터를 user-data-input 토픽으로 전송하여 데이터 변환 처리를 시작합니다."
    )
    @PostMapping("/user-data")
    public ResponseEntity<Map<String, String>> sendUserDataMessage(
            @Parameter(description = "변환할 사용자 데이터 (JSON 형식)") @RequestBody String userDataJson) {
        kafkaTemplate.send("user-data-input", userDataJson);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "사용자 데이터가 user-data-input 토픽으로 전송되었습니다.");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 트랜잭션 필터링 예제에 메시지 전송
     */
    @Operation(
        summary = "트랜잭션 필터링 예제에 메시지 전송",
        description = "JSON 형식의 트랜잭션 데이터를 transaction-input 토픽으로 전송하여 금액에 따른 필터링 처리를 시작합니다."
    )
    @PostMapping("/transaction")
    public ResponseEntity<Map<String, String>> sendTransactionMessage(
            @Parameter(description = "필터링할 트랜잭션 데이터 (JSON 형식)") @RequestBody String transactionJson) {
        kafkaTemplate.send("transaction-input", transactionJson);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "트랜잭션 데이터가 transaction-input 토픽으로 전송되었습니다.");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 단어 수 조회 (특정 단어의 카운트 조회)
     * 참고: 이 기능을 사용하려면 WordCountStream에 상태 저장소 설정이 필요합니다.
     */
    @Operation(
        summary = "특정 단어의 출현 횟수 조회",
        description = "단어 수 세기 예제에서 처리된 특정 단어의 출현 횟수를 상태 저장소에서 조회합니다."
    )
    @GetMapping("/word-count/{word}")
    public ResponseEntity<?> getWordCount(
            @Parameter(description = "조회할 단어", example = "kafka") @PathVariable String word) {
        try {
            KafkaStreams kafkaStreams = streamsBuilderFactoryBean.getKafkaStreams();
            if (kafkaStreams == null) {
                return ResponseEntity.badRequest().body("Kafka Streams가 아직 초기화되지 않았습니다.");
            }

            ReadOnlyKeyValueStore<String, Long> counts = kafkaStreams.store(
                    StoreQueryParameters.fromNameAndType("counts", QueryableStoreTypes.keyValueStore())
            );
            
            Long count = counts.get(word.toLowerCase());
            
            Map<String, Object> response = new HashMap<>();
            response.put("word", word);
            response.put("count", count != null ? count : 0);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("상태 저장소 조회 중 오류 발생: " + e.getMessage());
        }
    }

    /**
     * Kafka Streams 상태 정보 조회
     */
    @Operation(
        summary = "Kafka Streams 상태 정보 조회",
        description = "현재 실행 중인 Kafka Streams 애플리케이션의 상태 정보를 조회합니다."
    )
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStreamsStatus() {
        KafkaStreams kafkaStreams = streamsBuilderFactoryBean.getKafkaStreams();
        
        Map<String, Object> status = new HashMap<>();
        status.put("state", kafkaStreams != null ? kafkaStreams.state().name() : "NOT_INITIALIZED");
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * 이벤트 알림 예제에 메시지 전송
     */
    @Operation(
        summary = "이벤트 알림 예제에 메시지 전송",
        description = "JSON 형식의 이벤트 데이터를 events-input 토픽으로 전송하여 중요도에 따른 필터링 처리를 시작합니다."
    )
    @PostMapping("/event")
    public ResponseEntity<Map<String, Object>> sendEventMessage(
            @Parameter(description = "필터링할 이벤트 데이터 (JSON 형식)", 
                       example = "{\"id\":\"evt123\",\"type\":\"alert\",\"priority\":8,\"message\":\"시스템 경고\"}") 
            @RequestBody String eventJson) {
        
        kafkaTemplate.send("events-input", eventJson);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "이벤트 데이터가 events-input 토픽으로 전송되었습니다.");
        
        try {
            // 이벤트 데이터 파싱하여 응답에 포함
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode eventNode = objectMapper.readTree(eventJson);
            
            if (eventNode.has("priority")) {
                int priority = eventNode.get("priority").asInt();
                boolean isImportant = priority > 5;
                
                response.put("priority", priority);
                response.put("isImportant", isImportant);
                response.put("destination", isImportant ? "notifications-output" : "필터링됨");
            }
        } catch (Exception e) {
            // 파싱 오류는 무시하고 기본 응답만 반환
        }
        
        return ResponseEntity.ok(response);
    }
}