package com.example.bankcards.service;

import com.example.bankcards.dto.TransactionDTO;
import com.example.bankcards.dto.TransactionResponseDTO;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.AccessDeniedException;
import com.example.bankcards.exception.InvalidTransactionException;
import com.example.bankcards.repository.ICardRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final ICardRepository cardRepository;
    private final UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    @Transactional
    public TransactionResponseDTO transfer(TransactionDTO dto) {
        logger.info("Attempting transfer: sourceCardNumber={}, destinationCardNumber={}, amount={}",
                dto.getSourceCardNumber(), dto.getDestinationCardNumber(), dto.getAmount());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.getUserByUsername(auth.getName());
        logger.debug("Authenticated user: username={}", currentUser.getUsername());

        Card sourceCard = cardRepository.findByNumber(dto.getSourceCardNumber())
                .orElseThrow(() -> {
                    logger.error("Source card not found: cardNumber={}", dto.getSourceCardNumber());
                    return new InvalidTransactionException("Source card with number: '" + dto.getSourceCardNumber() + "' not found");
                });
        Card destinationCard = cardRepository.findByNumber(dto.getDestinationCardNumber())
                .orElseThrow(() -> {
                    logger.error("Destination card not found: cardNumber={}", dto.getDestinationCardNumber());
                    return new InvalidTransactionException("Destination card with number: '" + dto.getDestinationCardNumber() + "' not found");
                });

        if (!Objects.equals(sourceCard.getUser().getId(), currentUser.getId()) ||
                !Objects.equals(destinationCard.getUser().getId(), currentUser.getId())) {
            logger.error("Access denied: userId={} cannot transfer between cards sourceId={} and destinationId={}",
                    currentUser.getId(), sourceCard.getId(), destinationCard.getId());
            throw new AccessDeniedException("Cannot transfer between cards of another user");
        }

        if (dto.getAmount() <= 0) {
            logger.error("Amount must be positive");
            throw new InvalidTransactionException("Amount must be positive");
        }

        sourceCard.withdraw(dto.getAmount());
        destinationCard.deposit(dto.getAmount());

        cardRepository.save(sourceCard);
        cardRepository.save(destinationCard);
        logger.info("Transfer completed: sourceCardId={}, destinationCardId={}, amount={}, sourceBalance={}, destinationBalance={}",
                sourceCard.getId(), destinationCard.getId(), dto.getAmount(), sourceCard.getBalance(), destinationCard.getBalance());

        return toResponseDTO(sourceCard, destinationCard);
    }

    private TransactionResponseDTO toResponseDTO(Card sourceCard, Card destinationCard) {
        logger.debug("Converting cards to response DTO: sourceCardId={}, destinationCardId={}",
                sourceCard.getId(), destinationCard.getId());
        return TransactionResponseDTO.builder()
                .sourceId(sourceCard.getId())
                .destinationId(destinationCard.getId())
                .maskedSourceCardNumber(sourceCard.getMaskedNumber())
                .maskedDestinationCardNumber(destinationCard.getMaskedNumber())
                .sourceBalance(Double.parseDouble(String.format(Locale.US, "%.2f", sourceCard.getBalance())))
                .destinationBalance(Double.parseDouble(String.format(Locale.US, "%.2f", destinationCard.getBalance())))
                .owner(sourceCard.getOwner())
                .build();
    }
}
