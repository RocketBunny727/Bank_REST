package com.example.bankcards.repository;

import com.example.bankcards.entity.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class UserSpecifications {

    public static Specification<User> withFilters(
           Long id,
           String username,
           String name,
           String surname
    ) {
        return ((root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (id != null) {
                predicates.add(criteriaBuilder.equal(root.get("id"), id));
            }

            if (username != null) {
                predicates.add(criteriaBuilder.equal(root.get("username"), username));
            }

            if (name != null) {
                predicates.add(criteriaBuilder.equal(root.get("name"), name));
            }

            if (surname != null) {
                predicates.add(criteriaBuilder.equal(root.get("surname"), surname));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
    }
}
