# Kafka 학습 프로젝트

Spring Boot와 Apache Kafka를 활용한 기본 메시징 시스템입니다.

## 주요 기능

1. **기본 메시지 전송/수신**

   * 단일 메시지를 `basic-topic`으로 전송
   * 토픽에서 메시지를 받아 처리
2. **트랜잭션 메시징**

   * 여러 메시지를 원자적 트랜잭션으로 전송
   * 실패 시 자동 롤백

## 실행 환경

* Java 24
* Docker & Docker Compose

## 실행 방법

```bash
docker-compose up -d      # Kafka 환경 실행
./gradlew bootRun         # 애플리케이션 실행
```

## 테스트 방법

`src/main/resources/http-requests.http`의 HTTP 요청으로 확인:

* 기본 메시지 전송
* 트랜잭션 전송(성공/실패)

## API 엔드포인트

* `POST /api/send?msg=메시지`
* `POST /api/send-transaction?messages=msg1,msg2,...`
* `POST /api/simulate-transaction-failure?messages=msg1,msg2,...&failAfter=N`

## 향후 학습 예정 기능

* Apache Avro & Schema Registry
* Kafka Streams (예: 단어 카운팅)
* Kafka Connect (DB 연동)
* 커스텀 파티셔닝 전략
* Exactly-Once Processing
* 오류 처리 및 Dead Letter Queue

## 참고 자료

* Apache Kafka 공식 문서
* Confluent 튜토리얼
* Spring for Apache Kafka
* *Kafka: The Definitive Guide*
* *Designing Event-Driven Systems*

## 프로젝트 구조

```
kafka-basic/
├── src/
│   └── main/java/com/example/kafkabasic/
│       ├── config/KafkaConfig.java
│       ├── controller/MessageController.java
│       ├── service/
│       │   ├── ProducerService.java
│       │   ├── ConsumerService.java
│       │   └── TransactionalProducerService.java
│       └── KafkaBasicApplication.java
│   └── resources/
│       ├── application.yml
│       └── http-requests.http
├── docker-compose.yml
├── build.gradle.kts
└── README.md
```
