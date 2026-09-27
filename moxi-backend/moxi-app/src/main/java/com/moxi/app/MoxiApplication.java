package com.moxi.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.moxi"})
@MapperScan({"com.moxi.user.repository", "com.moxi.content.repository", "com.moxi.ai.repository", "com.moxi.billing.repository"})
@EnableScheduling
public class MoxiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MoxiApplication.class, args);
    }
}
