package com.aayush.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * MongoDB Configuration
 * Enables auditing for automatic timestamp management
 */
@Configuration
@EnableMongoAuditing
@EnableMongoRepositories(basePackages = "com.aayush.backend.repository")
public class MongoConfig {
    // MongoDB auditing configuration
    // @CreatedDate and @LastModifiedDate will be automatically populated
}
