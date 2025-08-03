package com.example.kafkabasic.controller;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.streams.KafkaStreams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 카프카 대시보드 컨트롤러
 * 카프카 토픽 및 스트림 정보를 시각적으로 보여주는 대시보드 페이지를 제공합니다.
 */
@Controller
public class DashboardController {

    private final KafkaAdmin kafkaAdmin;
    private final StreamsBuilderFactoryBean streamsBuilderFactoryBean;

    @Autowired
    public DashboardController(KafkaAdmin kafkaAdmin, StreamsBuilderFactoryBean streamsBuilderFactoryBean) {
        this.kafkaAdmin = kafkaAdmin;
        this.streamsBuilderFactoryBean = streamsBuilderFactoryBean;
    }

    /**
     * 카프카 대시보드 메인 페이지
     * 토픽 목록과 Kafka Streams 상태를 보여줍니다.
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        try {
            // 토픽 목록 조회
            AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties());
            ListTopicsResult topics = adminClient.listTopics();
            Set<String> topicNames = topics.names().get();
            
            // 내부 토픽 필터링 (언더스코어로 시작하는 토픽은 Kafka 내부 토픽)
            List<String> userTopics = topicNames.stream()
                    .filter(name -> !name.startsWith("_"))
                    .sorted()
                    .collect(Collectors.toList());
            
            model.addAttribute("topics", userTopics);
            
            // Kafka Streams 상태 조회
            KafkaStreams kafkaStreams = streamsBuilderFactoryBean.getKafkaStreams();
            String streamsStatus = kafkaStreams != null ? kafkaStreams.state().name() : "NOT_INITIALIZED";
            model.addAttribute("streamsStatus", streamsStatus);
            
            // 예제 토픽 그룹화
            List<String> wordCountTopics = new ArrayList<>();
            List<String> userDataTopics = new ArrayList<>();
            List<String> transactionTopics = new ArrayList<>();
            List<String> otherTopics = new ArrayList<>();
            
            for (String topic : userTopics) {
                if (topic.contains("word-count")) {
                    wordCountTopics.add(topic);
                } else if (topic.contains("user-data")) {
                    userDataTopics.add(topic);
                } else if (topic.contains("transaction") || 
                           topic.contains("amount-transactions")) {
                    transactionTopics.add(topic);
                } else {
                    otherTopics.add(topic);
                }
            }
            
            model.addAttribute("wordCountTopics", wordCountTopics);
            model.addAttribute("userDataTopics", userDataTopics);
            model.addAttribute("transactionTopics", transactionTopics);
            model.addAttribute("otherTopics", otherTopics);
            
            return "dashboard";
        } catch (Exception e) {
            model.addAttribute("error", "대시보드 정보 조회 중 오류 발생: " + e.getMessage());
            return "dashboard";
        }
    }
}