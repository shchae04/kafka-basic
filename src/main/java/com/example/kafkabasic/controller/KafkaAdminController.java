package com.example.kafkabasic.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Kafka 토픽 관리 및 정보 조회를 위한 REST 컨트롤러
 * 카프카 초보자도 쉽게 토픽 정보를 확인할 수 있는 API를 제공합니다.
 */
@RestController
@RequestMapping("/api/kafka")
@Tag(name = "Kafka Admin", description = "Kafka 토픽 관리 및 정보 조회 API")
public class KafkaAdminController {
    
    private final AdminClient adminClient;
    
    @Autowired
    public KafkaAdminController(KafkaAdmin kafkaAdmin) {
        // KafkaAdmin의 설정을 사용하여 AdminClient 생성
        this.adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties());
    }
    
    /**
     * 모든 Kafka 토픽 목록 조회
     * 
     * @return 토픽 이름 목록
     */
    @Operation(summary = "모든 Kafka 토픽 목록 조회", description = "Kafka 클러스터에 있는 모든 토픽의 이름을 조회합니다.")
    @GetMapping("/topics")
    public ResponseEntity<List<String>> getTopics() throws ExecutionException, InterruptedException {
        // 토픽 목록 조회
        ListTopicsResult topics = adminClient.listTopics();
        Set<String> topicNames = topics.names().get();
        
        // 내부 토픽 필터링 (언더스코어로 시작하는 토픽은 Kafka 내부 토픽)
        List<String> userTopics = topicNames.stream()
                .filter(name -> !name.startsWith("_"))
                .sorted()
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(userTopics);
    }
    
    /**
     * 특정 토픽의 상세 정보 조회
     * 
     * @param topicName 조회할 토픽 이름
     * @return 토픽 상세 정보 (파티션 수, 복제 팩터, 설정 등)
     */
    @Operation(summary = "토픽 상세 정보 조회", description = "특정 토픽의 파티션 수, 복제 팩터, 설정 등 상세 정보를 조회합니다.")
    @GetMapping("/topics/{topicName}")
    public ResponseEntity<Map<String, Object>> getTopicInfo(@PathVariable String topicName) 
            throws ExecutionException, InterruptedException {
        Map<String, Object> topicInfo = new HashMap<>();
        
        // 토픽 설명 가져오기
        DescribeTopicsResult describeTopicsResult = 
                adminClient.describeTopics(Collections.singleton(topicName));
        TopicDescription topicDescription = 
                describeTopicsResult.allTopicNames().get().get(topicName);
        
        // 기본 정보 추출
        topicInfo.put("name", topicDescription.name());
        topicInfo.put("partitions", topicDescription.partitions().size());
        
        // 복제 팩터 계산 (첫 번째 파티션의 복제 수)
        if (!topicDescription.partitions().isEmpty()) {
            int replicationFactor = topicDescription.partitions().get(0).replicas().size();
            topicInfo.put("replicationFactor", replicationFactor);
        }
        
        // 파티션 정보 추출
        List<Map<String, Object>> partitionsInfo = new ArrayList<>();
        for (TopicPartitionInfo partitionInfo : topicDescription.partitions()) {
            Map<String, Object> partition = new HashMap<>();
            partition.put("id", partitionInfo.partition());
            partition.put("leader", partitionInfo.leader().id());
            partition.put("replicas", partitionInfo.replicas().stream()
                    .map(node -> node.id())
                    .collect(Collectors.toList()));
            partitionsInfo.add(partition);
        }
        topicInfo.put("partitionsInfo", partitionsInfo);
        
        // 토픽 설정 가져오기
        ConfigResource configResource = new ConfigResource(
                ConfigResource.Type.TOPIC, topicName);
        DescribeConfigsResult configsResult = 
                adminClient.describeConfigs(Collections.singleton(configResource));
        Config config = configsResult.all().get().get(configResource);
        
        // 중요 설정만 추출
        Map<String, String> topicConfig = new HashMap<>();
        for (ConfigEntry entry : config.entries()) {
            if (entry.source() != ConfigEntry.ConfigSource.DEFAULT_CONFIG) {
                topicConfig.put(entry.name(), entry.value());
            }
        }
        topicInfo.put("config", topicConfig);
        
        return ResponseEntity.ok(topicInfo);
    }
    
    /**
     * 토픽의 메시지 수 추정 (오프셋 기반)
     * 
     * @param topicName 조회할 토픽 이름
     * @return 토픽의 대략적인 메시지 수
     */
    @Operation(summary = "토픽 메시지 수 추정", description = "토픽의 시작 오프셋과 끝 오프셋 차이를 기반으로 메시지 수를 추정합니다.")
    @GetMapping("/topics/{topicName}/messages")
    public ResponseEntity<Map<String, Object>> getTopicMessageCount(@PathVariable String topicName) 
            throws ExecutionException, InterruptedException {
        try {
            // 토픽 파티션 정보 가져오기
            DescribeTopicsResult describeTopicsResult = 
                    adminClient.describeTopics(Collections.singleton(topicName));
            TopicDescription topicDescription = 
                    describeTopicsResult.allTopicNames().get().get(topicName);
            
            // 토픽의 모든 파티션에 대한 TopicPartition 객체 생성
            List<TopicPartition> partitions = topicDescription.partitions().stream()
                    .map(partitionInfo -> new TopicPartition(topicName, partitionInfo.partition()))
                    .collect(Collectors.toList());
            
            // 시작 오프셋 조회를 위한 Map 생성 (각 파티션의 가장 이른 오프셋)
            Map<TopicPartition, OffsetSpec> earliestOffsetSpecs = partitions.stream()
                    .collect(Collectors.toMap(
                            tp -> tp,
                            tp -> OffsetSpec.earliest()
                    ));
            
            // 끝 오프셋 조회를 위한 Map 생성 (각 파티션의 가장 최근 오프셋)
            Map<TopicPartition, OffsetSpec> latestOffsetSpecs = partitions.stream()
                    .collect(Collectors.toMap(
                            tp -> tp,
                            tp -> OffsetSpec.latest()
                    ));
            
            // 시작 오프셋 조회
            Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> earliestOffsetResults = 
                    adminClient.listOffsets(earliestOffsetSpecs).all().get();
            
            // 끝 오프셋 조회
            Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> latestOffsetResults = 
                    adminClient.listOffsets(latestOffsetSpecs).all().get();
            
            // 각 파티션의 메시지 수 계산 및 합산
            long totalMessages = 0;
            Map<Integer, Long> partitionCounts = new HashMap<>();
            
            for (TopicPartition partition : partitions) {
                long startOffset = earliestOffsetResults.get(partition).offset();
                long endOffset = latestOffsetResults.get(partition).offset();
                long partitionCount = endOffset - startOffset;
                
                partitionCounts.put(partition.partition(), partitionCount);
                totalMessages += partitionCount;
            }
            
            // 결과 생성
            Map<String, Object> result = new HashMap<>();
            result.put("topic", topicName);
            result.put("totalMessages", totalMessages);
            result.put("partitionCounts", partitionCounts);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "토픽 메시지 수 조회 중 오류 발생: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}