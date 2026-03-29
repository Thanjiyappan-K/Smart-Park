package com.smartpark;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;

@SpringBootApplication
public class SmartParkApplication {

    public static void main(String[] args) {
        loadDotEnvIfPresent();
        SpringApplication.run(SmartParkApplication.class, args);
    }

    /**
     * Load project-root {@code .env} into JVM system properties before Spring starts so
     * placeholders such as {@code ${DB_PASSWORD}} in {@code application.yml} resolve when the
     * environment is built. Does not override existing OS env vars or JVM {@code -D} properties.
     */
    private static void loadDotEnvIfPresent() {
        File envFile = new File(System.getProperty("user.dir"), ".env");
        if (!envFile.isFile()) {
            return;
        }
        Dotenv dotenv = Dotenv.configure()
                .directory(System.getProperty("user.dir"))
                .ignoreIfMissing()
                .load();
        dotenv.entries().forEach(entry -> {
            String key = entry.getKey();
            if (System.getenv(key) == null && System.getProperty(key) == null) {
                System.setProperty(key, entry.getValue());
            }
        });
    }
}
