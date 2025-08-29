package com.example.bankcards.service;

import com.example.bankcards.dto.LoginDTO;
import com.example.bankcards.dto.LoginResponseDTO;
import com.example.bankcards.exception.DataNotFilledException;
import com.example.bankcards.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserService userService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void login_success() {
        LoginDTO dto = LoginDTO.builder()
                .username("user")
                .password("password")
                .build();
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtTokenProvider.generateToken(authentication)).thenReturn("jwt-token");

        LoginResponseDTO response = authService.login(dto);

        assertEquals("jwt-token", response.getToken());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtTokenProvider).generateToken(authentication);
        verify(securityContext).setAuthentication(authentication);
    }

    @Test
    void login_nullUsername_throwsDataNotFilledException() {
        LoginDTO dto = LoginDTO.builder()
                .username(null)
                .password("password")
                .build();

        DataNotFilledException exception = assertThrows(DataNotFilledException.class, () -> authService.login(dto));
        assertEquals("Username or password is empty", exception.getMessage());
        verifyNoInteractions(authenticationManager, jwtTokenProvider);
    }

    @Test
    void login_nullPassword_throwsDataNotFilledException() {
        LoginDTO dto = LoginDTO.builder()
                .username("user")
                .password(null)
                .build();

        DataNotFilledException exception = assertThrows(DataNotFilledException.class, () -> authService.login(dto));
        assertEquals("Username or password is empty", exception.getMessage());
        verifyNoInteractions(authenticationManager, jwtTokenProvider);
    }
}
