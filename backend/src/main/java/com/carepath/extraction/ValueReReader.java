package com.carepath.extraction;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Second-opinion OCR for one value cell. Re-reads the cell crop twice (different scale and binarisation,
 * digits-only whitelist) and compares with the first pass.
 *
 * Strictness rules:
 *  - A reading counts as a number only if the WHOLE text is one clean number. "12 34", "12.3.4" or "O45" are
 *    malformed readings, never silently repaired.
 *  - Agreement needs a KNOWN confidence of at least {@link #MIN_CONFIDENCE} from all three readers,
 *    including the first pass. Missing confidence is not agreement.
 *  - Agreement is never verification: all readers are Tesseract and can share one error.
 *  - When readers disagree, nothing is picked; every reading is kept in {@link Result#passes()}.
 */
public final class ValueReReader {

    public enum Status {
        /** All three readers give the same clean number with known, adequate confidence. Still UNVERIFIED. */
        READERS_AGREE,
        /** Both re-reads agree with each other but not with the first pass (e.g. 262 vs 26.2). */
        DIFFERS_FROM_FIRST_PASS,
        /** The two re-reads disagree, or only one of them produced a number. */
        REREAD_PASSES_DISAGREE,
        /** A reader produced text that is not one clean number (merged digits, repeated dots, letters in digits). */
        MALFORMED_READING,
        /** Readers agree but at least one confidence is unavailable, so agreement cannot be trusted. */
        CONFIDENCE_UNKNOWN,
        /** Readers agree but at least one reported low confidence. */
        LOW_CONFIDENCE,
        /** Nothing numeric could be read from the cell. */
        UNREADABLE,
        /** The cell was not re-read because a row or time limit was reached. */
        NOT_ATTEMPTED,
        /** The row holds more than one number (for example one per dated column). Each needs its own date check. */
        MULTIPLE_VALUES
    }

    /** {@code confidence} is 0-100 or NaN (unknown); any other value is stored as NaN. */
    public record Pass(String label, String rawText, String number, double confidence, boolean malformed) {
        public Pass {
            confidence = Confidence.sanitize(confidence);
        }
    }

    /**
     * @param agreedNumber   only set when status is READERS_AGREE
     * @param readCandidates distinct clean numbers actually READ from the image by any reader.
     *                       Decimal alternatives computed from digits are NOT included; see DecimalPlausibilityChecker.
     */
    public record Result(Status status, String agreedNumber, List<String> readCandidates, List<Pass> passes) {
        public boolean readersAgree() { return status == Status.READERS_AGREE; }
    }

    private static final Pattern STRICT_NUMBER = Pattern.compile("^[<>]?-?\\d+(?:\\.\\d+)?$");
    static final double MIN_CONFIDENCE = 70.0;

    private final OcrEngine engine;
    private final CellImagePreparer preparer;

    public ValueReReader(OcrEngine engine) {
        this(engine, new CellImagePreparer());
    }

    public ValueReReader(OcrEngine engine, CellImagePreparer preparer) {
        this.engine = engine;
        this.preparer = preparer;
    }

    /** First-pass confidence unknown, so agreement is impossible (result will be CONFIDENCE_UNKNOWN at best). */
    public Result reRead(BufferedImage page, Rectangle cell, String firstPassText) {
        return reRead(page, cell, firstPassText, Double.NaN);
    }

    /** No time limit other than the engine's own per-call timeout. */
    public static final long NO_DEADLINE = Long.MAX_VALUE;

    /**
     * @param firstPassText       the value text produced by the main OCR for this cell (null if none)
     * @param firstPassConfidence its OCR confidence 0-100, or NaN if unknown (other values are treated as unknown)
     */
    public Result reRead(BufferedImage page, Rectangle cell, String firstPassText, double firstPassConfidence) {
        return reRead(page, cell, firstPassText, firstPassConfidence, NO_DEADLINE);
    }

    /**
     * Same, but each OCR call is limited to the time left before {@code deadlineNanos} (a {@link System#nanoTime()}
     * value). If time runs out the result is NOT_ATTEMPTED, never "unreadable" or "disagree".
     */
    public Result reRead(BufferedImage page, Rectangle cell, String firstPassText, double firstPassConfidence,
                         long deadlineNanos) {
        Pass first = new Pass("first-pass", firstPassText, canonical(firstPassText), firstPassConfidence,
                isMalformed(firstPassText));
        if (remaining(deadlineNanos).isZero()) {
            return new Result(Status.NOT_ATTEMPTED, null, distinctNumbers(List.of(first)), List.of(first));
        }
        Pass a = runPass("reread-3x-gray", page, cell, 3.0, false, deadlineNanos);
        if (remaining(deadlineNanos).isZero()) return timedOut(first, a);
        Pass b = runPass("reread-4x-otsu", page, cell, 4.0, true, deadlineNanos);
        if (remaining(deadlineNanos).isZero() && (a.rawText().isBlank() || b.rawText().isBlank())) {
            return timedOut(first, a, b);               // an empty pass may be a timeout, so do not report "unreadable"
        }
        List<Pass> passes = List.of(first, a, b);
        List<String> candidates = distinctNumbers(passes);

        if (a.malformed() || b.malformed()) return new Result(Status.MALFORMED_READING, null, candidates, passes);
        if (a.number() == null && b.number() == null) return new Result(Status.UNREADABLE, null, candidates, passes);
        if (a.number() == null || b.number() == null || !a.number().equals(b.number())) {
            return new Result(Status.REREAD_PASSES_DISAGREE, null, candidates, passes);
        }
        if (first.number() == null || !first.number().equals(a.number())) {
            return new Result(Status.DIFFERS_FROM_FIRST_PASS, null, candidates, passes);
        }
        for (Pass p : passes) {
            if (Double.isNaN(p.confidence())) return new Result(Status.CONFIDENCE_UNKNOWN, null, candidates, passes);
        }
        for (Pass p : passes) {
            if (p.confidence() < MIN_CONFIDENCE) return new Result(Status.LOW_CONFIDENCE, null, candidates, passes);
        }
        return new Result(Status.READERS_AGREE, first.number(), candidates, passes);
    }

    /** Result for a row that was skipped because a limit was reached. Always flagged by the policy. */
    public static Result notAttempted(String firstPassText, double firstPassConfidence) {
        Pass first = new Pass("first-pass", firstPassText, canonical(firstPassText), firstPassConfidence,
                isMalformed(firstPassText));
        return new Result(Status.NOT_ATTEMPTED, null, distinctNumbers(List.of(first)), List.of(first));
    }

    private Pass runPass(String label, BufferedImage page, Rectangle cell, double scale, boolean binarize,
                         long deadlineNanos) {
        try {
            BufferedImage crop = preparer.prepare(page, cell, scale, binarize);
            Duration left = remaining(deadlineNanos);
            if (left.isZero()) return new Pass(label, "", null, Double.NaN, false);
            OcrEngine.Result r = engine.recognizeNumericLine(crop, left);
            return new Pass(label, r.text(), canonical(r.text()), r.confidence(), isMalformed(r.text()));
        } catch (RuntimeException e) {
            return new Pass(label, "", null, Double.NaN, false);
        }
    }

    /** Time left before the deadline, never negative. */
    private static Duration remaining(long deadlineNanos) {
        if (deadlineNanos == NO_DEADLINE) return Duration.ofDays(1);
        long left = deadlineNanos - System.nanoTime();
        return left <= 0 ? Duration.ZERO : Duration.ofNanos(left);
    }

    private static Result timedOut(Pass... passes) {
        List<Pass> list = List.of(passes);
        return new Result(Status.NOT_ATTEMPTED, null, distinctNumbers(list), list);
    }

    private static List<String> distinctNumbers(List<Pass> passes) {
        Set<String> distinct = new LinkedHashSet<>();
        for (Pass p : passes) if (p.number() != null) distinct.add(p.number());
        return new ArrayList<>(distinct).stream().toList();
    }

    /**
     * The text as ONE clean number, or null. Only trailing '.' / '-' noise is trimmed (e.g. "26.2." -> "26.2").
     * Internal spaces, several dots, letters or other characters make it null; nothing is merged or truncated.
     */
    static String canonical(String text) {
        if (text == null) return null;
        String t = text.trim().replaceAll("[.\\-]+$", "");
        return STRICT_NUMBER.matcher(t).matches() ? t : null;
    }

    /** Text that contains digits but is not one clean number, e.g. "12 34", "12.3.4", "O45". */
    static boolean isMalformed(String text) {
        return text != null && text.chars().anyMatch(Character::isDigit) && canonical(text) == null;
    }
}
