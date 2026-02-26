package com.uzenjitrust;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UzenjiTrustApplication {

    public static void main(String[] args) {
        SpringApplication.run(UzenjiTrustApplication.class, args);
    }
}
