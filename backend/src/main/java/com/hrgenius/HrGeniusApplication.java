package com.hrgenius;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
public class HrGeniusApplication {

    public static void main(String[] args) {
        SpringApplication.run(HrGeniusApplication.class, args);
    }
}
