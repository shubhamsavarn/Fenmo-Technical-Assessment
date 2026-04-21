package com.expensetracker.application.dto;

import org.springframework.data.domain.Page;
import java.util.List;

/**
 * Composite result for the GET /expenses endpoint.
 * Contains the paginated expense list AND the aggregate summary
 * (total amount for ALL matching records, not just the current page).
 */
public record ExpensePageResult(
        List<ExpenseResult> expenses,
        PageInfo pageInfo,
        SummaryInfo summary
) {
    public record PageInfo(
            int number,         // Current page (0-indexed)
            int size,           // Items per page
            long totalElements, // Total matching records
            int totalPages      // Total pages
    ) {
        public static PageInfo from(Page<?> page) {
            return new PageInfo(
                    page.getNumber(),
                    page.getSize(),
                    page.getTotalElements(),
                    page.getTotalPages()
            );
        }
    }

    public record SummaryInfo(
            String totalAmount, // Display string for ALL filtered records
            long totalPaise,    // Raw paise for ALL filtered records
            long count,         // Count of ALL filtered records
            String currency     // Always "INR"
    ) {}
}
