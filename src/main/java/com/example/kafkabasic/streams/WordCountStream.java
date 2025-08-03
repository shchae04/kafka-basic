package com.example.kafkabasic.streams;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.Stores;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 단어 수 세기 예제 - Kafka Streams의 대표적인 예제
 * 입력 토픽에서 텍스트를 읽어 단어별로 분리한 후 각 단어의 출현 횟수를 계산하여 출력 토픽으로 전송
 */
@Component
public class WordCountStream {

    // 입력 및 출력 토픽 이름
    private static final String INPUT_TOPIC = "word-count-input";
    private static final String OUTPUT_TOPIC = "word-count-output";
    
    // 상태 저장소 이름 (REST API에서 조회 가능)
    private static final String COUNTS_STORE = "counts";

    /**
     * Kafka Streams 토폴로지 정의
     * StreamsBuilder는 Spring에 의해 자동으로 주입됨
     */
    @Autowired
    public void buildPipeline(StreamsBuilder streamsBuilder) {
        // 입력 토픽에서 KStream 생성
        KStream<String, String> textLines = streamsBuilder.stream(
                INPUT_TOPIC,
                Consumed.with(Serdes.String(), Serdes.String())
        );

        // 단어 수 세기 로직 구현
        KTable<String, Long> wordCounts = textLines
                // 소문자로 변환
                .mapValues(value -> value.toLowerCase())
                // 단어로 분리 (flatMapValues는 각 입력 값을 여러 출력 값으로 변환)
                .flatMapValues(value -> Arrays.asList(value.split("\\W+")))
                // 빈 단어 필터링
                .filter((key, value) -> !value.isEmpty())
                // 단어를 키로 사용하기 위해 키-값 쌍 재구성
                .selectKey((key, value) -> value)
                // 같은 단어끼리 그룹화
                .groupByKey()
                // 각 단어의 출현 횟수 계산 및 상태 저장소에 저장
                .count(Materialized.<String, Long>as(
                        Stores.persistentKeyValueStore(COUNTS_STORE))
                        .withKeySerde(Serdes.String())
                        .withValueSerde(Serdes.Long()));

        // 결과를 출력 토픽으로 전송
        wordCounts.toStream().to(
                OUTPUT_TOPIC,
                Produced.with(Serdes.String(), Serdes.Long())
        );
    }
}