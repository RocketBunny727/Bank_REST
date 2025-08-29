package com.example.bankcards.service;

import com.example.bankcards.dto.LoginDTO;
import com.example.bankcards.dto.LoginResponseDTO;
import com.example.bankcards.exception.DataNotFilledException;
import com.example.bankcards.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public LoginResponseDTO login(LoginDTO dto) {
        if (dto.getUsername() == null || dto.getUsername().isEmpty()
                || dto.getPassword() == null || dto.getPassword().isEmpty()) {
            throw new DataNotFilledException("Username or password is empty");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtTokenProvider.generateToken(authentication);

        return LoginResponseDTO.builder()
                .token(token)
                .build();
    }
}
