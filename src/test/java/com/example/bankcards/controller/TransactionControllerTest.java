package com.example.bankcards.controller;

import com.example.bankcards.dto.TransactionDTO;
import com.example.bankcards.dto.TransactionResponseDTO;
import com.example.bankcards.exception.AccessDeniedException;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.repository.ICardRepository;
import com.example.bankcards.security.JwtAuthenticationFilter;
import com.example.bankcards.security.JwtTokenProvider;
import com.example.bankcards.service.TransactionService;
import com.example.bankcards.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@WebMvcTest(controllers = TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
public class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ICardRepository cardRepository;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private TransactionDTO transactionDTO;
    private TransactionResponseDTO transactionResponseDTO;

    @BeforeEach
    void setUp() {
        transactionDTO = TransactionDTO.builder()
                .sourceCardNumber("1234567890123456")
                .destinationCardNumber("9876543210987654")
                .amount(100.00)
                .build();

        transactionResponseDTO = TransactionResponseDTO.builder()
                .sourceId(1L)
                .destinationId(2L)
                .maskedSourceCardNumber("**** **** **** 3456")
                .maskedDestinationCardNumber("**** **** **** 7654")
                .sourceBalance(900.00)
                .destinationBalance(1100.00)
                .owner("Test User")
                .build();
    }

    @Test
    @WithMockUser(roles = "USER")
    void testMakeTransactionSuccess() throws Exception {
        when(transactionService.transfer(any(TransactionDTO.class))).thenReturn(transactionResponseDTO);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/transact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transactionDTO)))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.sourceId").value(1L))
                .andExpect(MockMvcResultMatchers.jsonPath("$.destinationId").value(2L))
                .andExpect(MockMvcResultMatchers.jsonPath("$.maskedSourceCardNumber").value("**** **** **** 3456"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.maskedDestinationCardNumber").value("**** **** **** 7654"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.sourceBalance").value(900.00))
                .andExpect(MockMvcResultMatchers.jsonPath("$.destinationBalance").value(1100.00))
                .andExpect(MockMvcResultMatchers.jsonPath("$.owner").value("Test User"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testMakeTransactionSourceCardNotFound() throws Exception {
        when(transactionService.transfer(any(TransactionDTO.class)))
                .thenThrow(new CardNotFoundException("Source card with number: '1234567890123456' not found"));

        mockMvc.perform(MockMvcRequestBuilders.put("/api/transact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transactionDTO)))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Source card with number: '1234567890123456' not found"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testMakeTransactionDestinationCardNotFound() throws Exception {
        when(transactionService.transfer(any(TransactionDTO.class)))
                .thenThrow(new CardNotFoundException("Destination card with number: '9876543210987654' not found"));

        mockMvc.perform(MockMvcRequestBuilders.put("/api/transact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transactionDTO)))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Destination card with number: '9876543210987654' not found"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testMakeTransactionAccessDenied() throws Exception {
        when(transactionService.transfer(any(TransactionDTO.class)))
                .thenThrow(new AccessDeniedException("Cannot transfer between cards of another user"));

        mockMvc.perform(MockMvcRequestBuilders.put("/api/transact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transactionDTO)))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Cannot transfer between cards of another user"));
    }
}