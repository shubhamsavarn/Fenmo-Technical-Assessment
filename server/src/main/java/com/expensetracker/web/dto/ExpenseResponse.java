package com.expensetracker.web.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * API response for a single expense.
 * Amount is always a String — never a JSON number — to prevent float precision issues.
 */
public record ExpenseResponse(
        UUID id,
        String amount,
        String category,
        String description,
        LocalDate date,
        Instant createdAt
) {}
