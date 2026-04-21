package com.expensetracker.infrastructure.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * JPA entity mapped to the 'expenses' table.
 *
 * <p>Note: The 'amount' column is GENERATED ALWAYS AS in PostgreSQL,
 * so we mark it insertable=false, updatable=false. Hibernate reads it
 * but never writes it.</p>
 */
@Entity
@Table(name = "expenses")
public class ExpenseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, unique = true, updatable = false)
    private UUID idempotencyKey;

    @Column(name = "amount_paise", nullable = false)
    private Long amountPaise;

    /**
     * Generated column in PostgreSQL (amount_paise / 100.0).
     * Read-only in JPA — never inserted or updated by Hibernate.
     */
    @Column(name = "amount", insertable = false, updatable = false)
    private BigDecimal amount;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // === JPA lifecycle callbacks ===

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // === Constructors ===

    protected ExpenseEntity() {
        // JPA requires no-arg constructor
    }

    public ExpenseEntity(UUID idempotencyKey, Long amountPaise, String category,
                         String description, LocalDate expenseDate) {
        this.idempotencyKey = idempotencyKey;
        this.amountPaise = amountPaise;
        this.category = category;
        this.description = description;
        this.expenseDate = expenseDate;
    }

    // === Getters (no setters — prefer immutability after creation) ===

    public UUID getId() { return id; }
    public UUID getIdempotencyKey() { return idempotencyKey; }
    public Long getAmountPaise() { return amountPaise; }
    public BigDecimal getAmount() { return amount; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public LocalDate getExpenseDate() { return expenseDate; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
