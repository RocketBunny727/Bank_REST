package com.example.bankcards.service;

import com.example.bankcards.dto.CardCreateDTO;
import com.example.bankcards.dto.CardResponseDTO;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.AccessDeniedException;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.repository.ICardRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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

    @Transactional
    public CardResponseDTO createCard(CardCreateDTO dto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) userService.loadUserByUsername(auth.getName());
        if (!currentUser.getRole().equals(Role.ADMIN)) {
            throw new AccessDeniedException("Only ADMIN can create cards");
        }
        if (dto.getCardNumber().length() != 19 || !dto.getCardNumber().matches("\\d+")) {
            throw new IllegalArgumentException("Card number must be 16 digits");
        }
        if (dto.getExpiryDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Expiry date cannot be in the past");
        }

        Card card = Card.builder()
                .number(dto.getCardNumber())
                .owner(dto.getUser().getName() + dto.getUser().getSurname())
                .expiryDate(dto.getExpiryDate())
                .status(dto.getStatus())
                .balance(dto.getBalance())
                .user(dto.getUser())
                .isBlockRequested(false)
                .build();
        card = cardRepository.save(card);

        return toResponseDTO(card);
    }

    @Transactional
    public CardResponseDTO getCard(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException("Card with id '" + id + "' not found"));
        checkCardAccess(card);
        return toResponseDTO(card);
    }

    @Transactional
    public Page<CardResponseDTO> getCards(Pageable pageable, String status) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) userService.loadUserByUsername(auth.getName());
        Page<Card> cards;
        if (currentUser.getRole().equals(Role.ADMIN)) {
                cards = cardRepository.findAll(pageable);
        } else {
            if (status != null && !status.isEmpty()) {
                cards = cardRepository.findByUserIdAndStatus(currentUser.getId(), CardStatus.valueOf(status), pageable);
            } else {
                cards = cardRepository.findByUserId(currentUser.getId(), pageable);
            }
        }
        return cards.map(this::toResponseDTO);
    }

    @Transactional
    public CardResponseDTO blockCard(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException("Card with id '" + id + "' not found"));
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) userService.loadUserByUsername(auth.getName());
        if (currentUser.getRole().equals(Role.ADMIN)) {
            card.blockCard();
            card.setBlockRequested(false);
        } else if (currentUser.getRole().equals(Role.USER) && Objects.equals(card.getUser().getId(), currentUser.getId())) {
            card.setBlockRequested(true);
        }else {
            throw new AccessDeniedException("Cannot request block card for another user");
        }

        return toResponseDTO(cardRepository.save(card));
    }

    @Transactional
    public CardResponseDTO unblockCard(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException("Card with id '" + id + "' not found"));
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) userService.loadUserByUsername(auth.getName());
        if (!currentUser.getRole().equals(Role.ADMIN)) {
            throw new AccessDeniedException("Only ADMIN can unblock cards");
        }
        if (card.getStatus() == CardStatus.EXPIRED) {
            throw new IllegalStateException("Cannot activate expired card");
        }
        card.unblockCard();
        card.setBlockRequested(false);

        return toResponseDTO(cardRepository.save(card));
    }

    @Transactional
    public void deleteCard(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException("Card with id '" + id + "' not found"));
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) userService.loadUserByUsername(auth.getName());
        if (!currentUser.getRole().equals(Role.ADMIN)) {
            throw new AccessDeniedException("Only ADMIN can delete cards");
        }
        if (card.getBalance() > 0) {
            throw new IllegalStateException("Cannot delete card with positive balance");
        }
        cardRepository.delete(card);
    }

    private CardResponseDTO toResponseDTO(Card card) {
        return CardResponseDTO.builder()
                .id(card.getId())
                .maskedNumber(card.getMaskedNumber())
                .balance(card.getBalance())
                .expiryDate(card.getExpiryDate())
                .status(card.getStatus())
                .owner(card.getOwner())
                .isBlockRequested(card.isBlockRequested())
                .build();
    }

    private void checkCardAccess(Card card) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) userService.loadUserByUsername(auth.getName());
        if (!currentUser.getRole().equals(Role.ADMIN) && !(card.getUser().getId() == currentUser.getId())) {
            throw new AccessDeniedException("Access denied to this card");
        }
    }
}
