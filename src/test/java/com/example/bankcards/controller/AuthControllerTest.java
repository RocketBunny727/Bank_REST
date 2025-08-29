package com.example.bankcards.controller;

import com.example.bankcards.dto.LoginDTO;
import com.example.bankcards.dto.LoginResponseDTO;
import com.example.bankcards.dto.UserCreateDTO;
import com.example.bankcards.dto.UserResponseDTO;
import com.example.bankcards.entity.Role;
import com.example.bankcards.exception.UsernameAlreadyExistsException;
import com.example.bankcards.security.JwtAuthenticationFilter;
import com.example.bankcards.security.JwtTokenProvider;
import com.example.bankcards.service.AuthService;
import com.example.bankcards.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private UserCreateDTO userCreateDTO;
    private UserResponseDTO userResponseDTO;
    private LoginDTO loginDTO;
    private LoginResponseDTO loginResponseDTO;

    @BeforeEach
    void setUp() {
        userCreateDTO = UserCreateDTO.builder()
                .username("testuser")
                .password("password")
                .name("Test")
                .surname("User")
                .build();

        userResponseDTO = UserResponseDTO.builder()
                .id(1L)
                .username("testuser")
                .name("Test")
                .surname("User")
                .role(Role.USER)
                .build();

        loginDTO = LoginDTO.builder()
                .username("testuser")
                .password("password")
                .build();

        loginResponseDTO = LoginResponseDTO.builder()
                .token("mocked-jwt-token")
                .build();
    }

    @Test
    void testSignupSuccess() throws Exception {
        when(userService.createUser(any(UserCreateDTO.class))).thenReturn(userResponseDTO);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userCreateDTO)))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1L))
                .andExpect(MockMvcResultMatchers.jsonPath("$.username").value("testuser"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Test"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.surname").value("User"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.role").value("USER"));
    }

    @Test
    void testSignupUsernameAlreadyExists() throws Exception {
        when(userService.createUser(any(UserCreateDTO.class)))
                .thenThrow(new UsernameAlreadyExistsException("Username already exists"));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userCreateDTO)))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Username already exists"));
    }

    @Test
    @WithMockUser
    void testLoginSuccess() throws Exception {
        when(authService.login(any(LoginDTO.class))).thenReturn(loginResponseDTO);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.token").value("mocked-jwt-token"));
    }

    @Test
    @WithMockUser
    void testLoginBadCredentials() throws Exception {
        when(authService.login(any(LoginDTO.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Bad credentials"));
    }
}
