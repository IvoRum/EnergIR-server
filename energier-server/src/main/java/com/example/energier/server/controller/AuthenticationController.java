package com.example.energier.server.controller;

import com.example.energier.server.model.request.AuthenticationRequest;
import com.example.energier.server.model.request.RegistrationRequest;
import com.example.energier.server.model.responce.AuthenticationResponse;
import com.example.energier.server.service.AuthenticationService;
import com.example.energier.server.service.RegisterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vi/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final RegisterService registerService;

    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @RequestBody AuthenticationRequest request,
            @RequestHeader("Host") String remoteAddress
    ) {
        request.setIpAddress(remoteAddress);
        var response = authenticationService.authenticate(request);
        return response.getAccessToken() != null ? ResponseEntity.ok(response) : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> registerUser(
            @RequestBody RegistrationRequest request,
            @RequestHeader("Host") String remoteAddress
    ) {
        request.setIpAddress(remoteAddress);
        AuthenticationResponse response = registerService.registerUser(request);
        if(response == null){
            return ResponseEntity.status(HttpStatus.SEE_OTHER).body(null);
        }
        return ResponseEntity.ok(response);
    }

}