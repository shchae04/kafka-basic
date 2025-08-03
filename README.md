# Kafka Streams 예제 프로젝트

이 프로젝트는 Kafka Streams를 사용한 다양한 스트리밍 처리 예제를 포함하고 있습니다.

## 기능 개요

이 프로젝트는 다음과 같은 Kafka 관련 기능을 제공합니다:

1. **기본 Kafka 프로듀서/컨슈머**
   - 일반 메시지 전송
   - 트랜잭션 메시지 전송
   - 오류 처리 및 재시도 메커니즘
   - 데드 레터 큐(DLQ) 처리

2. **Kafka Streams 예제**
   - 단어 수 세기 (Word Count)
   - 데이터 변환 (JSON 변환)
   - 트랜잭션 필터링 (금액 기준)

## 시작하기

### 사전 요구사항

- Java 24
- Docker 및 Docker Compose
- IntelliJ IDEA (권장)

### 실행 방법

1. Docker Compose로 Kafka 환경 실행:
   ```bash
   docker-compose up -d
   ```

2. 애플리케이션 실행:
   ```bash
   ./gradlew bootRun
   ```

## Kafka Streams 예제 설명

### 1. 단어 수 세기 (Word Count)

텍스트 메시지에서 각 단어의 출현 횟수를 계산하는 예제입니다.

- **입력 토픽**: `word-count-input`
- **출력 토픽**: `word-count-output`
- **처리 과정**:
  1. 텍스트를 소문자로 변환
  2. 단어로 분리
  3. 각 단어의 출현 횟수 계산
  4. 결과를 출력 토픽으로 전송

### 2. 데이터 변환 (JSON 변환)

JSON 형식의 사용자 데이터를 다른 형식으로 변환하는 예제입니다.

- **입력 토픽**: `user-data-input`
- **출력 토픽**: `user-data-transformed`
- **처리 과정**:
  1. 입력 JSON 파싱
  2. 필요한 필드 추출 및 변환
  3. 새로운 JSON 형식으로 변환
  4. 결과를 출력 토픽으로 전송

### 3. 트랜잭션 필터링 (금액 기준)

트랜잭션 데이터를 금액에 따라 필터링하여 다른 토픽으로 라우팅하는 예제입니다.

- **입력 토픽**: `transaction-input`
- **출력 토픽**:
  - `high-amount-transactions`: 100만원 이상
  - `medium-amount-transactions`: 10만원 이상 100만원 미만
  - `low-amount-transactions`: 10만원 미만
- **처리 과정**:
  1. 트랜잭션 데이터에서 금액 추출
  2. 금액에 따라 분류
  3. 해당하는 출력 토픽으로 전송

## API 엔드포인트

### Kafka Streams API

- **Kafka Streams 상태 확인**
  - `GET /api/streams/status`

- **단어 수 세기 예제**
  - 메시지 전송: `POST /api/streams/word-count`
  - 단어 카운트 조회: `GET /api/streams/word-count/{word}`

- **데이터 변환 예제**
  - 사용자 데이터 전송: `POST /api/streams/user-data`

- **트랜잭션 필터링 예제**
  - 트랜잭션 데이터 전송: `POST /api/streams/transaction`

### 기본 Kafka API

- **일반 메시지 전송**
  - `POST /api/send?msg={message}`

- **트랜잭션 메시지 전송**
  - `POST /api/send-transaction?messages={msg1,msg2,msg3}`
  - `POST /api/simulate-transaction-failure?messages={msg1,msg2,msg3}&failAfter={index}`

## 테스트 방법

프로젝트에 포함된 `http-requests.http` 파일을 사용하여 모든 API를 테스트할 수 있습니다.
IntelliJ IDEA에서는 이 파일을 열고 각 요청 옆의 실행 버튼을 클릭하여 테스트할 수 있습니다.

### 테스트 시나리오

1. **단어 수 세기 테스트**:
   - 텍스트 메시지 전송
   - 특정 단어의 카운트 조회

2. **데이터 변환 테스트**:
   - 사용자 데이터 JSON 전송
   - 변환된 결과는 `user-data-transformed` 토픽에서 확인

3. **트랜잭션 필터링 테스트**:
   - 다양한 금액의 트랜잭션 데이터 전송
   - 각 금액대별 토픽에서 결과 확인

## 오류 처리 및 복원력 기능

이 프로젝트는 실무 환경에서 필요한 오류 처리 및 복원력 기능을 포함하고 있습니다:

### 1. Resilience4j를 활용한 복원력 패턴

