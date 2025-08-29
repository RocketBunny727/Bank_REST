package com.example.bankcards.service;

import com.example.bankcards.dto.CardCreateDTO;
import com.example.bankcards.dto.CardResponseDTO;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.AccessDeniedException;
import com.example.bankcards.exception.CardAlreadyExistsException;
import com.example.bankcards.repository.ICardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CardServiceTest {

    @InjectMocks
    private CardService cardService;

    @Mock
    private ICardRepository cardRepository;

    @Mock
    private UserService userService;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    private User adminUser;
    private User regularUser;
    private Card card;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        adminUser = User.builder().id(1L).username("admin").role(Role.ADMIN).name("Admin").surname("User").build();
        regularUser = User.builder().id(2L).username("user").role(Role.USER).name("John").surname("Doe").build();
        card = Card.builder()
                .id(1L)
                .number("1111 1111 1111 1111")
                .owner("John Doe")
                .expiryDate(LocalDate.of(2026, 12, 1))
                .status(CardStatus.ACTIVE)
                .balance(100.0)
                .user(regularUser)
                .isBlockRequested(false)
                .build();

        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void createCard_adminSuccess() {
        when(authentication.getName()).thenReturn("admin");
        when(userService.getUserByUsername("admin")).thenReturn(adminUser);
        when(userService.getUserById(2L)).thenReturn(regularUser);
        when(cardRepository.findByNumber("1111 1111 1111 1111")).thenReturn(Optional.empty());
        when(cardRepository.save(any(Card.class))).thenReturn(card);

        CardCreateDTO dto = CardCreateDTO.builder()
                .cardNumber("1111 1111 1111 1111")
                .expiryDate("12/26")
                .balance(100.0)
                .userId(2L)
                .build();

        CardResponseDTO response = cardService.createCard(dto);

        assertEquals(1L, response.getId());
        assertEquals("**** **** **** 1111", response.getMaskedNumber());
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void createCard_nonAdmin_throwsAccessDenied() {
        when(authentication.getName()).thenReturn("user");
        when(userService.getUserByUsername("user")).thenReturn(regularUser);

        CardCreateDTO dto = CardCreateDTO.builder()
                .cardNumber("1111 1111 1111 1111")
                .expiryDate("12/26")
                .balance(100.0)
                .userId(2L)
                .build();

        assertThrows(AccessDeniedException.class, () -> cardService.createCard(dto));
    }

    @Test
    void createCardWithExistsNumberThrowsCardAlreadyExistsException() {
        when(authentication.getName()).thenReturn("admin");
        when(userService.getUserByUsername("admin")).thenReturn(adminUser);
        when(cardRepository.findByNumber("1111 1111 1111 1111")).thenReturn(Optional.of(card));

        CardCreateDTO otherDto = CardCreateDTO.builder()
                .cardNumber("1111 1111 1111 1111")
                .expiryDate("12/26")
                .balance(100.0)
                .userId(2L)
                .build();

        assertThrows(CardAlreadyExistsException.class, () -> cardService.createCard(otherDto));
    }

    @Test
    void getCard_success() {
        when(authentication.getName()).thenReturn("user");
        when(userService.getUserByUsername("user")).thenReturn(regularUser);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        CardResponseDTO response = cardService.getCard(1L);

        assertEquals(1L, response.getId());
        assertEquals("**** **** **** 1111", response.getMaskedNumber());
    }

    @Test
    void getAllCards_adminSuccess() {
        when(authentication.getName()).thenReturn("admin");
        when(userService.getUserByUsername("admin")).thenReturn(adminUser);
        when(cardRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(card), PageRequest.of(0, 10), 1));

        Page<CardResponseDTO> response = cardService.getAllCards(PageRequest.of(0, 10));

        assertEquals(1, response.getTotalElements());
        assertEquals("**** **** **** 1111", response.getContent().get(0).getMaskedNumber());
    }

    @Test
    void blockCard_adminSuccess() {
        Card blockedCard = Card.builder()
                .id(1L)
                .number("1111 1111 1111 1111")
                .owner("John Doe")
                .expiryDate(LocalDate.of(2026, 12, 31))
                .status(CardStatus.BLOCKED)
                .balance(100.0)
                .user(regularUser)
                .isBlockRequested(false)
                .build();

        when(authentication.getName()).thenReturn("admin");
        when(userService.getUserByUsername("admin")).thenReturn(adminUser);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenReturn(blockedCard);

        CardResponseDTO response = cardService.blockCard(1L);

        assertEquals(1L, response.getId());
        assertEquals(CardStatus.BLOCKED, response.getStatus());
        assertFalse(response.isBlockRequested());
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void blockCard_userOwnCard_blockRequested() {
        Card blockRequestedCard = Card.builder()
                .id(1L)
                .number("1111 1111 1111 1111")
                .owner("John Doe")
                .expiryDate(LocalDate.of(2026, 12, 31))
                .status(CardStatus.ACTIVE)
                .balance(100.0)
                .user(regularUser)
                .isBlockRequested(true)
                .build();

        when(authentication.getName()).thenReturn("user");
        when(userService.getUserByUsername("user")).thenReturn(regularUser);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenReturn(blockRequestedCard);

        CardResponseDTO response = cardService.blockCard(1L);

        assertEquals(1L, response.getId());
        assertEquals(CardStatus.ACTIVE, response.getStatus());
        assertTrue(response.isBlockRequested());
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void blockCard_userOtherCard_throwsAccessDenied() {
        User otherUser = User.builder()
                .id(3L)
                .username("other")
                .role(Role.USER)
                .name("Jane")
                .surname("Smith")
                .build();
        Card otherCard = Card.builder()
                .id(2L)
                .number("9876 5432 1098 7654")
                .owner("Jane Smith")
                .expiryDate(LocalDate.of(2026, 12, 31))
                .status(CardStatus.ACTIVE)
                .balance(200.0)
                .user(otherUser)
                .isBlockRequested(false)
                .build();

        when(authentication.getName()).thenReturn("user");
        when(userService.getUserByUsername("user")).thenReturn(regularUser);
        when(cardRepository.findById(2L)).thenReturn(Optional.of(otherCard));

        assertThrows(AccessDeniedException.class, () -> cardService.blockCard(2L));
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void unblockCard_adminSuccess() {
        Card unblockedCard = Card.builder()
                .id(1L)
                .number("1111 1111 1111 1111")
                .owner("John Doe")
                .expiryDate(LocalDate.of(2026, 12, 31))
                .status(CardStatus.ACTIVE)
                .balance(100.0)
                .user(regularUser)
                .isBlockRequested(false)
                .build();

        when(authentication.getName()).thenReturn("admin");
        when(userService.getUserByUsername("admin")).thenReturn(adminUser);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenReturn(unblockedCard);

        CardResponseDTO response = cardService.unblockCard(1L);

        assertEquals(1L, response.getId());
        assertEquals(CardStatus.ACTIVE, response.getStatus());
        assertFalse(response.isBlockRequested());
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void unblockCard_nonAdmin_throwsAccessDenied() {
        when(authentication.getName()).thenReturn("user");
        when(userService.getUserByUsername("user")).thenReturn(regularUser);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        assertThrows(AccessDeniedException.class, () -> cardService.unblockCard(1L));
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void deleteCard_adminSuccess() {
        when(authentication.getName()).thenReturn("admin");
        when(userService.getUserByUsername("admin")).thenReturn(adminUser);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        card.setBalance(0.0);

        cardService.deleteCard(1L);

        verify(cardRepository).delete(card);
    }

    @Test
    void deleteCard_nonAdmin_throwsAccessDenied() {
        when(authentication.getName()).thenReturn("user");
        when(userService.getUserByUsername("user")).thenReturn(regularUser);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        assertThrows(AccessDeniedException.class, () -> cardService.deleteCard(1L));
        verify(cardRepository, never()).delete(any(Card.class));
    }

    @Test
    void deleteCard_positiveBalance_throwsIllegalState() {
        when(authentication.getName()).thenReturn("admin");
        when(userService.getUserByUsername("admin")).thenReturn(adminUser);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        assertThrows(IllegalStateException.class, () -> cardService.deleteCard(1L));
        verify(cardRepository, never()).delete(any(Card.class));
    }
}
