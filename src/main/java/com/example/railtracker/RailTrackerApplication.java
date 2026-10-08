package com.example.railtracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
@EnableScheduling
public class RailTrackerApplication {
    public static void main(String[] args) {
        loadDotenv();
        SpringApplication.run(RailTrackerApplication.class, args);
    }

    private static void loadDotenv() {
        try {
            Path envPath = Path.of(".env");
            if (!Files.exists(envPath)) {
                envPath = Path.of("Backend", ".env");
            }
            if (Files.exists(envPath)) {
                for (String line : Files.readAllLines(envPath)) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#") && line.contains("=")) {
                        int eq = line.indexOf('=');
                        String key = line.substring(0, eq).trim();
                        String val = line.substring(eq + 1).trim();
                        if (System.getProperty(key) == null && System.getenv(key) == null) {
                            System.setProperty(key, val);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }
}
