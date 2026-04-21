package com.expensetracker.infrastructure.persistence;

/**
 * Projection interface for the summary aggregate query.
 * Spring Data JPA auto-maps result columns to these getter methods.
 */
public interface ExpenseSummaryProjection {

    Long getTotalPaise();

    Long getCount();
}
