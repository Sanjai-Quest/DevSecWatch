package com.devsecwatch.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WorkerApplication {

    public static void main(String[] args) {
        System.out.println("DEBUG: RABBITMQ_HOST = " + System.getenv("RABBITMQ_HOST"));
        SpringApplication.run(WorkerApplication.class, args);
    }

}
