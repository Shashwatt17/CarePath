package com.carepath.intelligence;

import static com.carepath.intelligence.ExtractionData.*;
import java.util.*;

public class DocumentClassifier {

    private static final Map<String, String> HEADINGS = Map.ofEntries(
        Map.entry("LAB REPORT", "LAB_REPORT"),
        Map.entry("LABORATORY REPORT", "LAB_REPORT"),
        Map.entry("LABORATORY INVESTIGATION REPORT", "LAB_REPORT"),
        Map.entry("LABORATORY INVESTIGATIONS REPORT", "LAB_REPORT"),
        Map.entry("PRESCRIPTION", "PRESCRIPTION"),
        Map.entry("DIAGNOSTIC REPORT", "DIAGNOSTIC_REPORT"),
        Map.entry("DISCHARGE SUMMARY", "DISCHARGE_SUMMARY"),
        Map.entry("VACCINATION RECORD", "VACCINATION_RECORD"),
        Map.entry("DOCTOR NOTE", "DOCTOR_NOTE"),
        Map.entry("REFERRAL", "REFERRAL"),
        Map.entry("MEDICAL BILL", "MEDICAL_BILL")
    );

    private static final List<String> STRONG_LAB_PHRASES = List.of(
        "LABORATORY INVESTIGATION REPORT",
        "LABORATORY INVESTIGATION",
        "CLINICAL BIOCHEMISTRY"
    );

    private static final List<String> LAB_SIGNALS = List.of(
        "REFERENCE RANGE",
        "REF RANGE",
        "REF INTERVAL",
        "BIO REF",
        "RESULT",
        "UNIT",
        "SPECIMEN",
        "SAMPLE",
        "COLLECTION DATE",
        "COLLECTION DATE/TIME",
        "REPORTING DATE",
        "REPORTING DATE/TIME",
        "INVESTIGATION"
    );

    private static final List<String> LAB_UNITS = List.of(
        "MG/DL",
        "G/DL",
        "NG/ML",
        "MMOL/L",
        "UG/L",
        "UMOL/L",
        "MIU/L",
        "IU/L",
        "U/L"
    );

    public Classification classify(List<Page> pages) {

        Map<String, List<Source>> found = new LinkedHashMap<>();

        /*
         * Stage 1:
         * Preserve deterministic exact-heading classification.
         */
        for (Page page : pages) {
            for (Line line : page.lines()) {

                Source source = TextPages.source(page, line);

                String normalized = normalize(
                    source.text()
                );

                String category = HEADINGS.get(normalized);

                if (category != null) {
                    found.computeIfAbsent(
                        category,
                        k -> new ArrayList<>()
                    ).add(source);
                }
            }
        }

        /*
         * One unambiguous explicit heading wins.
         */
        if (found.size() == 1) {

            var entry = found.entrySet()
                .iterator()
                .next();

            boolean nativeHeading = entry.getValue()
                .stream()
                .allMatch(
                    source ->
                        source.method() == Method.PDFBOX_TEXT
                );

            return new Classification(
                entry.getKey(),
                nativeHeading ? Band.HIGH : Band.MEDIUM,
                "EXACT_HEADING_V2",
                entry.getValue()
            );
        }

        /*
         * Multiple contradictory explicit document types must never
         * be resolved using heuristics.
         */
        if (found.size() > 1) {

            return new Classification(
                "OTHER",
                Band.LOW,
                "EXACT_HEADING_V2",
                found.values()
                    .stream()
                    .flatMap(List::stream)
                    .limit(10)
                    .toList()
            );
        }

        /*
         * Stage 2:
         * Detect strong laboratory-specific phrases.
         *
         * This handles OCR reports such as:
         *
         * "Laboratory Investigation Report"
         * "Clinical Biochemistry"
         *
         * even when units elsewhere in the report are corrupted.
         */
        List<Source> strongEvidence = new ArrayList<>();

        for (Page page : pages) {
            for (Line line : page.lines()) {

                Source source = TextPages.source(page, line);

                String upper = normalize(
                    source.text()
                );

                boolean strongMatch = STRONG_LAB_PHRASES
                    .stream()
                    .anyMatch(upper::contains);

                if (strongMatch) {

                    if (strongEvidence.size() < 10) {
                        strongEvidence.add(source);
                    }
                }
            }
        }

        /*
         * "Laboratory Investigation Report" is itself strong
         * document-type evidence.
         */
        boolean explicitLabInvestigation =
            strongEvidence.stream()
                .map(Source::text)
                .map(DocumentClassifier::normalize)
                .anyMatch(text ->
                    text.contains(
                        "LABORATORY INVESTIGATION REPORT"
                    )
                );

        if (explicitLabInvestigation) {

            return new Classification(
                "LAB_REPORT",
                Band.MEDIUM,
                "LAB_STRONG_HEADING_V1",
                strongEvidence
            );
        }

        /*
         * Stage 3:
         * Evidence fallback for reports where OCR damaged the
         * document heading.
         *
         * Multiple independent laboratory signals are required
         * so ordinary numeric documents are not promoted to
         * LAB_REPORT.
         */
        int structuralSignals = 0;
        int unitSignals = 0;

        List<Source> evidence = new ArrayList<>();

        for (Page page : pages) {
            for (Line line : page.lines()) {

                Source source = TextPages.source(page, line);

                String upper = normalize(
                    source.text()
                );

                boolean structuralMatch =
                    LAB_SIGNALS.stream()
                        .anyMatch(upper::contains);

                boolean unitMatch =
                    LAB_UNITS.stream()
                        .anyMatch(upper::contains);

                if (structuralMatch) {

                    structuralSignals++;

                    if (evidence.size() < 10) {
                        evidence.add(source);
                    }
                }

                if (unitMatch) {

                    unitSignals++;

                    if (
                        evidence.size() < 10
                        && !evidence.contains(source)
                    ) {
                        evidence.add(source);
                    }
                }
            }
        }

        if (
            structuralSignals >= 2
            && unitSignals >= 2
        ) {

            return new Classification(
                "LAB_REPORT",
                Band.MEDIUM,
                "LAB_EVIDENCE_V3",
                evidence
            );
        }

        return new Classification(
            "OTHER",
            Band.LOW,
            "LAB_EVIDENCE_V3",
            evidence
        );
    }

    /*
     * OCR commonly introduces inconsistent whitespace and casing.
     * Keep normalization deliberately conservative:
     * no fuzzy spelling correction and no semantic guessing.
     */
    private static String normalize(String value) {

        if (value == null) {
            return "";
        }

        return value
            .strip()
            .replaceAll("\\s+", " ")
            .toUpperCase(Locale.ROOT);
    }
}