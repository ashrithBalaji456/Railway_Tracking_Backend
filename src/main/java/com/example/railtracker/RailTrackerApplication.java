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
        normalizeDatabaseUrl();
        SpringApplication.run(RailTrackerApplication.class, args);
    }

    /**
     * Converts cloud platform PostgreSQL URLs (e.g. Render's postgres://user:password@host:port/db)
     * into standard JDBC format (jdbc:postgresql://host:port/db) and extracts credentials.
     */
    private static void normalizeDatabaseUrl() {
        String dbUrl = System.getenv("DATABASE_URL");
        if (dbUrl == null || dbUrl.isBlank()) {
            dbUrl = System.getProperty("DATABASE_URL");
        }
        if (dbUrl != null && !dbUrl.isBlank() && !dbUrl.startsWith("jdbc:")) {
            try {
                java.net.URI uri = new java.net.URI(dbUrl);
                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                String path = uri.getPath() != null ? uri.getPath() : "";
                String query = uri.getQuery();

                String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;
                if (query != null && !query.isBlank()) {
                    jdbcUrl += "?" + query;
                }

                System.setProperty("DATABASE_URL", jdbcUrl);
                System.setProperty("spring.datasource.url", jdbcUrl);

                String userInfo = uri.getUserInfo();
                if (userInfo != null && userInfo.contains(":")) {
                    String[] parts = userInfo.split(":", 2);
                    System.setProperty("DATABASE_USERNAME", parts[0]);
                    System.setProperty("DATABASE_PASSWORD", parts[1]);
                    System.setProperty("spring.datasource.username", parts[0]);
                    System.setProperty("spring.datasource.password", parts[1]);
                }
                System.setProperty("spring.datasource.driver-class-name", "org.postgresql.Driver");
            } catch (Exception ignored) {
            }
        }

        String redisUrl = System.getenv("REDIS_URL");
        if (redisUrl == null || redisUrl.isBlank()) {
            redisUrl = System.getProperty("REDIS_URL");
        }
        if (redisUrl != null && !redisUrl.isBlank()) {
            System.setProperty("spring.data.redis.url", redisUrl);
        }
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
