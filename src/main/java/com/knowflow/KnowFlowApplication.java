package com.knowflow;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@MapperScan("com.knowflow.mapper")
@SpringBootApplication
public class KnowFlowApplication {
    public static void main(String[] args) {
        SpringApplication.run(KnowFlowApplication.class, args);
    }
}
