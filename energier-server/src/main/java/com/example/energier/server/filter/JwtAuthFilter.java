package com.example.energier.server.filter;

import com.example.energier.server.jwt.JwtUtil;
import com.example.energier.server.repository.UserRepository;
import com.example.energier.server.type.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;


    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        if (request.getServletPath().contains("/api/v1/auth")) {
            filterChain.doFilter(request, response);
            return;
        }
        final String authHeader=
                request.getHeader(AUTHORIZATION);
        if (authHeader == null ||!authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        final String userEmail;
        final String jwtToken;
        jwtToken = authHeader.substring(7);
        userEmail = jwtUtil.extractUsername(jwtToken);

        if (userEmail != null) {
            var optionalDetails = this.userRepository.findByEmail(userEmail);
            UserDetails userDetails;
            if(optionalDetails.isPresent()){
                userDetails = optionalDetails.get();
                if (request.getServletPath().contains("/admin") &&
                        !(userDetails.getAuthorities().contains(new SimpleGrantedAuthority(Role.ADMIN.name()))
                            || userDetails.getAuthorities().contains(new SimpleGrantedAuthority(Role.SUPERADMIN.name())))) {
                    filterChain.doFilter(request, response);
                    return;
                }
                if (request.getServletPath().contains("/super") &&
                        !(userDetails.getAuthorities().contains(new SimpleGrantedAuthority(Role.SUPERADMIN.name())))) {
                    filterChain.doFilter(request, response);
                    return;
                }
            } else {
                filterChain.doFilter(request,response);
                return;
            }

            if (jwtUtil.isTokenValid(jwtToken, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        filterChain.doFilter(request, response);
    }
}