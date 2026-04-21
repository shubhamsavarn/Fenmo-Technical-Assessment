package com.expensetracker.web.dto;

import java.util.List;

/**
 * API response for paginated expense listing.
 * Includes page metadata and aggregate summary for ALL matching records.
 */
public record ExpensePageResponse(
        List<ExpenseResponse> content,
        PageMeta page,
        SummaryMeta summary
) {
    public record PageMeta(
            int number,
            int size,
            long totalElements,
            int totalPages
    ) {}

    public record SummaryMeta(
            String totalAmount,
            long count,
            String currency
    ) {}
}
