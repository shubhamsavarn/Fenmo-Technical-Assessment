package com.expensetracker.infrastructure.persistence;

import com.expensetracker.domain.Money;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Maps between the domain model and the JPA entity.
 *
 * <p>This boundary ensures that:
 * <ul>
 *   <li>Domain objects (Money) stay framework-free</li>
 *   <li>JPA entities stay persistence-focused</li>
 *   <li>Neither leaks into the other's layer</li>
 * </ul>
 */
@Component
public class ExpenseMapper {

    /**
     * Creates a new JPA entity from domain inputs.
     * Called during expense creation after validation.
     */
    public ExpenseEntity toEntity(UUID idempotencyKey, Money amount,
                                  String category, String description,
                                  LocalDate expenseDate) {
        return new ExpenseEntity(
                idempotencyKey,
                amount.toPaise(),
                category.toLowerCase().trim(),
                description != null ? description.trim() : "",
                expenseDate
        );
    }

    /**
     * Extracts the Money value object from a persisted entity.
     */
    public Money toMoney(ExpenseEntity entity) {
        return Money.fromPaise(entity.getAmountPaise());
    }
}
