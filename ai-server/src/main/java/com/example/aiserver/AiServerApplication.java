package com.example.aiserver;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.example.aiserver.mapper")
@SpringBootApplication
public class AiServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiServerApplication.class, args);
    }
}
