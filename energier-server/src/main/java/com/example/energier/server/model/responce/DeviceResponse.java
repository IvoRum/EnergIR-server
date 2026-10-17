package com.example.energier.server.model.responce;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeviceResponse {
    private Integer id;
    private String code;
    private String name;
    private String description;
    private String location;
    private String type;
    private String communicationType;
    private String port;
    private String baudrate;
    private String parity;
    private String dataBits;
    private String status;
    private Instant lastSeen;
    private Instant createdOn;
}
