package com.example.energier.server.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateDeviceRequest {
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
}
