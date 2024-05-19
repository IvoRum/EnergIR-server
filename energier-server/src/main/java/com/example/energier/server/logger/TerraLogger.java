package com.example.energier.server.logger;

import com.mongodb.client.MongoClients;
import org.springframework.data.mongodb.core.MongoTemplate;

public abstract class TerraLogger extends Thread {

    static final String CONNECTION_STRING = "mongodb+srv://ivo:ar4ebar4e@terra-log.rumnftg.mongodb.net/?retryWrites=true&w=majority&appName=terra-log";

    protected static MongoTemplate getTemplate(){
        return new MongoTemplate
                (MongoClients.create(String.format(CONNECTION_STRING)), "prod");
    }

}
