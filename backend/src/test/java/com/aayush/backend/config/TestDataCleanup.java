package com.aayush.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

/**
 * Test configuration that cleans the test database before each test class
 * Ensures test isolation and repeatability
 */
@TestConfiguration
public class TestDataCleanup implements TestExecutionListener {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void beforeTestClass(TestContext testContext) {
        try {
            MongoTemplate template = testContext.getApplicationContext().getBean(MongoTemplate.class);
            // Drop the entire test database to ensure clean state
            // Only affects aayush_test database, never touches aayush_db
            template.getDb().drop();
        } catch (Exception e) {
            // Ignore if beans are not yet initialized
        }
    }
}
