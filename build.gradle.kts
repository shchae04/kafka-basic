plugins {
    java
    id("org.springframework.boot") version "3.5.4"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(24))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf") // 대시보드 UI를 위한 Thymeleaf
    implementation("org.springframework.boot:spring-boot-starter-actuator") // 헬스 체크를 위한 Spring Actuator
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0") // API 문서화를 위한 Swagger/OpenAPI
    implementation("org.springframework.boot:spring-boot-starter-validation")

    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.apache.kafka:kafka-streams")

    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
    implementation("io.github.resilience4j:resilience4j-retry:2.2.0")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:2.2.0")
    implementation("org.springframework.boot:spring-boot-starter-aop") // Resilience4j에 필요한 AOP 지원


    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}