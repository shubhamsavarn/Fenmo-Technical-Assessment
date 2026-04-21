package com.expensetracker.application.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Result object returned by the service layer after creating or retrieving an expense.
 * The controller maps this to the HTTP response DTO.
 */
public record ExpenseResult(
        UUID id,
        String amount,          // Display string e.g., "1250.50"
        long amountPaise,
        String category,
        String description,
        LocalDate expenseDate,
        Instant createdAt,
        boolean isNew           // true = 201 Created, false = 200 OK (idempotent replay)
) {}
