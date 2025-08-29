package com.example.bankcards.service;

import com.example.bankcards.dto.TransactionDTO;
import com.example.bankcards.dto.TransactionResponseDTO;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.*;
import com.example.bankcards.repository.ICardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class TransactionServiceTest {

    @InjectMocks
    private TransactionService transactionService;

    @Mock
    private ICardRepository cardRepository;

    @Mock
    private UserService userService;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    private User user;
    private Card sourceCard;
    private Card destinationCard;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user = User.builder()
                .id(1L)
                .username("john_doe")
                .role(Role.USER)
                .name("John")
                .surname("Doe")
                .build();
        sourceCard = Card.builder()
                .id(1L)
                .number("1111 1111 1111 1111")
                .owner("John Doe")
                .expiryDate(LocalDate.of(2026, 12, 31))
                .status(CardStatus.ACTIVE)
                .balance(100.0)
                .user(user)
                .isBlockRequested(false)
                .build();
        destinationCard = Card.builder()
                .id(2L)
                .number("2222 2222 2222 2222")
                .owner("John Doe")
                .expiryDate(LocalDate.of(2026, 12, 31))
                .status(CardStatus.ACTIVE)
                .balance(50.0)
                .user(user)
                .isBlockRequested(false)
                .build();

        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void transferSuccess() {
        TransactionDTO dto = TransactionDTO.builder()
                .sourceCardNumber("1111 1111 1111 1111")
                .destinationCardNumber("2222 2222 2222 2222")
                .amount(50.0)
                .build();
        when(authentication.getName()).thenReturn("john_doe");
        when(userService.getUserByUsername("john_doe")).thenReturn(user);
        when(cardRepository.findByNumber("1111 1111 1111 1111")).thenReturn(Optional.of(sourceCard));
        when(cardRepository.findByNumber("2222 2222 2222 2222")).thenReturn(Optional.of(destinationCard));
        when(cardRepository.save(any(Card.class))).thenReturn(sourceCard).thenReturn(destinationCard);

        TransactionResponseDTO response = transactionService.transfer(dto);

        assertEquals(1L, response.getSourceId());
        assertEquals(2L, response.getDestinationId());
        assertEquals(50.0, response.getSourceBalance());
        assertEquals(100.0, response.getDestinationBalance());
        verify(cardRepository, times(2)).save(any(Card.class));
    }

    @Test
    void transferSourceCardNotFoundThrowsCardNotFoundException() {
        TransactionDTO dto = TransactionDTO.builder()
                .sourceCardNumber("1111 1111 1111 1111")
                .destinationCardNumber("2222 2222 2222 2222")
                .amount(50.0)
                .build();
        when(authentication.getName()).thenReturn("john_doe");
        when(userService.getUserByUsername("john_doe")).thenReturn(user);
        when(cardRepository.findByNumber("1111 1111 1111 1111")).thenReturn(Optional.empty());

        assertThrows(InvalidTransactionException.class, () -> transactionService.transfer(dto));
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void transferDestinationCardNotFoundThrowsCardNotFoundException() {
        TransactionDTO dto = TransactionDTO.builder()
                .sourceCardNumber("1111 1111 1111 1111")
                .destinationCardNumber("2222 2222 2222 2222")
                .amount(50.0)
                .build();
        when(authentication.getName()).thenReturn("john_doe");
        when(userService.getUserByUsername("john_doe")).thenReturn(user);
        when(cardRepository.findByNumber("1111 1111 1111 1111")).thenReturn(Optional.of(sourceCard));
        when(cardRepository.findByNumber("2222 2222 2222 2222")).thenReturn(Optional.empty());

        assertThrows(InvalidTransactionException.class, () -> transactionService.transfer(dto));
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void transferAccessDeniedThrowsAccessDeniedException() {
        User otherUser = User.builder()
                .id(2L)
                .username("jane_smith")
                .role(Role.USER)
                .name("Jane")
                .surname("Smith")
                .build();
        Card otherCard = Card.builder()
                .id(2L)
                .number("3333 3333 3333 3333")
                .owner("Jane Smith")
                .expiryDate(LocalDate.of(2026, 12, 31))
                .status(CardStatus.ACTIVE)
                .balance(50.0)
                .user(otherUser)
                .isBlockRequested(false)
                .build();
        TransactionDTO dto = TransactionDTO.builder()
                .sourceCardNumber("1111 1111 1111 1111")
                .destinationCardNumber("3333 3333 3333 3333")
                .amount(50.0)
                .build();
        when(authentication.getName()).thenReturn("john_doe");
        when(userService.getUserByUsername("john_doe")).thenReturn(user);
        when(cardRepository.findByNumber("1111 1111 1111 1111")).thenReturn(Optional.of(sourceCard));
        when(cardRepository.findByNumber("3333 3333 3333 3333")).thenReturn(Optional.of(otherCard));

        assertThrows(AccessDeniedException.class, () -> transactionService.transfer(dto));
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void transferInsufficientFundsThrowsInsufficientFundsException() {
        TransactionDTO dto = TransactionDTO.builder()
                .sourceCardNumber("1111 1111 1111 1111")
                .destinationCardNumber("2222 2222 2222 2222")
                .amount(200.0)
                .build();
        when(authentication.getName()).thenReturn("john_doe");
        when(userService.getUserByUsername("john_doe")).thenReturn(user);
        when(cardRepository.findByNumber("1111 1111 1111 1111")).thenReturn(Optional.of(sourceCard));
        when(cardRepository.findByNumber("2222 2222 2222 2222")).thenReturn(Optional.of(destinationCard));

        assertThrows(InsufficientFundsException.class, () -> transactionService.transfer(dto));
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void transferBlockedCardThrowsBlockedCardException() {
        Card blockedCard = Card.builder()
                .id(1L)
                .number("1234 5678 9012 3456")
                .owner("John Doe")
                .expiryDate(LocalDate.of(2026, 12, 31))
                .status(CardStatus.BLOCKED)
                .balance(100.0)
                .user(user)
                .isBlockRequested(false)
                .build();

        TransactionDTO dto = TransactionDTO.builder()
                .sourceCardNumber("1234 5678 9012 3456")
                .destinationCardNumber("9876 5432 1098 7654")
                .amount(50.0)
                .build();
        when(authentication.getName()).thenReturn("john_doe");
        when(userService.getUserByUsername("john_doe")).thenReturn(user);
        when(cardRepository.findByNumber("1234 5678 9012 3456")).thenReturn(Optional.of(blockedCard));
        when(cardRepository.findByNumber("9876 5432 1098 7654")).thenReturn(Optional.of(destinationCard));

        assertThrows(CardBlockedException.class, () -> transactionService.transfer(dto));
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void transferExpiredCardThrowsExpiredCardException() {
        Card expiredCard = Card.builder()
                .id(1L)
                .number("1234 5678 9012 3456")
                .owner("John Doe")
                .expiryDate(LocalDate.of(2023, 12, 31))
                .status(CardStatus.EXPIRED)
                .balance(100.0)
                .user(user)
                .isBlockRequested(false)
                .build();

        TransactionDTO dto = TransactionDTO.builder()
                .sourceCardNumber("1234 5678 9012 3456")
                .destinationCardNumber("9876 5432 1098 7654")
                .amount(50.0)
                .build();
        when(authentication.getName()).thenReturn("john_doe");
        when(userService.getUserByUsername("john_doe")).thenReturn(user);
        when(cardRepository.findByNumber("1234 5678 9012 3456")).thenReturn(Optional.of(expiredCard));
        when(cardRepository.findByNumber("9876 5432 1098 7654")).thenReturn(Optional.of(destinationCard));

        assertThrows(CardExpiredException.class, () -> transactionService.transfer(dto));
        verify(cardRepository, never()).save(any(Card.class));
    }
}
