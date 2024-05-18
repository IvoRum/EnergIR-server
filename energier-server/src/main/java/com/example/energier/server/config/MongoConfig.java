package com.example.energier.server.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

//@Configuration
public class MongoConfig {
    public @Bean MongoClient mongoClient() {
        return MongoClients.create("mongodb+srv://ivo:ar4ebar4e@terra-log.rumnftg.mongodb.net/?retryWrites=true&w=majority&appName=terra-log");
    }

    @Bean
    public MongoTemplate mongoTemplate() {
        return new MongoTemplate(mongoClient(),"prod");
    }
}

