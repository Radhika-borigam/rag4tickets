package com.rag4tickets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class Rag4TicketsApplication {
    public static void main(String[] args) {
        SpringApplication.run(Rag4TicketsApplication.class, args);
    }
}
