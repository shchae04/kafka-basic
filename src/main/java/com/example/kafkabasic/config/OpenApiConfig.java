package com.example.kafkabasic.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI 문서화 설정
 * API 문서의 기본 정보와 서버 정보를 설정합니다.
 */
@Configuration
public class OpenApiConfig {

    /**
     * OpenAPI 설정 빈
     * API 문서의 제목, 설명, 버전, 연락처 등의 정보를 설정합니다.
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Kafka Basic API")
                        .description("Kafka 기본 기능과 Kafka Streams 예제를 위한 REST API")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Kafka Basic Team")
                                .email("shchae04@naver.com")
                                .url("https://github.com/shchae04/kafka-basic"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Development Server")
                ));
    }
}