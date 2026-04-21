package com.expensetracker.web.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * API request body for creating an expense.
 * Validated using Jakarta Bean Validation annotations.
 *
 * <p>Amount is a String (not a number) to prevent JavaScript float precision loss
 * during JSON serialization/deserialization.</p>
 */
public record CreateExpenseRequest(

        @NotBlank(message = "Amount is required")
        @Pattern(regexp = "^\\d+(\\.\\d{1,2})?$",
                 message = "Amount must be a valid number with up to 2 decimal places")
        String amount,

        @NotBlank(message = "Category is required")
        @Size(min = 1, max = 50, message = "Category must be between 1 and 50 characters")
        String category,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        @NotNull(message = "Date is required")
        LocalDate date
) {}
