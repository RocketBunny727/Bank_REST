package com.example.bankcards.controller;

import com.example.bankcards.dto.UserResponseDTO;
import com.example.bankcards.entity.Role;
import com.example.bankcards.exception.AccessDeniedException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.security.JwtAuthenticationFilter;
import com.example.bankcards.security.JwtTokenProvider;
import com.example.bankcards.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
public class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private UserResponseDTO userResponseDTO;
    private Page<UserResponseDTO> userPage;

    @BeforeEach
    void setUp() {
        userResponseDTO = UserResponseDTO.builder()
                .id(1L)
                .username("testuser")
                .name("Test")
                .surname("User")
                .role(Role.ADMIN)
                .build();

        userPage = new PageImpl<>(Collections.singletonList(userResponseDTO), PageRequest.of(0, 10), 1);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetByIdSuccess() throws Exception {
        when(userService.getById(anyLong())).thenReturn(userResponseDTO);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1L))
                .andExpect(MockMvcResultMatchers.jsonPath("$.username").value("testuser"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Test"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.surname").value("User"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetByIdNotFound() throws Exception {
        when(userService.getById(anyLong()))
                .thenThrow(new UserNotFoundException("User with id '1' not found"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("User with id '1' not found"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetByIdAccessDenied() throws Exception {
        when(userService.getById(anyLong()))
                .thenThrow(new AccessDeniedException("Only ADMIN can view the users information"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Only ADMIN can view the users information"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testFilterUsersSuccess() throws Exception {
        when(userService.getUsersByFilter(any(Pageable.class), any(), any(), any(), any()))
                .thenReturn(userPage);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/users")
                        .param("page", "0")
                        .param("size", "10")
                        .param("username", "testuser")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].id").value(1L))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].username").value("testuser"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].name").value("Test"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].surname").value("User"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].role").value("ADMIN"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testFilterUsersNotFound() throws Exception {
        when(userService.getUsersByFilter(any(Pageable.class), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/users")
                        .param("page", "0")
                        .param("size", "10")
                        .param("username", "nonexistent")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content").isEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(0));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testFilterUsersAccessDenied() throws Exception {
        when(userService.getUsersByFilter(any(Pageable.class), any(), any(), any(), any()))
                .thenThrow(new AccessDeniedException("Only ADMIN can view the users information"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/users")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Only ADMIN can view the users information"));
    }
}
