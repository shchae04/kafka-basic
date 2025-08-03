package com.example.kafkabasic.streams;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 이벤트 알림 시스템 예제 - 중요도(priority)에 따라 이벤트를 필터링
 * 입력 토픽에서 이벤트 데이터를 읽어 중요도가 높은 이벤트만 출력 토픽으로 전송
 */
@Component
public class NotificationStream {

    private static final Logger logger = LoggerFactory.getLogger(NotificationStream.class);
    
    // 입력 및 출력 토픽 이름
    private static final String INPUT_TOPIC = "events-input";
    private static final String OUTPUT_TOPIC = "notifications-output";
    
    // 중요 이벤트 기준 (priority 값이 5보다 큰 경우)
    private static final int PRIORITY_THRESHOLD = 5;
    
    // JSON 처리를 위한 ObjectMapper
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Kafka Streams 토폴로지 정의
     * StreamsBuilder는 Spring에 의해 자동으로 주입됨
     */
    @Autowired
    public void buildPipeline(StreamsBuilder streamsBuilder) {
        // 입력 토픽에서 KStream 생성
        KStream<String, String> eventStream = streamsBuilder.stream(
                INPUT_TOPIC,
                Consumed.with(Serdes.String(), Serdes.String())
        );
        
        // 중요 이벤트만 필터링
        KStream<String, String> importantEvents = eventStream
                .filter((key, value) -> isImportantEvent(value));
        
        // 필터링된 이벤트를 출력 토픽으로 전송
        importantEvents.to(
                OUTPUT_TOPIC,
                Produced.with(Serdes.String(), Serdes.String())
        );
        
        // 로깅
        importantEvents.foreach((key, value) -> 
                logger.info("중요 이벤트 감지: {}", value));
    }
    
    /**
     * 중요 이벤트 여부 확인 (priority 값이 임계값보다 큰 경우)
     * 
     * @param eventJson 이벤트 JSON 문자열
     * @return 중요 이벤트 여부
     */
    private boolean isImportantEvent(String eventJson) {
        try {
            JsonNode eventNode = objectMapper.readTree(eventJson);
            
            // priority 필드가 있고, 값이 임계값보다 큰 경우
            if (eventNode.has("priority")) {
                int priority = eventNode.get("priority").asInt(0);
                return priority > PRIORITY_THRESHOLD;
            }
            
            return false;
        } catch (Exception e) {
            logger.error("이벤트 파싱 중 오류 발생: {}", e.getMessage());
            return false;
        }
    }
}