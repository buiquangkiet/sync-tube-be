package com.example.synctube;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SynctubeApplication {

    public static void main(String[] args) {
        loadEnvFile();
        SpringApplication.run(SynctubeApplication.class, args);
    }

    private static void loadEnvFile() {
        Dotenv dotenv = Dotenv.configure()
                .directory(System.getProperty("user.dir"))
                .filename(".env")
                .ignoreIfMissing()
                .load();

        dotenv.entries().forEach(entry -> {
            if (System.getenv(entry.getKey()) != null) {
                return;
            }
            System.setProperty(entry.getKey(), entry.getValue());
        });

        String profile = dotenv.get("SPRING_PROFILES_ACTIVE");
        if (profile != null && System.getProperty("spring.profiles.active") == null
                && System.getenv("SPRING_PROFILES_ACTIVE") == null) {
            System.setProperty("spring.profiles.active", profile);
        }
    }

}
