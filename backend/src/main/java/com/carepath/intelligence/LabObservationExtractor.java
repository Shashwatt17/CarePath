package com.carepath.intelligence;

import static com.carepath.intelligence.ExtractionData.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.*;

public class LabObservationExtractor {

    private static final Set<String> UNITS = Set.of(
        "g/dL",
        "mg/dL",
        "ng/mL",
        "mmol/L",
        "%",
        "U/L",
        "IU/L",
        "mIU/L",
        "10^9/L",
        "10^12/L",
        "fL",
        "pg",
        "mg/L",
        "ug/L",
        "Âµg/L",
        "umol/L",
        "Âµmol/L"
    );

    private static final Pattern NUMBER = Pattern.compile(
        "^([<>â‰¤â‰¥]=?)?([+-]?\\d{1,9}(?:\\.\\d{1,8})?)$"
    );

    private static final Pattern LOOSE = Pattern.compile(
        "^([\\p{L}][\\p{L} ()/._%-]{1,100}?)\\s+" +
        "([<>â‰¤â‰¥]?[+-]?\\d+(?:\\.\\d+)?)\\s*(.*)$"
    );

    /*
     * Conservative OCR fallback:
     *
     * <tail> must end in an explicit numeric reference range.
     *
     * Examples:
     *   pod. 250-450
     *   por 136 - 392
     *   mgid) 70-140
     *
     * The token before the range is preserved as OCR evidence but is NOT
     * considered a trusted/recognized unit.
     */
    private static final Pattern OCR_RANGE_TAIL = Pattern.compile(
        "^(.{1,50}?)\\s+" +
        "([+-]?\\d+(?:\\.\\d+)?)\\s*[-–]\\s*" +
        "([+-]?\\d+(?:\\.\\d+)?)$"
    );

    private final ReferenceRangeParser ranges =
        new ReferenceRangeParser();

    private final ExtractionConfidence confidence =
        new ExtractionConfidence();

