package com.example.bankcards.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Configuration
public class DatabaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    @Bean
    public CommandLineRunner setEncryptionKey(DataSource dataSource, @Value("${encrypt.key}") String encryptionKey) {
        return args -> {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("SELECT set_config('encrypt.key', '" + encryptionKey + "', false)");
                logger.info("Encryption key set successfully");
            } catch (SQLException e) {
                logger.error("Failed to set encryption key: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to set encryption key", e);
            }
        };
    }
}