[Resilience4j](https://resilience4j.readme.io/)는 Netflix Hystrix에서 영감을 받은 경량 장애 허용 라이브러리로, 다양한 복원력 패턴을 제공합니다.

#### 1.1 재시도 패턴 (Retry)

메시지 처리 중 일시적인 오류가 발생할 경우, 자동으로 재시도하는 기능을 제공합니다.

- **구현 방식**: Resilience4j의 `@Retry` 어노테이션을 활용한 선언적 재시도
- **재시도 정책**: 
  - 기본 3회 재시도
  - 1초 간격으로 시작하여 지수 백오프 적용 (2배씩 증가)
  - 최대 대기 시간 10초
  - 특정 예외는 재시도하지 않음 (예: IllegalArgumentException)

#### 1.2 서킷 브레이커 패턴 (Circuit Breaker)

연속적인 오류가 발생할 경우, 서킷을 열어 시스템 과부하를 방지합니다.

- **구현 방식**: Resilience4j의 `@CircuitBreaker` 어노테이션 활용
- **서킷 브레이커 정책**:
  - 50% 실패율에 서킷 오픈
  - 최소 10번의 호출 후 실패율 계산
  - 30초 동안 서킷 오픈 상태 유지
  - 서킷 하프 오픈 상태에서 5번의 호출 허용

#### 1.3 벌크헤드 패턴 (Bulkhead)

동시 요청 수를 제한하여 시스템 리소스를 보호합니다.

- **구현 방식**: Resilience4j의 `@Bulkhead` 어노테이션 활용
- **벌크헤드 정책**:
  - 최대 20개의 동시 호출 허용
  - 최대 1초의 대기 시간

### 2. 데드 레터 큐(DLQ)

모든 재시도 후에도 처리할 수 없는 메시지는 데드 레터 큐(DLQ)로 자동 전송됩니다.

- **DLQ 토픽 명명 규칙**: 원본 토픽 이름 + `.DLQ` (예: `user-data-input.DLQ`)
- **DLQ 메시지 내용**: 원본 메시지 내용 그대로 보존
- **활용 방안**: 
  - 문제가 있는 메시지 분석
  - 오류 원인 파악 후 수정된 메시지 재처리
  - 시스템 모니터링 및 알림

### 3. Resilience4j 모니터링

Resilience4j는 Spring Actuator와 통합되어 다양한 모니터링 엔드포인트를 제공합니다.

- **모니터링 엔드포인트**:
  - `/actuator/retries`: 재시도 상태 및 통계
  - `/actuator/circuitbreakers`: 서킷 브레이커 상태 및 통계
  - `/actuator/bulkheads`: 벌크헤드 상태 및 통계
  - `/actuator/health`: 전체 시스템 건강 상태 (재시도, 서킷 브레이커, 벌크헤드 포함)

### 오류 처리 테스트 방법

다음 API를 사용하여 Resilience4j 패턴과 DLQ 기능을 테스트할 수 있습니다:

1. **재시도 패턴 테스트**:
   ```
   POST /api/streams/user-data
   Content-Type: application/json
   
   {"name": "Test User", "data": "error message"}
   ```
   - "error"를 포함한 메시지는 RuntimeException 발생
   - Resilience4j의 재시도 메커니즘에 의해 3회 재시도
   - 모든 재시도 실패 시 폴백 메서드 호출 및 `user-data-input.DLQ` 토픽으로 전송
   - 로그에서 재시도 시도 확인 가능

2. **재시도 없이 즉시 DLQ로 전송되는 케이스 테스트**:
   ```
   POST /api/streams/user-data
   Content-Type: application/json
   
   {"name": "Test User", "data": "invalid data"}
   ```
   - "invalid"를 포함한 메시지는 IllegalArgumentException 발생
   - Resilience4j 설정에서 IllegalArgumentException은 재시도하지 않음
   - 재시도 없이 바로 폴백 메서드 호출 및 `user-data-input.DLQ` 토픽으로 전송

3. **서킷 브레이커 패턴 테스트**:
   - 연속적으로 오류 메시지 전송하여 서킷 브레이커 트립 유도:
   ```
   # 여러 번 반복 실행
   POST /api/streams/user-data
   Content-Type: application/json
   
   {"name": "Test User", "data": "error message"}
   ```
   - 10번 이상의 호출 중 50% 이상 실패 시 서킷 오픈
   - 서킷 오픈 상태에서는 폴백 메서드가 즉시 호출됨 (재시도 없음)
   - `/actuator/circuitbreakers` 엔드포인트에서 서킷 상태 확인 가능

4. **벌크헤드 패턴 테스트**:
   - 동시에 많은 요청을 전송하여 벌크헤드 제한 테스트
   - 20개 이상의 동시 요청 시 일부 요청은 거부됨
   - `/actuator/bulkheads` 엔드포인트에서 벌크헤드 상태 확인 가능

5. **DLQ 토픽 확인**:
   - Kafka 클라이언트 도구를 사용하여 DLQ 토픽의 메시지 확인
   - 예: `kafka-console-consumer --bootstrap-server localhost:9092 --topic user-data-input.DLQ --from-beginning`

6. **Resilience4j 모니터링**:
   - 브라우저에서 다음 엔드포인트 접속하여 Resilience4j 상태 확인:
     - `http://localhost:8080/actuator/retries`
     - `http://localhost:8080/actuator/circuitbreakers`
     - `http://localhost:8080/actuator/bulkheads`
     - `http://localhost:8080/actuator/health`

## 참고 사항

- 상태 저장소는 `/tmp/kafka-streams` 디렉토리에 저장됩니다.
- 모든 Kafka Streams 처리는 exactly-once 의미 체계로 구성되어 있습니다.
- 애플리케이션 재시작 시 상태 저장소가 복구됩니다.
- DLQ 토픽은 애플리케이션 시작 시 자동으로 생성됩니다.