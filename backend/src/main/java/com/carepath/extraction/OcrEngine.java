package com.carepath.extraction;

import java.awt.image.BufferedImage;
import java.time.Duration;

/**
 * Minimal OCR abstraction so the re-read logic does not depend on how Tesseract is invoked
 * (CLI, Tess4J, or a test fake). Implement it with whatever your backend already uses.
 */
public interface OcrEngine {

    /**
     * @param text recognised text ("" if nothing); @param confidence 0-100, or NaN if unknown.
     * Any confidence that is not a finite number from 0 to 100 is stored as NaN (unknown).
     */
    record Result(String text, double confidence) {
        public static final Result EMPTY = new Result("", 0);

        public Result {
            text = text == null ? "" : text;
            confidence = Confidence.sanitize(confidence);
        }
    }

    /** Recognise ONE line that should contain a numeric value (digits, '.', '<', '>', '-'). */
    Result recognizeNumericLine(BufferedImage image);

    /**
     * Same, but the call must not run longer than {@code maxTime}; on expiry it must stop any work it started and
     * return {@link Result#EMPTY}. The default ignores the limit, so engines that can be stopped should override it.
     */
    default Result recognizeNumericLine(BufferedImage image, Duration maxTime) {
        return recognizeNumericLine(image);
    }
}
