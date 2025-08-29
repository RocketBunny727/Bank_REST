package com.example.bankcards.controller;

import com.example.bankcards.dto.CardCreateDTO;
import com.example.bankcards.dto.CardResponseDTO;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.exception.AccessDeniedException;
import com.example.bankcards.exception.CardAlreadyExistsException;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.GlobalExceptionHandler;
import com.example.bankcards.repository.ICardRepository;
import com.example.bankcards.security.JwtAuthenticationFilter;
import com.example.bankcards.security.JwtTokenProvider;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@WebMvcTest(controllers = CardController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
public class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CardService cardService;

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

    private CardCreateDTO cardCreateDTO;
    private CardResponseDTO cardResponseDTO;
    private Page<CardResponseDTO> cardPage;

    @BeforeEach
    void setUp() {
        cardCreateDTO = CardCreateDTO.builder()
                .cardNumber("1234 5678 9012 3456")
                .expiryDate("12/25")
                .userId(1L)
                .balance(100.00)
                .build();

        cardResponseDTO = CardResponseDTO.builder()
                .id(1L)
                .maskedNumber("**** **** **** 3456")
                .owner("Test User")
                .expiryDate("12/25")
                .status(CardStatus.ACTIVE)
                .balance(100.00)
                .isBlockRequested(false)
                .build();

        cardPage = new PageImpl<>(Collections.singletonList(cardResponseDTO), PageRequest.of(0, 10), 1);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateCardSuccess() throws Exception {
        when(cardService.createCard(any(CardCreateDTO.class))).thenReturn(cardResponseDTO);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardCreateDTO)))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1L))
                .andExpect(MockMvcResultMatchers.jsonPath("$.maskedNumber").value("**** **** **** 3456"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.owner").value("Test User"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.expiryDate").value("12/25"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("ACTIVE"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.balance").value(100.00))
                .andExpect(MockMvcResultMatchers.jsonPath("$.blockRequested").value(false));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testCreateCardAccessDenied() throws Exception {
        when(cardService.createCard(any(CardCreateDTO.class)))
                .thenThrow(new AccessDeniedException("Only ADMIN can create cards"));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardCreateDTO)))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Only ADMIN can create cards"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateCardAlreadyExists() throws Exception {
        when(cardService.createCard(any(CardCreateDTO.class)))
                .thenThrow(new CardAlreadyExistsException("Card number already exists"));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardCreateDTO)))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Card number already exists"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testFindCardByIdAdminSuccess() throws Exception {
        when(cardService.getCard(anyLong())).thenReturn(cardResponseDTO);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/cards/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1L))
                .andExpect(MockMvcResultMatchers.jsonPath("$.maskedNumber").value("**** **** **** 3456"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testFindCardByIdUserSuccess() throws Exception {
        when(cardService.getCard(anyLong())).thenReturn(cardResponseDTO);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/cards/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1L));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testFindCardByIdAccessDenied() throws Exception {
        when(cardService.getCard(anyLong()))
                .thenThrow(new AccessDeniedException("Access denied to this card"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/cards/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Access denied to this card"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testFindCardByIdNotFound() throws Exception {
        when(cardService.getCard(anyLong()))
                .thenThrow(new CardNotFoundException("Card with id '1' not found"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/cards/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Card with id '1' not found"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testFindAllCardsAdminSuccess() throws Exception {
        when(cardService.getAllCards(any(Pageable.class))).thenReturn(cardPage);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/cards/all")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].id").value(1L))
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testFindAllCardsUserSuccess() throws Exception {
        when(cardService.getAllCards(any(Pageable.class))).thenReturn(cardPage);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/cards/all")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].id").value(1L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testFindAllCardsNotFound() throws Exception {
        when(cardService.getAllCards(any(Pageable.class)))
                .thenThrow(new CardNotFoundException("Cards not found"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/cards/all")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Cards not found"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testBlockCardAdminSuccess() throws Exception {
        when(cardService.blockCard(anyLong())).thenReturn(cardResponseDTO);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/cards/1/block")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1L));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testBlockCardUserSuccess() throws Exception {
        when(cardService.blockCard(anyLong())).thenReturn(cardResponseDTO);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/cards/1/block")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1L));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testBlockCardAccessDenied() throws Exception {
        when(cardService.blockCard(anyLong()))
                .thenThrow(new AccessDeniedException("Cannot request block card for another user"));

        mockMvc.perform(MockMvcRequestBuilders.put("/api/cards/1/block")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Cannot request block card for another user"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testBlockCardNotFound() throws Exception {
        when(cardService.blockCard(anyLong()))
                .thenThrow(new CardNotFoundException("Card with id '1' not found"));

        mockMvc.perform(MockMvcRequestBuilders.put("/api/cards/1/block")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Card with id '1' not found"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUnblockCardSuccess() throws Exception {
        when(cardService.unblockCard(anyLong())).thenReturn(cardResponseDTO);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/cards/1/unblock")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1L));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testUnblockCardAccessDenied() throws Exception {
        when(cardService.unblockCard(anyLong()))
                .thenThrow(new AccessDeniedException("Only ADMIN can unblock cards"));

        mockMvc.perform(MockMvcRequestBuilders.put("/api/cards/1/unblock")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Only ADMIN can unblock cards"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUnblockCardNotFound() throws Exception {
        when(cardService.unblockCard(anyLong()))
                .thenThrow(new CardNotFoundException("Card with id '1' not found"));

        mockMvc.perform(MockMvcRequestBuilders.put("/api/cards/1/unblock")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Card with id '1' not found"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUnblockCardExpired() throws Exception {
        when(cardService.unblockCard(anyLong()))
                .thenThrow(new IllegalStateException("Cannot activate expired card"));

        mockMvc.perform(MockMvcRequestBuilders.put("/api/cards/1/unblock")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Cannot activate expired card"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeleteCardSuccess() throws Exception {
        doNothing().when(cardService).deleteCard(anyLong());

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/cards/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testDeleteCardAccessDenied() throws Exception {
        doThrow(new AccessDeniedException("Only ADMIN can delete cards")).when(cardService).deleteCard(anyLong());

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/cards/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Only ADMIN can delete cards"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeleteCardNotFound() throws Exception {
        doThrow(new CardNotFoundException("Card with id '1' not found")).when(cardService).deleteCard(anyLong());

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/cards/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Card with id '1' not found"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeleteCardPositiveBalance() throws Exception {
        doThrow(new IllegalStateException("Cannot delete card with positive balance")).when(cardService).deleteCard(anyLong());

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/cards/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Cannot delete card with positive balance"));
    }
}
