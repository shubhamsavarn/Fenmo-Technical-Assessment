package com.expensetracker.web.controller;

import com.expensetracker.application.ExpenseService;
import com.expensetracker.application.dto.*;
import com.expensetracker.web.dto.*;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for expense operations.
 *
 * <p>Responsibilities (and nothing more):
 * <ul>
 *   <li>Parse HTTP request → application command/criteria</li>
 *   <li>Call service layer</li>
 *   <li>Map application result → HTTP response</li>
 *   <li>Set correct HTTP status codes</li>
 * </ul>
 *
 * <p>No business logic lives here. No SQL lives here. This is a thin HTTP adapter.</p>
 */
@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private static final Logger log = LoggerFactory.getLogger(ExpenseController.class);

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    /**
     * POST /api/expenses
     *
     * Creates a new expense. Requires Idempotency-Key header.
     * Returns 201 for new records, 200 for idempotent replays.
     */
    @PostMapping
    public ResponseEntity<?> createExpense(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKeyHeader,
            @Valid @RequestBody CreateExpenseRequest request) {

        // Validate Idempotency-Key header
        if (idempotencyKeyHeader == null || idempotencyKeyHeader.isBlank()) {
            ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
            problem.setTitle("Missing Idempotency Key");
            problem.setDetail("The 'Idempotency-Key' header is required for POST requests. "
                    + "Generate a UUID v4 and include it as a header.");
            return ResponseEntity.unprocessableEntity().body(problem);
        }

        UUID idempotencyKey;
        try {
            idempotencyKey = UUID.fromString(idempotencyKeyHeader.trim());
        } catch (IllegalArgumentException e) {
            ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
            problem.setTitle("Invalid Idempotency Key");
            problem.setDetail("The 'Idempotency-Key' header must be a valid UUID.");
            return ResponseEntity.unprocessableEntity().body(problem);
        }

        // Build command
        CreateExpenseCommand command = new CreateExpenseCommand(
                idempotencyKey,
                request.amount(),
                request.category(),
                request.description() != null ? request.description() : "",
                request.date().toString()
        );

        // Execute
        ExpenseResult result = expenseService.createExpense(command);

        // Map to response
        ExpenseResponse response = toResponse(result);

        // 201 for new, 200 for idempotent replay
        HttpStatus status = result.isNew() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }

    /**
     * GET /api/expenses
     *
     * Lists expenses with optional filtering, sorting, and pagination.
     * Summary reflects ALL matching records, not just the current page.
     */
    @GetMapping
    public ResponseEntity<ExpensePageResponse> getExpenses(
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "date_desc") String sort,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {

        ExpenseFilterCriteria criteria = new ExpenseFilterCriteria(category, sort, page, size);
        ExpensePageResult result = expenseService.getExpenses(criteria);

        // Map to API response
        List<ExpenseResponse> content = result.expenses().stream()
                .map(this::toResponse)
                .toList();

        ExpensePageResponse response = new ExpensePageResponse(
                content,
                new ExpensePageResponse.PageMeta(
                        result.pageInfo().number(),
                        result.pageInfo().size(),
                        result.pageInfo().totalElements(),
                        result.pageInfo().totalPages()
                ),
                new ExpensePageResponse.SummaryMeta(
                        result.summary().totalAmount(),
                        result.summary().count(),
                        result.summary().currency()
                )
        );

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/expenses/categories
     *
     * Returns all distinct categories for the filter dropdown.
     */
    @GetMapping("/categories")
    public ResponseEntity<Map<String, List<String>>> getCategories() {
        List<String> categories = expenseService.getCategories();
        return ResponseEntity.ok(Map.of("categories", categories));
    }

    // ── Private mapping ─────────────────────────────────

    private ExpenseResponse toResponse(ExpenseResult result) {
        return new ExpenseResponse(
                result.id(),
                result.amount(),
                result.category(),
                result.description(),
                result.expenseDate(),
                result.createdAt()
        );
    }
}
