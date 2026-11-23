package com.carepath.extraction;

import java.util.regex.Pattern;

/**
 * Single place that decides what counts as a usable OCR confidence.
 * A confidence is a finite number from 0 to 100. Anything else (negative, above 100, NaN, Infinity, hex floats,
 * exponent notation, trailing text) means UNKNOWN and is represented as NaN, never clamped or guessed.
 */
final class Confidence {

    private static final Pattern PLAIN_DECIMAL = Pattern.compile("^\\d{1,3}(?:\\.\\d{1,9})?$");

    private Confidence() { }

    /** The value if it is finite and within 0-100, otherwise NaN (unknown). */
    static double sanitize(double c) {
        return Double.isFinite(c) && c >= 0.0 && c <= 100.0 ? c : Double.NaN;
    }

    /**
     * Parses a Tesseract TSV confidence field. Only plain decimals are accepted, so Java-specific forms such as
     * "Infinity", "NaN", "1e2", "0x1p4" or "95d" are unknown instead of being parsed.
     */
    static double parse(String field) {
        if (field == null) return Double.NaN;
        String t = field.trim();
        if (!PLAIN_DECIMAL.matcher(t).matches()) return Double.NaN;
        return sanitize(Double.parseDouble(t));
    }
}
