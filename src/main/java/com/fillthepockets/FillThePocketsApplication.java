package com.fillthepockets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FillThePocketsApplication {
    public static void main(String[] args) {
        SpringApplication.run(FillThePocketsApplication.class, args);
    }
}
