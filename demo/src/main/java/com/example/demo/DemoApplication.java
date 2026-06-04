package com.example.demo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot应用启动类。
 */
@MapperScan("com.example.demo.mapper")
@SpringBootApplication
public class DemoApplication {

    /**
     * 启动Spring Boot应用。
     */
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

}
