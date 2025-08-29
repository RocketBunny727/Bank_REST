package com.example.bankcards.entity;

import com.example.bankcards.exception.CardBlockedException;
import com.example.bankcards.exception.CardExpiredException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.util.ExpiryDateConverter;
import com.example.bankcards.util.NumberMasker;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnTransformer;

import java.time.LocalDate;
import java.util.Locale;

@Entity
@Table(name = "cards")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @ColumnTransformer(
            read = "pgp_sym_decrypt(number, current_setting('encrypt.key'))",
            write = "pgp_sym_encrypt(?, current_setting('encrypt.key'))"
    )
    private String number;

    public String getMaskedNumber() {
        return NumberMasker.mask(number);
    }

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CardStatus status;

    @Column(nullable = false, columnDefinition = "numeric(15, 2)")
    private String owner;

    @Column(nullable = false)
    private double balance;

    @Column(nullable = false)
    private LocalDate expiryDate;

    public String getFormattedExpiryDate() {
        return ExpiryDateConverter.convertDateToString(this.expiryDate);
    }

    public void blockCard() {
        this.status = CardStatus.BLOCKED;
    }

    public void unblockCard() {
        if (this.expiryDate != null && !this.expiryDate.isBefore(LocalDate.now())) {
            this.status = CardStatus.ACTIVE;
        }
    }

    @PrePersist
    @PreUpdate
    public void updateExpireStatus() {
        if (this.expiryDate != null && this.expiryDate.isBefore(LocalDate.now())) {
            this.status = CardStatus.EXPIRED;
        }
    }

    public void withdraw(double amount) {
        if (amount <= 0 || this.balance < amount) {
            throw new InsufficientFundsException("Insufficient funds on source card");
        }

        if (this.status == CardStatus.EXPIRED) {
            throw new CardExpiredException("Source card has expired");
        } else if (this.status == CardStatus.BLOCKED) {
            throw new CardBlockedException("Source card is blocked");
        }

        this.balance -= Double.parseDouble(String.format(Locale.US, "%.2f", amount));
    }

    public void deposit(double amount) {
        if (this.status == CardStatus.EXPIRED) {
            throw new CardExpiredException("Destination card has expired");
        } else if (this.status == CardStatus.BLOCKED) {
            throw new CardBlockedException("Destination card is blocked");
        }

        this.balance += Double.parseDouble(String.format(Locale.US, "%.2f", amount));
    }

    @JoinColumn(name = "user_id")
    @ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private User user;

    @Column(nullable = false)
    private boolean isBlockRequested;
}
