package com.inplay.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

// 레포지토리는 com.inplay.ingest 에 있어 Boot 기본 스캔(애플리케이션 패키지=com.inplay.api)
// 범위 밖이다. 명시적으로 ingest 를 가리켜 Mongo 레포 빈을 등록한다.
@SpringBootApplication(scanBasePackages = "com.inplay")
@EnableMongoRepositories(basePackages = "com.inplay.ingest")
@EnableScheduling
public class InPlayApplication {

    public static void main(String[] args) {
        SpringApplication.run(InPlayApplication.class, args);
    }
}
