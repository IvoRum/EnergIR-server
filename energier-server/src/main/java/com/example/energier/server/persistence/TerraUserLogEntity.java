package com.example.energier.server.persistence;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.util.Date;

@Document(collection = "terra_user_log")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class TerraUserLogEntity {
    @MongoId
    private ObjectId id;
    private String userEmail;
    private String token;
    private String ipAddress;
    private String macAddress;
    private Date date;
}
