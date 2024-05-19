
package com.example.energier.server.logger;

import com.example.energier.server.persistence.TerraUserLogEntity;

import java.sql.Date;
import java.time.Instant;

public class LoginActivityLogger extends TerraLogger {
    private final String token;
    private final String email;
    private final String ipAddress;
    private final String macAddress;

    public LoginActivityLogger(String token, String email, String ipAddress, String macAddress) {
        this.token = token;
        this.email = email;
        this.ipAddress = ipAddress;
        this.macAddress = macAddress;
    }

    public void run(){
        var log = new TerraUserLogEntity();
        log.setToken(token);
        log.setUserEmail(email);
        log.setIpAddress(ipAddress);
        log.setMacAddress(macAddress);
        log.setDate(Date.from(Instant.now()));
        getTemplate().insert(log);
    }
}
