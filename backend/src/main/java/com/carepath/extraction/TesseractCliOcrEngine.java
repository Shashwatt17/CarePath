package com.carepath.extraction;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Runs the local {@code tesseract} binary in single-line mode (psm 7) restricted to numeric characters.
 * One short-lived process per cell keeps memory flat. Output size and run time are bounded by one deadline,
 * and the process is always killed and reaped when it is exceeded.
 */
public final class TesseractCliOcrEngine implements OcrEngine {

    private static final String WHITELIST = "0123456789.<>-";
    static final int MAX_OUTPUT_BYTES = 64 * 1024;
    static final int MAX_WORDS = 20;
    static final int MAX_WORD_CHARS = 32;

    private final Path directory;
    private final String executable;
    private final Duration timeout;

    public TesseractCliOcrEngine() {
        this("tesseract", Duration.ofSeconds(15));
    }

    public TesseractCliOcrEngine(String executable, Duration timeout) {
        this(executable,timeout,null);
    }
    public TesseractCliOcrEngine(String executable,Duration timeout,Path directory) {
        this.directory=directory;
        this.executable = executable;
        this.timeout = timeout;
    }

    @Override
    public Result recognizeNumericLine(BufferedImage image) {
        return recognizeNumericLine(image, timeout);
    }

    /** The effective limit is the smaller of this engine's own timeout and the caller's remaining time. */
    @Override
    public Result recognizeNumericLine(BufferedImage image, Duration maxTime) {
        Duration limit = maxTime.compareTo(timeout) < 0 ? maxTime : timeout;
        Path tmp = null;
        try {
            tmp = directory==null ? Files.createTempFile("cell-", ".png") : Files.createTempFile(directory,"cell-", ".png");
            ImageIO.write(image, "png", tmp.toFile());
            List<String> cmd = List.of(executable, tmp.toString(), "stdout",
                    "--psm", "7", "-l", "eng",
                    "-c", "tessedit_char_whitelist=" + WHITELIST,
                    "-c", "user_defined_dpi=300",
                    "tsv");
            BoundedProcess.Outcome o = BoundedProcess.run(cmd, Map.of("OMP_THREAD_LIMIT", "1"), limit, MAX_OUTPUT_BYTES);
            if (!o.ok()) return Result.EMPTY;           // timeout, runaway output or failure: "could not re-read"
            return parseTsv(new String(o.bytes(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            return Result.EMPTY;
        } finally {
            if (tmp != null) {
                try { Files.deleteIfExists(tmp); } catch (IOException ignored) { }
            }
        }
    }

    /** Reads at most max+1 bytes, so the caller can tell that the limit was exceeded. */
    static byte[] readLimited(InputStream in, int max) {
        try (in) {
            return in.readNBytes(max + 1);
        } catch (IOException e) {
            return new byte[0];
        }
    }

    /**
     * TSV columns: level page block par line word left top width height conf text.
     * Words are joined with a SPACE so "12 34" stays two tokens and is rejected downstream instead of becoming 1234.
     * Confidence is the minimum word confidence; if any word has no valid confidence it is NaN (unknown).
     */
    static Result parseTsv(String tsv) {
        List<String> words = new ArrayList<>();
        double minConf = Double.NaN;
        boolean unknown = false;
        for (String line : tsv.split("\\R")) {
            String[] c = line.split("\t", -1);
            if (c.length < 12 || !"5".equals(c[0])) continue;
            String text = c[11].trim();
            if (text.isEmpty()) continue;
            if (words.size() >= MAX_WORDS || text.length() > MAX_WORD_CHARS) {
                return Result.EMPTY;           // runaway or oversized output: unusable, never partially trusted
            }
            words.add(text);
            double conf = Confidence.parse(c[10]);      // NaN for -1, 101, Infinity, text, etc.
            if (Double.isNaN(conf)) unknown = true;
            else minConf = Double.isNaN(minConf) ? conf : Math.min(minConf, conf);
        }
        if (words.isEmpty()) return Result.EMPTY;
        return new Result(String.join(" ", words), unknown ? Double.NaN : minConf);
    }
}
