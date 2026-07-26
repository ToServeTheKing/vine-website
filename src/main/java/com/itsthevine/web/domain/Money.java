package com.itsthevine.web.domain;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * What a price is, in one place: how the bakery types one in, how it's stored, and how it's printed.
 *
 * <p>Editors type "24", "$24", "24.50" or nothing at all, so the parsing lives here rather than in the
 * browser — a price the server didn't agree to is not a price, and {@code Number(text) * 100} gives
 * 2410.0000000000005 for "24.10" on the way past. Stored as whole cents, because money is not a
 * floating-point number.
 */
public final class Money {

    /** Ten thousand dollars. Above this it's a decimal point in the wrong place, not a wedding. */
    private static final int MOST_ANYTHING_COSTS = 1_000_000;

    private Money() {
    }

    /**
     * @param typed what the editor put in the box
     * @return whole cents, or null for a column that doesn't state a price
     */
    public static Integer cents(String typed) {
        String cleaned = typed == null ? "" : typed.replace("$", "").replace(",", "").replace(" ", "").trim();
        if (cleaned.isEmpty()) {
            // Not an error: "ask us" is a legitimate thing for a column to say, and it says it by
            // leaving the price empty.
            return null;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("\"" + typed
                    + "\" isn't a price. Leave it empty if that column doesn't have one.");
        }
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("Prices go to the cent — \"" + typed + "\" is finer than that.");
        }
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("A price can't be less than nothing.");
        }

        long asCents = amount.movePointRight(2).longValueExact();
        if (asCents > MOST_ANYTHING_COSTS) {
            throw new IllegalArgumentException("That price is over $10,000 — check the decimal point.");
        }
        return (int) asCents;
    }

    /**
     * "$24", "$1,250", "$24.50" — never "$24.0", and null stays null.
     *
     * <p>{@code Locale.US} rather than the default: the price of a cake in Princeville, Illinois does
     * not depend on which locale the container started in, and a default of de-DE would print
     * "$1.250" for one thousand two hundred and fifty dollars.
     */
    public static String format(Integer cents) {
        if (cents == null) {
            return null;
        }
        int dollars = cents / 100;
        int change = cents % 100;
        return change == 0
                ? String.format(Locale.US, "$%,d", dollars)
                : String.format(Locale.US, "$%,d.%02d", dollars, change);
    }
}
