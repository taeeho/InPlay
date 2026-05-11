package com.inplay.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.inplay")
@EnableScheduling
public class InPlayApplication {

    public static void main(String[] args) {
        SpringApplication.run(InPlayApplication.class, args);
    }
}
