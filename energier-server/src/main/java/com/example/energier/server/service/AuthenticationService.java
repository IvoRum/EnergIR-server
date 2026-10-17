package com.example.energier.server.service;

import com.example.energier.server.jwt.JwtUtil;
import com.example.energier.server.model.request.AuthenticationRequest;
import com.example.energier.server.model.responce.AuthenticationResponse;
import com.example.energier.server.persistence.Company;
import com.example.energier.server.persistence.UserCompany;
import com.example.energier.server.repository.UserCompanyRepository;
import com.example.energier.server.repository.UserRepository;
import com.example.energier.server.type.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthenticationService {
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final UserCompanyRepository userCompanyRepository;
    private final AuthenticationManager authenticationManager;

    public AuthenticationResponse authenticate(AuthenticationRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        var user = userRepository.findByEmail(request.getEmail()).orElseThrow(
                () -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "user not found"
                )
        );

        // Login resolves the company the user is attached to, so the client can
        // immediately request that company's data.
        Company company = userCompanyRepository.findFirstByUser_IdOrderByIdAsc(user.getId())
                .map(UserCompany::getCompany)
                .orElse(null);

        if (user.getAuthorities().contains(new SimpleGrantedAuthority(Role.BANNED.name()))) {
            return AuthenticationResponse.builder()
                    .accessToken(null)
                    .role(user.getRole().toString())
                    .companyId(company != null ? company.getId() : null)
                    .companyCode(company != null ? company.getCode() : null)
                    .companyName(company != null ? company.getName() : null)
                    .build();
        }

        var jwtToken = jwtUtil.generateToken(user);
        //TODO remove when created logger server
        // new LoginActivityLogger(jwtToken,request.getEmail(),request.getIpAddress(),request.getMacAddress()).start();
        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .role(user.getRole().toString())
                .companyId(company != null ? company.getId() : null)
                .companyCode(company != null ? company.getCode() : null)
                .companyName(company != null ? company.getName() : null)
                .build();
    }
}
