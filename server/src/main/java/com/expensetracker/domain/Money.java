package com.expensetracker.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Immutable Value Object representing a monetary amount in Indian Rupees (INR).
 *
 * <h3>Design Decisions:</h3>
 * <ul>
 *   <li>Internally stores amount as paise (long) for exact integer arithmetic</li>
 *   <li>Accepts String input to avoid float→BigDecimal precision loss</li>
 *   <li>Enforces positive-only (expenses can't be negative or zero)</li>
 *   <li>Maximum 2 decimal places (no fractional paise)</li>
 *   <li>Maximum amount: ₹99,99,99,999.99 (fits in long with massive headroom)</li>
 *   <li>Immutable — thread-safe, cache-safe, no defensive copies needed</li>
 * </ul>
 *
 * <h3>Why not raw BigDecimal?</h3>
 * <p>{@code new BigDecimal("1.0").equals(new BigDecimal("1.00"))} returns {@code false} in Java.
 * Money compares by paise, which is always unambiguous. Also, raw BigDecimal has no constraints —
 * {@code new BigDecimal("-0.001")} is valid. Money makes invalid states unrepresentable.</p>
 */
public final class Money {

    private static final long MAX_PAISE = 9_999_999_999_99L; // ₹99,99,99,999.99

    private final long paise;

    private Money(long paise) {
        if (paise < 0) {
            throw new IllegalArgumentException("Amount must not be negative, got: " + paise + " paise");
        }
        if (paise > MAX_PAISE) {
            throw new IllegalArgumentException(
                    "Amount exceeds maximum (₹99,99,99,999.99), got: " + paise + " paise");
        }
        this.paise = paise;
    }

    /**
     * Creates Money from a decimal string like "1250.50".
     * This is the primary factory method — used when parsing API input.
     *
     * @param amount Decimal string (e.g., "100", "100.5", "100.50")
     * @throws IllegalArgumentException if null, empty, non-numeric, negative, zero,
     *                                  more than 2 decimal places, or exceeds max
     * @throws NumberFormatException    if not a valid decimal string
     */
    public static Money fromString(String amount) {
        if (amount == null || amount.isBlank()) {
            throw new IllegalArgumentException("Amount must not be null or blank");
        }

        BigDecimal bd;
        try {
            bd = new BigDecimal(amount.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Amount is not a valid number: " + amount);
        }

        if (bd.scale() > 2) {
            throw new IllegalArgumentException(
                    "Amount must have at most 2 decimal places, got: " + amount);
        }
        if (bd.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must not be negative, got: " + amount);
        }

        // Convert to paise — movePointRight(2) then exact long conversion
        // longValueExact() throws ArithmeticException if fractional part remains (impossible here)
        long paise = bd.movePointRight(2).longValueExact();
        return new Money(paise);
    }

    /**
     * Creates Money from paise (e.g., from database column).
     *
     * @param paise Amount in paise (1 INR = 100 paise)
     */
    public static Money fromPaise(long paise) {
        return new Money(paise);
    }

    /** Returns the amount in paise (canonical representation). */
    public long toPaise() {
        return paise;
    }

    /** Returns the amount as BigDecimal with scale 2 (e.g., 100.50). */
    public BigDecimal toBigDecimal() {
        return BigDecimal.valueOf(paise, 2);
    }

    /** Returns the amount as a plain string (e.g., "1250.50"), never scientific notation. */
    public String toDisplayString() {
        return toBigDecimal().toPlainString();
    }

    /** Adds two Money amounts. Overflow-safe via Math.addExact. */
    public Money add(Money other) {
        return new Money(Math.addExact(this.paise, other.paise));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money money)) return false;
        return paise == money.paise;
    }

    @Override
    public int hashCode() {
        return Objects.hash(paise);
    }

    @Override
    public String toString() {
        return "Money{₹" + toDisplayString() + "}";
    }
}
