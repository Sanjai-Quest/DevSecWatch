package com.devsecwatch.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class DevSecWatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(DevSecWatchApplication.class, args);
    }

}
