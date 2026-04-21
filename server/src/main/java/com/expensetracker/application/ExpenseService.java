package com.expensetracker.application;

import com.expensetracker.application.dto.*;
import com.expensetracker.application.exception.IdempotencyConflictException;
import com.expensetracker.domain.Money;
import com.expensetracker.infrastructure.persistence.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Core business logic for expense management.
 *
 * <h3>Idempotency Algorithm:</h3>
 * <ol>
 *   <li>Check if idempotency key already exists in DB</li>
 *   <li>If yes → verify payload matches (amount + category + date) → return cached result</li>
 *   <li>If no → validate, construct Money, insert → return new result</li>
 *   <li>If concurrent race (DataIntegrityViolation on UNIQUE) → re-fetch and return cached</li>
 * </ol>
 *
 * <h3>Transaction Isolation:</h3>
 * <p>Uses READ_COMMITTED (default). The UNIQUE constraint on idempotency_key is the ultimate
 * guard against duplicates, even if two requests race past the application-level check.</p>
 */
@Service
public class ExpenseService {

    private static final Logger log = LoggerFactory.getLogger(ExpenseService.class);

    /** Map from API sort parameter to Spring Data Sort. */
    private static final Map<String, Sort> SORT_STRATEGIES = Map.of(
            "date_desc", Sort.by(Sort.Order.desc("expenseDate"), Sort.Order.desc("createdAt")),
            "date_asc", Sort.by(Sort.Order.asc("expenseDate"), Sort.Order.asc("createdAt")),
            "amount_desc", Sort.by(Sort.Order.desc("amountPaise"), Sort.Order.desc("createdAt")),
            "amount_asc", Sort.by(Sort.Order.asc("amountPaise"), Sort.Order.asc("createdAt"))
    );

    private final ExpenseJpaRepository repository;
    private final ExpenseMapper mapper;

    public ExpenseService(ExpenseJpaRepository repository, ExpenseMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /**
     * Creates an expense with full idempotency guarantee.
     *
     * @param command validated command from the controller
     * @return result with isNew flag indicating 201 vs 200
     * @throws IdempotencyConflictException if same key used with different payload
     * @throws IllegalArgumentException     if amount is invalid
     */
    @Transactional
    public ExpenseResult createExpense(CreateExpenseCommand command) {
        UUID idempotencyKey = command.idempotencyKey();

        // Step 1: Check for existing record with this idempotency key
        Optional<ExpenseEntity> existing = repository.findByIdempotencyKey(idempotencyKey);

        if (existing.isPresent()) {
            ExpenseEntity entity = existing.get();
            log.info("Idempotent replay detected for key={}", idempotencyKey);

            // Step 2: Verify payload hasn't changed (prevent key reuse abuse)
            validatePayloadMatch(entity, command);
            return toResult(entity, false);
        }

        // Step 3: Parse and validate the amount using Money value object
        Money amount = Money.fromString(command.amount());
        if (amount.toPaise() == 0) {
            throw new IllegalArgumentException("Expense amount must be positive");
        }
        LocalDate expenseDate = LocalDate.parse(command.date());

        // Step 4: Persist
        ExpenseEntity entity = mapper.toEntity(
                idempotencyKey, amount,
                command.category(), command.description(),
                expenseDate
        );

        try {
            ExpenseEntity saved = repository.saveAndFlush(entity);
            log.info("Expense created: id={}, amount={}, category={}",
                    saved.getId(), amount.toDisplayString(), saved.getCategory());
            return toResult(saved, true);

        } catch (DataIntegrityViolationException e) {
            // Race condition: concurrent request with same key won the insert.
            // The UNIQUE constraint caught it. Re-fetch and return the existing record.
            log.warn("Concurrent idempotency race for key={}, returning existing record",
                    idempotencyKey);
            return repository.findByIdempotencyKey(idempotencyKey)
                    .map(existing2 -> toResult(existing2, false))
                    .orElseThrow(() -> new IllegalStateException(
                            "Idempotency conflict but record not found — data integrity issue"));
        }
    }

    /**
     * Lists expenses with filtering, sorting, and pagination.
     * Also computes aggregate summary for ALL matching records (not just current page).
     */
    @Transactional(readOnly = true)
    public ExpensePageResult getExpenses(ExpenseFilterCriteria criteria) {
        // Build dynamic filter specification
        Specification<ExpenseEntity> spec = Specification.where(
                ExpenseSpecifications.withCategory(criteria.category())
        );

        // Build sort + pagination
        Sort sort = SORT_STRATEGIES.getOrDefault(criteria.sort(), SORT_STRATEGIES.get("date_desc"));
        Pageable pageable = PageRequest.of(criteria.page(), criteria.size(), sort);

        // Query 1: Paginated list
        Page<ExpenseEntity> page = repository.findAll(spec, pageable);

        // Query 2: Aggregate summary for ALL matching records (ignoring pagination)
        ExpenseSummaryProjection summary = repository.calculateSummary(criteria.category());

        // Map results
        List<ExpenseResult> expenses = page.getContent().stream()
                .map(entity -> toResult(entity, false))
                .toList();

        long totalPaise = summary != null && summary.getTotalPaise() != null ? summary.getTotalPaise() : 0L;
        long count = summary != null && summary.getCount() != null ? summary.getCount() : 0L;
        String totalDisplay = Money.fromPaise(totalPaise).toDisplayString();

        return new ExpensePageResult(
                expenses,
                ExpensePageResult.PageInfo.from(page),
                new ExpensePageResult.SummaryInfo(totalDisplay, totalPaise, count, "INR")
        );
    }

    /**
     * Returns all distinct categories for populating the filter dropdown.
     */
    @Transactional(readOnly = true)
    public List<String> getCategories() {
        return repository.findDistinctCategories();
    }

    // ── Private helpers ─────────────────────────────────────

    /**
     * Validates that a replay request has the same payload as the original.
     * Prevents abuse where a client reuses an idempotency key with different data.
     */
    private void validatePayloadMatch(ExpenseEntity entity, CreateExpenseCommand command) {
        Money commandAmount = Money.fromString(command.amount());
        boolean matches = entity.getAmountPaise().equals(commandAmount.toPaise())
                && entity.getCategory().equalsIgnoreCase(command.category().trim())
                && entity.getExpenseDate().equals(LocalDate.parse(command.date()));

        if (!matches) {
            throw new IdempotencyConflictException(
                    "Idempotency key '" + command.idempotencyKey()
                    + "' was already used with a different request payload");
        }
    }

    /**
     * Maps an entity to an ExpenseResult.
     */
    private ExpenseResult toResult(ExpenseEntity entity, boolean isNew) {
        // Use the generated 'amount' column or derive from paise
        String displayAmount = entity.getAmount() != null
                ? entity.getAmount().toPlainString()
                : Money.fromPaise(entity.getAmountPaise()).toDisplayString();

        return new ExpenseResult(
                entity.getId(),
                displayAmount,
                entity.getAmountPaise(),
                entity.getCategory(),
                entity.getDescription(),
                entity.getExpenseDate(),
                entity.getCreatedAt(),
                isNew
        );
    }
}
