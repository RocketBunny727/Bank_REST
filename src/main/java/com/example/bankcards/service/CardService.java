package com.example.bankcards.service;

import com.example.bankcards.dto.CardCreateDTO;
import com.example.bankcards.dto.CardResponseDTO;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.*;
import com.example.bankcards.repository.CardSpecification;
import com.example.bankcards.repository.ICardRepository;
import com.example.bankcards.util.ExpiryDateConverter;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CardService {

    private final ICardRepository cardRepository;
    private final UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(CardService.class);

    @Transactional
    public CardResponseDTO createCard(CardCreateDTO dto) {
        logger.info("Attempting to create card for userId={}", dto.getUserId());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.getUserByUsername(auth.getName());
        if (!currentUser.getRole().equals(Role.ADMIN)) {
            logger.error("Access denied: user={} is not ADMIN", currentUser.getUsername());
            throw new AccessDeniedException("Only ADMIN can create cards");
        }
        if (dto.getCardNumber() == null) {
            logger.error("Card number is null");
            throw new MissingCardNumberException("Card number is null");
        }
        if (dto.getCardNumber().length() != 19 || !dto.getCardNumber().matches("\\d{4} \\d{4} \\d{4} \\d{4}")) {
            logger.error("Invalid card number format: cardNumber={}", dto.getCardNumber());
            throw new IllegalArgumentException("Card number must be 16 digits");
        }
        if(cardRepository.findByNumber(dto.getCardNumber()).isPresent()) {
            logger.error("Card number already exists");
            throw new CardAlredyExistsException("Card number already exists");
        }
        if (dto.getExpiryDate() == null) {
            logger.error("Expiry date is null");
            throw new MissingExpiryDateException("Expiry Date is null");
        }
        LocalDate expiryDate = ExpiryDateConverter.convertStringToDate(dto.getExpiryDate());

        if (expiryDate.isBefore(LocalDate.now())) {
            logger.error("Expiry date is in the past: expiryDate={}", expiryDate);
            throw new IllegalArgumentException("Expiry date cannot be in the past");
        }

        if (dto.getBalance() == null) {
            logger.warn("Balance is null, setting to 0.0");
            dto.setBalance(0.0);
        }

        User cardUser = userService.getUserById(dto.getUserId());
        Card card = Card.builder()
                .number(dto.getCardNumber())
                .owner(cardUser.getName() + " " + cardUser.getSurname())
                .expiryDate(expiryDate)
                .status(CardStatus.ACTIVE)
                .balance(dto.getBalance())
                .user(cardUser)
                .isBlockRequested(false)
                .build();
        card = cardRepository.save(card);
        logger.info("Successfully created card: cardId={}, maskedNumber={}", card.getId(), card.getMaskedNumber());

        return toResponseDTO(card);
    }

    @Transactional
    public CardResponseDTO getCard(Long id) {
        logger.info("Fetching card with id={}", id);
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Card not found: cardId={}", id);
                    return new CardNotFoundException("Card with id '" + id + "' not found");
                });
        checkCardAccess(card);
        logger.info("Successfully retrieved card: cardId={}, maskedNumber={}", card.getId(), card.getMaskedNumber());
        return toResponseDTO(card);
    }

    @Transactional
    public Page<CardResponseDTO> getAllCards(Pageable pageable) {
        logger.info("Fetching all cards for pageable={}", pageable);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.getUserByUsername(auth.getName());
        Page<Card> cards;
        if (currentUser.getRole().equals(Role.ADMIN)) {
            logger.debug("User is ADMIN, fetching all cards");
            cards = cardRepository.findAll(pageable);
        } else {
            logger.debug("User is USER, fetching cards for userId={}", currentUser.getId());
            cards = cardRepository.findByUserId(currentUser.getId(), pageable);
        }

        if (cards.isEmpty()) {
            logger.warn("No cards found for user={}", currentUser.getUsername());
            throw new CardNotFoundException("Cards not found");
        }

        logger.info("Successfully retrieved {} all available cards", cards.getTotalElements());
        return cards.map(this::toResponseDTO);
    }

    @Transactional
    public Page<CardResponseDTO> getFilteredCards(
            Pageable pageable,
            Long userId,
            String number,
            CardStatus status,
            String owner,
            Double balance,
            LocalDate expiryDate,
            boolean isBlockRequired,
            Double minBalance,
            Double maxBalance,
            LocalDate expiryDateFrom,
            LocalDate expiryDateTo
    ) {
        logger.info("Fetching filtered cards: userId={}, number={}, status={}, owner={}, balance={}, expiryDate={}, isBlockRequired={}, minBalance={}, maxBalance={}, expiryDateFrom={}, expiryDateTo={}",
                userId, number, status, owner, balance, expiryDate, isBlockRequired, minBalance, maxBalance, expiryDateFrom, expiryDateTo);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.getUserByUsername(auth.getName());

        if (minBalance != null && maxBalance != null && minBalance > maxBalance) {
            logger.error("Invalid balance range: minBalance={} > maxBalance={}", minBalance, maxBalance);
            throw new IllegalArgumentException("minBalance cannot be greater than maxBalance");
        }
        if (expiryDateFrom != null && expiryDateTo != null && expiryDateFrom.isAfter(expiryDateTo)) {
            logger.error("Invalid expiry date range: expiryDateFrom={} > expiryDateTo={}", expiryDateFrom, expiryDateTo);
            throw new IllegalArgumentException("expiryDateFrom cannot be greater than expiryDateTo");
        }

        Long currentUserId = currentUser.getRole().equals(Role.ADMIN) ? null : currentUser.getId();
        Page<Card> cards = cardRepository.findAll(
                CardSpecification.withFilter(currentUserId, number, status, owner, balance, expiryDate, isBlockRequired,
                        minBalance, maxBalance, expiryDateFrom, expiryDateTo),
                pageable
        );

        logger.info("Successfully retrieved {} filtered cards", cards.getTotalElements());
        return cards.map(this::toResponseDTO);
    }

    @Transactional
    public CardResponseDTO blockCard(Long id) {
        logger.info("Attempting to block card: cardId={}", id);
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Card not found (tried to block card): cardId={}", id);
                    return new CardNotFoundException("Card with id '" + id + "' not found");
                });
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.getUserByUsername(auth.getName());
        if (currentUser.getRole().equals(Role.ADMIN)) {
            card.blockCard();
            card.setBlockRequested(false);
            logger.info("Card blocked by ADMIN: cardId={}, maskedNumber={}", card.getId(), card.getMaskedNumber());
        } else if (currentUser.getRole().equals(Role.USER) && Objects.equals(card.getUser().getId(), currentUser.getId())) {
            card.setBlockRequested(true);
            logger.info("Block requested by USER: cardId={}, maskedNumber={}", card.getId(), card.getMaskedNumber());
        } else {
            logger.error("Access denied: user={} cannot block cardId={}", currentUser.getUsername(), id);
            throw new AccessDeniedException("Cannot request block card for another user");
        }

        Card savedCard = cardRepository.save(card);
        logger.info("Card status updated: cardId={}, status={}, blockRequested={}", savedCard.getId(), savedCard.getStatus(), savedCard.isBlockRequested());
        return toResponseDTO(savedCard);
    }

    @Transactional
    public CardResponseDTO unblockCard(Long id) {
        logger.info("Attempting to unblock card: cardId={}", id);
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Card not found (tried to unblock card): cardId={}", id);
                    return new CardNotFoundException("Card with id '" + id + "' not found");
                });
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.getUserByUsername(auth.getName());
        if (!currentUser.getRole().equals(Role.ADMIN)) {
            logger.error("Access denied (tried to unblock card): user={} is not ADMIN", currentUser.getUsername());
            throw new AccessDeniedException("Only ADMIN can unblock cards");
        }
        if (card.getStatus() == CardStatus.EXPIRED) {
            logger.error("Cannot unblock expired card: cardId={}", id);
            throw new IllegalStateException("Cannot activate expired card");
        }
        card.unblockCard();
        card.setBlockRequested(false);
        Card savedCard = cardRepository.save(card);
        logger.info("Card unblocked: cardId={}, maskedNumber={}", savedCard.getId(), savedCard.getMaskedNumber());
        return toResponseDTO(savedCard);
    }

    @Transactional
    public void deleteCard(Long id) {
        logger.info("Attempting to delete card: cardId={}", id);
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Card not found (tried to delete card): cardId={}", id);
                    return new CardNotFoundException("Card with id '" + id + "' not found");
                });
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.getUserByUsername(auth.getName());
        if (!currentUser.getRole().equals(Role.ADMIN)) {
            logger.error("Access denied (tried to delete card): user={} is not ADMIN", currentUser.getUsername());
            throw new AccessDeniedException("Only ADMIN can delete cards");
        }
        if (card.getBalance() > 0) {
            logger.error("Cannot delete card with positive balance: cardId={}, balance={}", id, card.getBalance());
            throw new IllegalStateException("Cannot delete card with positive balance");
        }
        cardRepository.delete(card);
        logger.info("Card deleted: cardId={}, maskedNumber={}", id, card.getMaskedNumber());
    }

    private CardResponseDTO toResponseDTO(Card card) {
        return CardResponseDTO.builder()
                .id(card.getId())
                .maskedNumber(card.getMaskedNumber())
                .balance(card.getBalance())
                .expiryDate(ExpiryDateConverter.convertDateToString(card.getExpiryDate()))
                .status(card.getStatus())
                .owner(card.getOwner())
                .isBlockRequested(card.isBlockRequested())
                .build();
    }

    private void checkCardAccess(Card card) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.getUserByUsername(auth.getName());
        if (!currentUser.getRole().equals(Role.ADMIN) && !Objects.equals(card.getUser().getId(), currentUser.getId())) {
            logger.error("Access denied: user={} cannot access cardId={}", currentUser.getUsername(), card.getId());
            throw new AccessDeniedException("Access denied to this card");
        }
        logger.debug("Access granted for cardId={} to user={}", card.getId(), currentUser.getUsername());
    }
}
