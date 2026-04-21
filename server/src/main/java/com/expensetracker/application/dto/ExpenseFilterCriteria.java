package com.expensetracker.application.dto;

/**
 * Encapsulates query parameters for listing expenses.
 * Built by the controller from validated HTTP query params.
 */
public record ExpenseFilterCriteria(
        String category,    // null means no filter
        String sort,        // "date_desc", "date_asc", "amount_desc", "amount_asc"
        int page,           // 0-indexed (Spring convention)
        int size            // items per page
) {
    public ExpenseFilterCriteria {
        if (sort == null || sort.isBlank()) sort = "date_desc";
        if (page < 0) page = 0;
        if (size < 1) size = 20;
        if (size > 100) size = 100;
    }
}
