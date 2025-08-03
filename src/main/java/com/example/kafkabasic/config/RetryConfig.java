package com.example.kafkabasic.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class RetryConfig {

    /**
     * Kafka 작업을 위한 재시도 템플릿 설정
     * - 최대 3번 재시도
     * - 지수 백오프 전략 (초기 1초, 최대 10초)
     */
    @Bean
    public RetryTemplate kafkaRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        // 재시도 정책: 최대 3번 재시도
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        
        // 백오프 정책: 지수 백오프 (초기 1초, 최대 10초)
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000); // 1초
        backOffPolicy.setMultiplier(2.0);       // 각 재시도마다 2배로 증가
        backOffPolicy.setMaxInterval(10000);    // 최대 10초
        
        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        return retryTemplate;
    }
}