    public List<Candidate> extract(
        List<Page> pages,
        Information information
    ) {

        List<Candidate> candidates = new ArrayList<>();

        for (Page page : pages) {
            for (Line line : page.lines()) {

                Source source = TextPages.source(page, line);
                String raw = source.text().strip();

                if (raw.length() > 1000) {
                    continue;
                }

                boolean structured =
                    raw.contains("|")
                    || raw.contains("\t")
                    || raw.matches(".*\\s{2,}.*");

                boolean uncertainOcrFallback = false;

                String[] cells = structured
                    ? raw.split(
                        raw.contains("|")
                            ? "\\|"
                            : "\\s{2,}|\\t",
                        -1
                    )
                    : new String[0];

                if (!structured) {

                    var match = LOOSE.matcher(raw);

                    if (!match.matches()) {
                        continue;
                    }

                    String tail = match.group(3).strip();

                    String unit = null;
                    String reference = tail;

                    for (String known : UNITS) {
                        if (
                            tail.equals(known)
                            || tail.startsWith(known + " ")
                        ) {
                            unit = known;
                            reference = tail
                                .substring(known.length())
                                .strip();
                            break;
                        }
                    }

                    /*
                     * Existing strict path failed. Permit a candidate only
                     * when the OCR line has an explicit numeric reference
                     * interval. Never repair or normalize the OCR unit.
                     */
                    if (unit == null) {

                        var ocrRange =
                            OCR_RANGE_TAIL.matcher(tail);

                        if (!ocrRange.matches()) {
                            continue;
                        }

                        String possibleUnit =
                            clean(ocrRange.group(1));

                        /*
                         * Require some non-numeric unit-like material.
                         * This prevents arbitrary "name value low-high"
                         * prose from automatically becoming a lab row.
                         */
                        if (
                            possibleUnit == null
                            || possibleUnit.length() > 50
                            || possibleUnit.matches("[+-]?\\d+(?:\\.\\d+)?")
                        ) {
                            continue;
                        }

                        unit = possibleUnit;

                       reference = tail.substring(
    ocrRange.start(2),
    ocrRange.end(3)
).strip();

                        uncertainOcrFallback = true;
                    }

                    cells = new String[]{
                        match.group(1),
                        match.group(2),
                        unit,
                        reference
                    };
                }

                if (cells.length < 3 || cells.length > 5) {
                    continue;
                }

                String name = clean(cells[0]);
                String value = clean(cells[1]);

                if (
                    name == null
                    || name.length() > 160
                    || value != null && value.length() > 80
                    || name.contains(":")
                    || !Character.isLetter(name.codePointAt(0))
                    || Set.of(
                        "test",
                        "test name",
                        "measurement",
                        "analyte"
                    ).contains(name.toLowerCase(Locale.ROOT))
                ) {
                    continue;
                }

                if (
                    !structured
                    && (
                        value == null
                        || !value.matches(".*[0-9].*")
                    )
                ) {
                    continue;
                }

                String unit =
                    cells.length > 2
                        ? clean(cells[2])
                        : null;

                String reference =
                    cells.length > 3
                        ? clean(cells[3])
                        : null;

                String flag =
                    cells.length > 4
                        ? flag(clean(cells[4]))
                        : null;

                if (
                    unit != null && unit.length() > 50
                    || reference != null
                        && reference.length() > 255
                ) {
                    continue;
                }

                var numeric = NUMBER.matcher(
                    value == null ? "" : value
                );

                BigDecimal number = null;
                String comparator = null;

                if (numeric.matches()) {
                    number = new BigDecimal(
                        numeric.group(2)
                    );

                    comparator = numeric.group(1);
                }

                var range = ranges.parse(reference);

                var score = confidence.score(
                    number != null,
                    unit != null && UNITS.contains(unit),
                    range.text() != null,
                    range.valid(),
                    structured,
                    line,
                    page.method()
                );

                Band candidateBand = score.band();

                List<String> reasons =
                    new ArrayList<>(score.reasons());

                if (uncertainOcrFallback) {
                    candidateBand = Band.LOW;
                    reasons.add(
                        "OCR_UNRECOGNIZED_UNIT_WITH_REFERENCE_RANGE"
                    );
                }

                candidates.add(
                    new Candidate(
                        UUID.randomUUID(),
                        name,
                        value,
                        number,
                        comparator,
                        unit,
                        range.lower(),
                        range.upper(),
                        range.text(),
                        flag,
                        information.date() == null
                            ? null
                            : LocalDate.parse(
                                information.date().value()
                            ),
                        information.provider() == null
                            ? null
                            : information.provider().value(),
                        candidateBand,
                        reasons,
                        source
                    )
                );

                if (candidates.size() > 500) {
                    throw new ProcessingFailure(
                        "RESOURCE_LIMIT",
                        false
                    );
                }
            }
        }

        Map<String, Set<String>> values =
            new HashMap<>();

        for (var candidate : candidates) {
            values.computeIfAbsent(
                candidate.originalTestName(),
                k -> new HashSet<>()
            ).add(
                String.valueOf(
                    candidate.originalValue()
                )
            );
        }

        return candidates.stream()
            .map(c -> {

                if (
                    values.get(c.originalTestName())
                        .size() < 2
                ) {
                    return c;
                }

                var reasons =
                    new ArrayList<>(c.reasons());

                reasons.add("CONFLICTING_VALUES");

                return new Candidate(
                    c.id(),
                    c.originalTestName(),
                    c.originalValue(),
                    c.numericValue(),
                    c.comparator(),
                    c.originalUnit(),
                    c.referenceLower(),
                    c.referenceUpper(),
                    c.referenceText(),
                    c.abnormalFlag(),
                    c.reportDate(),
                    c.providerName(),
                    Band.LOW,
                    reasons,
                    c.source()
                );
            })
            .toList();
    }

    private static String clean(String value) {
        return value == null
            || value.isBlank()
            || value.strip().equals("-")
            ? null
            : value.strip();
    }

    private static String flag(String value) {

        if (value == null) {
            return null;
        }

        return switch (
            value.toUpperCase(Locale.ROOT)
        ) {
            case "HIGH", "H" -> "HIGH";
            case "LOW", "L" -> "LOW";
            case "ABNORMAL", "A" -> "ABNORMAL";
            default -> null;
        };
    }
}