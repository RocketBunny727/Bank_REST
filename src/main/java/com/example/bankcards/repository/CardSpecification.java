package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CardSpecification {

    public static Specification<Card> withFilter(
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
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (userId != null) {
                predicates.add(criteriaBuilder.equal(root.get("user").get("id"), userId));
            }

            if (number != null) {
                predicates.add(criteriaBuilder.equal(root.get("number"), number));
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (owner != null) {
                predicates.add(criteriaBuilder.equal(root.get("owner"), owner));
            }

            if (balance != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("balance"), balance));
            }

            if (expiryDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("expiryDate"), expiryDate));
            }

            if (isBlockRequired) {
                predicates.add(criteriaBuilder.equal(root.get("blockRequired"), false));
            }

            if (minBalance != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("minBalance"), minBalance));
            }

            if (maxBalance != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("maxBalance"), maxBalance));
            }

            if (expiryDateFrom != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("expiryDate"), expiryDateFrom));
            }

            if (expiryDateTo != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("expiryDate"), expiryDateTo));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
