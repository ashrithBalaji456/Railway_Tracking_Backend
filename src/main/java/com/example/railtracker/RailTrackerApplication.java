package com.example.railtracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RailTrackerApplication {
    public static void main(String[] args) {
        SpringApplication.run(RailTrackerApplication.class, args);
    }
}
