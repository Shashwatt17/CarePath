package com.carepath.extraction;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Flags values that look like a misplaced or dropped decimal point, using the printed reference range.
 *
 * Design rules:
 *  - It NEVER corrects a value. It only reports alternatives for the user to choose from.
 *  - A value outside the reference range is NOT an error: abnormal results are real. A value is only
 *    suspected when it sits far outside a "plausible window" around the range AND a decimal-shifted
 *    reading of the same digits falls inside that window.
 *  - Without a reference range, nothing can be concluded, so the value is passed through as such.
 */
public final class DecimalPlausibilityChecker {

    public enum Status {
        /** Inside the printed reference range. */
        CONSISTENT,
        /** Outside the range but close enough to be a genuine abnormal result. */
        OUTSIDE_RANGE_PLAUSIBLE,
        /** Far outside the window and a decimal shift of the same digits fits. Show alternatives. */
        SUSPECT_DECIMAL,
        /** Far outside the window and no decimal shift explains it. Confirm against the source. */
        FAR_OUTSIDE_RANGE,
        NO_REFERENCE_RANGE,
        /** Not a plain number (e.g. contains a comma, letters, or several numbers). */
        UNPARSEABLE
    }

    public record Verdict(Status status, BigDecimal value, List<BigDecimal> computedAlternatives, String note) {
        public boolean needsReview() {
            return status == Status.SUSPECT_DECIMAL
                    || status == Status.FAR_OUTSIDE_RANGE
                    || status == Status.UNPARSEABLE;
        }
    }

    private static final Pattern PLAIN =
            Pattern.compile("^\\s*[<>]?=?\\s*(-?)(\\d+)(?:\\.(\\d+))?\\s*$");

    /** @param refLow lower bound or null (e.g. "&lt;200"); @param refHigh upper bound or null (e.g. "&gt;40"). */
    public Verdict check(String valueText, BigDecimal refLow, BigDecimal refHigh) {
        return check(valueText, refLow, refHigh, null, null);
    }

    /**
     * @param hardMin optional physiological minimum for this test and unit (null if unknown)
     * @param hardMax optional physiological maximum for this test and unit (null if unknown)
     */
    public Verdict check(String valueText, BigDecimal refLow, BigDecimal refHigh,
                         BigDecimal hardMin, BigDecimal hardMax) {
        if (valueText == null) {
            return new Verdict(Status.UNPARSEABLE, null, List.of(), "No value text.");
        }
        Matcher m = PLAIN.matcher(valueText);
        if (!m.matches()) {
            return new Verdict(Status.UNPARSEABLE, null, List.of(),
                    "Not a single plain number (check separators or merged values): " + valueText);
        }
        String sign = m.group(1);
        String intPart = m.group(2);
        String fracPart = m.group(3) == null ? "" : m.group(3);
        BigDecimal value = new BigDecimal(sign + intPart + (fracPart.isEmpty() ? "" : "." + fracPart));

        Window window = windowFor(refLow, refHigh);
        if (window == null) {
            return new Verdict(Status.NO_REFERENCE_RANGE, value, List.of(),
                    "No usable reference range, so plausibility cannot be judged.");
        }

        if (inRange(value, refLow, refHigh)) {
            return new Verdict(Status.CONSISTENT, value, List.of(), "Inside the printed reference range.");
        }
        boolean hardOk = withinHard(value, hardMin, hardMax);
        if (hardOk && window.contains(value)) {
            return new Verdict(Status.OUTSIDE_RANGE_PLAUSIBLE, value, List.of(),
                    "Outside the reference range but within a plausible distance of it.");
        }

        List<BigDecimal> alternatives = new ArrayList<>();
        for (BigDecimal c : decimalPlacements(sign, intPart + fracPart)) {
            if (c.compareTo(value) != 0 && window.contains(c) && withinHard(c, hardMin, hardMax)) {
                alternatives.add(c);
            }
        }
        BigDecimal target = window.midpoint();
        alternatives.sort(Comparator.comparing((BigDecimal c) -> c.subtract(target).abs()));

        if (!alternatives.isEmpty()) {
            return new Verdict(Status.SUSPECT_DECIMAL, value, List.copyOf(alternatives),
                    "Far outside the reference range; the same digits with a different decimal "
                            + "position fit it better. Verify against the source.");
        }
        return new Verdict(Status.FAR_OUTSIDE_RANGE, value, List.of(),
                "Far outside the reference range and no decimal shift explains it. Verify against the source.");
    }

    /** Every placement of a decimal point in the digit string, including none (integer). */
    static List<BigDecimal> decimalPlacements(String sign, String digits) {
        List<BigDecimal> out = new ArrayList<>();
        out.add(new BigDecimal(sign + digits));
        for (int k = 1; k < digits.length(); k++) {
            out.add(new BigDecimal(sign + digits.substring(0, k) + "." + digits.substring(k)));
        }
        return out;
    }

    private static boolean inRange(BigDecimal v, BigDecimal low, BigDecimal high) {
        return (low == null || v.compareTo(low) >= 0) && (high == null || v.compareTo(high) <= 0);
    }

    private static boolean withinHard(BigDecimal v, BigDecimal min, BigDecimal max) {
        return (min == null || v.compareTo(min) >= 0) && (max == null || v.compareTo(max) <= 0);
    }

    private record Window(BigDecimal lo, BigDecimal hi) {
        boolean contains(BigDecimal v) { return v.compareTo(lo) >= 0 && v.compareTo(hi) <= 0; }
        BigDecimal midpoint() { return lo.add(hi).divide(BigDecimal.valueOf(2), MathContext.DECIMAL64); }
    }

    /**
     * Plausible window around the reference range:
     *  both bounds: range widened by one range-width on each side (not below 0 if the range is non-negative);
     *  only upper bound: 0 .. 1.5 x upper;  only lower bound: 0.5 x lower .. 5 x lower.
     */
    private static Window windowFor(BigDecimal low, BigDecimal high) {
        if (low != null && high != null) {
            if (high.compareTo(low) <= 0) return null;
            BigDecimal span = high.subtract(low);
            BigDecimal lo = low.subtract(span);
            if (low.signum() >= 0 && lo.signum() < 0) lo = BigDecimal.ZERO;
            return new Window(lo, high.add(span));
        }
        if (high != null && high.signum() > 0) {
            return new Window(BigDecimal.ZERO, high.multiply(new BigDecimal("1.5")));
        }
        if (low != null && low.signum() > 0) {
            return new Window(low.multiply(new BigDecimal("0.5")), low.multiply(BigDecimal.valueOf(5)));
        }
        return null;
    }
}
