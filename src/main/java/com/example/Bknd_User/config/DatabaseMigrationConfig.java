package com.example.Bknd_User.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DatabaseMigrationConfig {

    @Bean
    public ApplicationRunner updateUserTable(JdbcTemplate jdbcTemplate) {
        return args -> {
            jdbcTemplate.execute("ALTER TABLE IF EXISTS usuarios ADD COLUMN IF NOT EXISTS google_linked BOOLEAN NOT NULL DEFAULT FALSE");
            jdbcTemplate.execute("ALTER TABLE IF EXISTS usuarios ADD COLUMN IF NOT EXISTS google_id VARCHAR(255)");
            jdbcTemplate.execute("ALTER TABLE IF EXISTS usuarios ALTER COLUMN password DROP NOT NULL");

            jdbcTemplate.execute("ALTER TABLE IF EXISTS usuarios ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
        };
    }
}