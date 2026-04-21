-- ============================================================
-- V1: Create expenses table
-- 
-- Key design decisions:
--   1. amount_paise (BIGINT) is the single source of truth
--   2. amount (NUMERIC) is a GENERATED column — derived, never manually set
--   3. idempotency_key has a UNIQUE constraint for duplicate prevention
--   4. Functional indexes on LOWER(category) for case-insensitive queries
--   5. CHECK constraints as defense-in-depth (app validates too)
-- ============================================================

CREATE TABLE expenses (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key   UUID        NOT NULL,
    amount_paise      BIGINT      NOT NULL,
    amount            NUMERIC(12,2) GENERATED ALWAYS AS (amount_paise / 100.0) STORED,
    category          VARCHAR(50) NOT NULL,
    description       VARCHAR(500) NOT NULL DEFAULT '',
    expense_date      DATE        NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Idempotency: prevent duplicate expense creation from retries
    CONSTRAINT uq_expenses_idempotency_key UNIQUE (idempotency_key),

    -- Data integrity: defense-in-depth (application validates first)
    CONSTRAINT chk_expenses_amount_positive CHECK (amount_paise > 0),
    CONSTRAINT chk_expenses_category_not_blank CHECK (TRIM(category) <> '')
);

-- Index for category filtering (case-insensitive)
CREATE INDEX idx_expenses_category ON expenses USING btree (LOWER(category));

-- Index for default sort (newest first, with created_at tiebreaker)
CREATE INDEX idx_expenses_date_desc ON expenses USING btree (expense_date DESC, created_at DESC);

-- Composite index for filter + sort combined query
CREATE INDEX idx_expenses_category_date ON expenses USING btree (LOWER(category), expense_date DESC);
