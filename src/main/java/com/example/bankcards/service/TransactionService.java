package com.example.bankcards.service;

import com.example.bankcards.dto.TransactionDTO;
import com.example.bankcards.dto.TransactionResponseDTO;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.AccessDeniedException;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.InvalidTransactionException;
import com.example.bankcards.repository.ICardRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final ICardRepository cardRepository;
    private final UserService userService;
    private final CardService cardService;

    @Transactional
    public TransactionResponseDTO transfer(TransactionDTO dto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) userService.loadUserByUsername(auth.getName());

        Card sourceCard = cardRepository.findById(dto.getFromCardId())
                .orElseThrow(() -> new CardNotFoundException("Source card with id: '" + dto.getFromCardId() + "' not found"));
        Card destinationCard = cardRepository.findById(dto.getToCardId())
                .orElseThrow(() -> new CardNotFoundException("Destination card with id: '" + dto.getToCardId() + "' not found"));

        if (!Objects.equals(sourceCard.getUser().getId(), currentUser.getId()) ||
                !Objects.equals(destinationCard.getUser().getId(), currentUser.getId())) {
            throw new AccessDeniedException("Cannot transfer between cards of another user");
        }

        if (!sourceCard.withdraw(dto.getAmount()) || !destinationCard.deposit(dto.getAmount())) {
            throw new InvalidTransactionException("Invalid transaction");
        }

        cardRepository.save(sourceCard);
        cardRepository.save(destinationCard);
        return toResponseDTO(sourceCard, destinationCard);
    }

    private TransactionResponseDTO toResponseDTO(Card sourceCard, Card destinationCard) {
        return TransactionResponseDTO.builder()
                .sourceId(sourceCard.getId())
                .destinationId(destinationCard.getId())
                .maskedSourceCardNumber(sourceCard.getMaskedNumber())
                .maskedDestinationCardNumber(destinationCard.getMaskedNumber())
                .sourceBalance(sourceCard.getBalance())
                .destinationBalance(destinationCard.getBalance())
                .owner(sourceCard.getOwner())
                .build();
    }
}
