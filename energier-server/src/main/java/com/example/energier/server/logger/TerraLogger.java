package com.example.energier.server.logger;

import com.mongodb.client.MongoClients;
import org.springframework.data.mongodb.core.MongoTemplate;

public abstract class TerraLogger extends Thread {

    // Set MONGODB_URI in the environment (e.g. /etc/volts/energir.env).
    static final String CONNECTION_STRING =
            System.getenv().getOrDefault("MONGODB_URI", "mongodb://localhost:27017");

    protected static MongoTemplate getTemplate(){
        return new MongoTemplate
                (MongoClients.create(CONNECTION_STRING), "prod");
    }

}
