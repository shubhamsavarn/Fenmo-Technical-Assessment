package com.expensetracker.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for expense persistence.
 *
 * <p>Extends JpaSpecificationExecutor for dynamic query composition
 * (filter by category, sort by different fields) without writing
 * separate @Query methods for each combination.</p>
 */
@Repository
public interface ExpenseJpaRepository
        extends JpaRepository<ExpenseEntity, UUID>,
                JpaSpecificationExecutor<ExpenseEntity> {

    /**
     * Find an expense by its idempotency key.
     * Used in the idempotency check flow to detect duplicate submissions.
     */
    Optional<ExpenseEntity> findByIdempotencyKey(UUID idempotencyKey);

    /**
     * Get all distinct categories (lowercased), sorted alphabetically.
     * Used to populate the category filter dropdown on the frontend.
     */
    @Query(value = "SELECT DISTINCT LOWER(e.category) FROM expenses e ORDER BY 1",
           nativeQuery = true)
    List<String> findDistinctCategories();

    /**
     * Calculate the total amount (in paise) and count for filtered expenses.
     * This aggregate runs across ALL matching records, ignoring pagination.
     *
     * <p>Used alongside the paginated query so the UI can show
     * "Total: ₹45,230.75 (142 expenses)" even when viewing page 3 of 8.</p>
     *
     * @param category filter category (null means no filter)
     * @return projection with totalPaise and count
     */
    @Query(value = """
            SELECT COALESCE(SUM(e.amount_paise), 0) AS totalPaise,
                   COUNT(e.id) AS count
            FROM expenses e
            WHERE (:category IS NULL OR LOWER(e.category) = LOWER(CAST(:category AS VARCHAR)))
            """, nativeQuery = true)
    ExpenseSummaryProjection calculateSummary(@Param("category") String category);
}
