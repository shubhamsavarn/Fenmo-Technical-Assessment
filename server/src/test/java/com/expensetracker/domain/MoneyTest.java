package com.expensetracker.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Money value object.
 * No Spring context needed — pure domain logic.
 */
class MoneyTest {

    @Nested
    @DisplayName("fromString — valid inputs")
    class FromStringValid {

        @Test
        @DisplayName("whole number: '100' → 10000 paise")
        void wholeNumber() {
            Money money = Money.fromString("100");
            assertEquals(10000L, money.toPaise());
            assertEquals("100.00", money.toDisplayString());
        }

        @Test
        @DisplayName("with two decimals: '1250.50' → 125050 paise")
        void twoDecimals() {
            Money money = Money.fromString("1250.50");
            assertEquals(125050L, money.toPaise());
        }

        @Test
        @DisplayName("with one decimal: '99.5' → 9950 paise")
        void oneDecimal() {
            Money money = Money.fromString("99.5");
            assertEquals(9950L, money.toPaise());
        }

        @Test
        @DisplayName("smallest amount: '0.01' → 1 paisa")
        void smallestAmount() {
            Money money = Money.fromString("0.01");
            assertEquals(1L, money.toPaise());
        }

        @Test
        @DisplayName("large amount: '9999999.99' → 999999999 paise")
        void largeAmount() {
            Money money = Money.fromString("9999999.99");
            assertEquals(999999999L, money.toPaise());
        }

        @Test
        @DisplayName("zero: '0' → 0 paise")
        void zeroAmount() {
            Money money = Money.fromString("0");
            assertEquals(0L, money.toPaise());
            assertEquals("0.00", money.toDisplayString());
        }

        @Test
        @DisplayName("with leading/trailing whitespace: ' 100.50 ' → 10050 paise")
        void withWhitespace() {
            Money money = Money.fromString("  100.50  ");
            assertEquals(10050L, money.toPaise());
        }
    }

    @Nested
    @DisplayName("fromString — invalid inputs")
    class FromStringInvalid {

        @Test
        @DisplayName("rejects negative amount")
        void negative() {
            assertThrows(IllegalArgumentException.class, () -> Money.fromString("-100"));
        }

        @Test
        @DisplayName("rejects 3+ decimal places")
        void threeDecimals() {
            assertThrows(IllegalArgumentException.class, () -> Money.fromString("10.123"));
        }

        @Test
        @DisplayName("rejects non-numeric")
        void nonNumeric() {
            assertThrows(IllegalArgumentException.class, () -> Money.fromString("abc"));
        }

        @Test
        @DisplayName("rejects null")
        void nullInput() {
            assertThrows(IllegalArgumentException.class, () -> Money.fromString(null));
        }

        @Test
        @DisplayName("rejects blank")
        void blank() {
            assertThrows(IllegalArgumentException.class, () -> Money.fromString("   "));
        }
    }

    @Nested
    @DisplayName("fromPaise")
    class FromPaise {

        @Test
        @DisplayName("valid paise → Money")
        void validPaise() {
            Money money = Money.fromPaise(10050);
            assertEquals(10050L, money.toPaise());
            assertEquals("100.50", money.toDisplayString());
        }

        @Test
        @DisplayName("zero paise → Money")
        void zeroPaise() {
            Money money = Money.fromPaise(0);
            assertEquals(0L, money.toPaise());
            assertEquals("0.00", money.toDisplayString());
        }

        @Test
        @DisplayName("rejects negative paise")
        void negativePaise() {
            assertThrows(IllegalArgumentException.class, () -> Money.fromPaise(-1));
        }
    }

    @Nested
    @DisplayName("toBigDecimal")
    class ToBigDecimal {

        @Test
        @DisplayName("10050 paise → BigDecimal 100.50")
        void exactRepresentation() {
            Money money = Money.fromPaise(10050);
            assertEquals(new BigDecimal("100.50"), money.toBigDecimal());
        }

        @Test
        @DisplayName("1 paisa → BigDecimal 0.01")
        void singlePaisa() {
            Money money = Money.fromPaise(1);
            assertEquals(new BigDecimal("0.01"), money.toBigDecimal());
        }
    }

    @Nested
    @DisplayName("add")
    class Add {

        @Test
        @DisplayName("100.50 + 200.25 = 300.75")
        void addition() {
            Money a = Money.fromString("100.50");
            Money b = Money.fromString("200.25");
            Money sum = a.add(b);
            assertEquals(30075L, sum.toPaise());
            assertEquals("300.75", sum.toDisplayString());
        }
    }

    @Nested
    @DisplayName("equality")
    class Equality {

        @Test
        @DisplayName("same paise value → equal")
        void sameValue() {
            Money a = Money.fromString("100.50");
            Money b = Money.fromPaise(10050);
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("different paise value → not equal")
        void differentValue() {
            Money a = Money.fromString("100.50");
            Money b = Money.fromString("100.51");
            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("Money('1.0') == Money('1.00') — unlike raw BigDecimal!")
        void scaleIndependentEquality() {
            // This is why we use Money instead of raw BigDecimal:
            // new BigDecimal("1.0").equals(new BigDecimal("1.00")) is FALSE in Java
            Money a = Money.fromString("1.0");
            Money b = Money.fromString("1.00");
            assertEquals(a, b);
        }
    }
}
