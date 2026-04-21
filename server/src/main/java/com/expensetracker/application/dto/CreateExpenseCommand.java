package com.expensetracker.application.dto;

import java.util.UUID;

/**
 * Command object for creating an expense.
 * Constructed by the web layer after parsing and validating the HTTP request.
 * Consumed by the service layer — framework-agnostic.
 */
public record CreateExpenseCommand(
        UUID idempotencyKey,
        String amount,        // Raw string — service converts to Money
        String category,
        String description,
        String date           // ISO 8601 date string
) {}
