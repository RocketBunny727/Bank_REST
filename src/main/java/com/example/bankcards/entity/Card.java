package com.example.bankcards.entity;

import com.example.bankcards.util.ExpiryDateConverter;
import com.example.bankcards.util.NumberMasker;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnTransformer;

import java.time.LocalDate;

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
    private long id;

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

    @Column(nullable = false)
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

    public boolean canTransact() {
        return this.status == CardStatus.ACTIVE;
    }

    public boolean withdraw(double amount) {
        if (!canTransact() || amount <= 0 || this.balance < amount) {
            return false;
        }
        this.balance -= amount;
        return true;
    }

    public boolean deposit(double amount) {
        if (!canTransact() || amount <= 0) {
            return false;
        }
        this.balance += amount;
        return true;
    }

    @JoinColumn(name = "user_id")
    @ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private User user;

    @Column(nullable = false)
    private boolean isBlockRequested;
}
