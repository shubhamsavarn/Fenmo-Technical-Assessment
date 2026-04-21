package com.expensetracker.infrastructure.persistence;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

/**
 * JPA Specifications for composable, type-safe query predicates.
 *
 * <p>Specifications act as the <b>Strategy Pattern</b> for queries —
 * each filter is an independent, composable unit that can be combined
 * with AND/OR logic. This eliminates the combinatorial explosion of
 * needing separate @Query methods for each filter combination.</p>
 *
 * <p>Usage: {@code Specification.where(withCategory("food")).and(anotherSpec)}</p>
 */
public final class ExpenseSpecifications {

    private ExpenseSpecifications() {
        // Utility class — no instantiation
    }

    /**
     * Filter expenses by category (case-insensitive exact match).
     * Returns null if category is blank, which Spring Data ignores in
     * Specification.where() — effectively a no-op filter.
     */
    public static Specification<ExpenseEntity> withCategory(String category) {
        if (category == null || category.isBlank()) {
            return null; // No filter applied
        }
        return (Root<ExpenseEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) ->
                cb.equal(
                        cb.lower(root.get("category")),
                        category.toLowerCase().trim()
                );
    }
}
