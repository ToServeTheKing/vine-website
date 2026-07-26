package com.itsthevine.web.domain;

/**
 * Trimming and length rules for editor-supplied prose.
 *
 * <p>Every one of these limits is a column width, so the choice is between checking them here and
 * letting Postgres reject the insert — which reaches the editor as an unexplained 500 with their
 * afternoon's work still unsaved. The message names the offending text, because a catering table is
 * a grid of thirty small cells and "too long" alone doesn't say which one.
 */
final class Text {

    private Text() {
    }

    static String required(String value, int max, String missing) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(missing);
        }
        return capped(trimmed, max);
    }

    /** Trimmed, possibly empty — a blank cell is a real thing to want. */
    static String optional(String value, int max) {
        return capped(value == null ? "" : value.trim(), max);
    }

    private static String capped(String text, int max) {
        if (text.length() > max) {
            String preview = text.substring(0, Math.min(40, text.length()));
            throw new IllegalArgumentException(
                    "\"" + preview + "…\" is longer than the " + max + " characters that fit there.");
        }
        return text;
    }
}
