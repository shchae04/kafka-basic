package com.example.kafkabasic.streams;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 데이터 변환 예제 - JSON 형식의 메시지를 다른 형식으로 변환
 * 입력 토픽에서 JSON 형식의 사용자 데이터를 읽어 필요한 필드만 추출하여 새로운 형식으로 변환 후 출력 토픽으로 전송
 */
@Component
public class DataTransformationStream {

    // 입력 및 출력 토픽 이름
    private static final String INPUT_TOPIC = "user-data-input";
    private static final String OUTPUT_TOPIC = "user-data-transformed";
    
    // JSON 처리를 위한 ObjectMapper
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Kafka Streams 토폴로지 정의
     * StreamsBuilder는 Spring에 의해 자동으로 주입됨
     */
    @Autowired
    public void buildPipeline(StreamsBuilder streamsBuilder) {
        // 입력 토픽에서 KStream 생성
        KStream<String, String> userDataStream = streamsBuilder.stream(
                INPUT_TOPIC,
                Consumed.with(Serdes.String(), Serdes.String())
        );

        // 데이터 변환 로직 구현
        KStream<String, String> transformedStream = userDataStream
                .mapValues(this::transformUserData);

        // 결과를 출력 토픽으로 전송
        transformedStream.to(
                OUTPUT_TOPIC,
                Produced.with(Serdes.String(), Serdes.String())
        );
    }

    /**
     * 사용자 데이터 변환 메서드
     * 입력 JSON 예시: {"id": "123", "name": "홍길동", "email": "hong@example.com", "age": 30, "address": "서울시"}
     * 출력 JSON 예시: {"userId": "123", "displayName": "홍길동", "contactInfo": {"email": "hong@example.com"}}
     */
    private String transformUserData(String userDataJson) {
        try {
            // 입력 JSON 파싱
            JsonNode userNode = objectMapper.readTree(userDataJson);
            
            // 새로운 JSON 객체 생성
            ObjectNode transformedNode = JsonNodeFactory.instance.objectNode();
            
            // 필요한 필드 추출 및 변환
            transformedNode.put("userId", userNode.path("id").asText());
            transformedNode.put("displayName", userNode.path("name").asText());
            
            // 중첩 객체 생성
            ObjectNode contactInfo = transformedNode.putObject("contactInfo");
            contactInfo.put("email", userNode.path("email").asText());
            
            // JSON 문자열로 변환하여 반환
            return objectMapper.writeValueAsString(transformedNode);
        } catch (JsonProcessingException e) {
            // 오류 발생 시 원본 데이터 반환
            return userDataJson;
        }
    }
}