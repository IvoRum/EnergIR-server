package com.example.energier.server.service;

import com.example.energier.server.config.SecurityConfig;
import com.example.energier.server.model.request.AuthenticationRequest;
import com.example.energier.server.model.request.RegistrationRequest;
import com.example.energier.server.model.responce.AuthenticationResponse;
import com.example.energier.server.persistence.User;
import com.example.energier.server.repository.UserRepository;
import com.example.energier.server.type.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@ReadingConverter
@RequiredArgsConstructor
public class RegisterService {

    private final UserRepository userRepository;
    private final AuthenticationService authenticationService;
    private final SecurityConfig securityConfig;
    public AuthenticationResponse registerUser(RegistrationRequest request) {

        var user = userRepository.findByEmail(request.getEmail());
        if(user.isPresent()){
            //email already taken
            return null;
        }

        User toRegister = new User();
        toRegister.setEmail(request.getEmail());
        toRegister.setPassword(securityConfig.passwordEncoder().encode(request.getPassword()));
        toRegister.setFirstName(request.getFirstName());
        toRegister.setFamilyName(request.getLastName());
        toRegister.setRole(Role.USER);
        userRepository.save(toRegister);

        return authenticationService.authenticate(
                new AuthenticationRequest(request.getEmail(),request.getPassword(),
                        request.getMacAddress(),request.getIpAddress()));
    }
}
