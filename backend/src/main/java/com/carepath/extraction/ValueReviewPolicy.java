package com.carepath.extraction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Combines the re-read result and the plausibility verdict for the review flow.
 *
 * Every reading stays UNVERIFIED until the user confirms it. This policy never verifies anything: agreement
 * between Tesseract passes is not independent evidence. {@code flagged} only says that the readers disagreed or
 * the value looks implausible, so the UI can show it prominently.
 *
 * The two candidate lists have different origins and must be labelled differently in the UI:
 *  - readFromImage: text actually read from the page image by a reader (first pass and re-reads), as read.
 *  - computedAlternatives: same digits with a different decimal position, computed from a reference range.
 *    These were NOT read from the image.
 */
public final class ValueReviewPolicy {

    public record Decision(boolean flagged, List<String> reasons,
                           List<String> readFromImage, List<String> computedAlternatives) { }

    public Decision decide(ValueReReader.Result reread, DecimalPlausibilityChecker.Verdict verdict) {
        List<String> reasons = new ArrayList<>();
        switch (reread.status()) {
            case READERS_AGREE -> { }
            case DIFFERS_FROM_FIRST_PASS -> reasons.add("Re-read of the cell disagrees with the first reading.");
            case REREAD_PASSES_DISAGREE -> reasons.add("The re-reads of the cell disagree with each other.");
            case MALFORMED_READING -> reasons.add(
                    "A reading was not one clean number (merged, repeated or non-numeric characters).");
            case CONFIDENCE_UNKNOWN -> reasons.add("OCR confidence was not available, so agreement cannot be trusted.");
            case LOW_CONFIDENCE -> reasons.add("The readers agree but reported low confidence.");
            case UNREADABLE -> reasons.add("No number could be read from the cell image.");
            case NOT_ATTEMPTED -> reasons.add("Not re-read because a processing limit was reached. Review manually.");
            case MULTIPLE_VALUES -> reasons.add("This row has more than one value (for example one per date column). "
                    + "Check each value against its printed date on the original.");
        }
        if (verdict.needsReview()) reasons.add(verdict.note());

        List<String> read = new ArrayList<>();
        for (var p : reread.passes()) {
            String t = p.rawText() == null ? "" : p.rawText().trim();
            if (!t.isEmpty() && !read.contains(t)) read.add(t);
        }
        List<String> computed = new ArrayList<>();
        if (verdict.needsReview()) {
            for (BigDecimal alt : verdict.computedAlternatives()) {
                String s = alt.stripTrailingZeros().toPlainString();
                if (!computed.contains(s) && read.stream().noneMatch(r -> sameNumber(r, alt))) computed.add(s);
            }
        }
        return new Decision(!reasons.isEmpty(), List.copyOf(reasons), List.copyOf(read), List.copyOf(computed));
    }

    private static boolean sameNumber(String readText, BigDecimal alt) {
        try {
            return new BigDecimal(readText.replaceAll("^[<>]", "")).compareTo(alt) == 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
