package com.carepath.intelligence;

import static com.carepath.intelligence.ExtractionData.*;
import java.util.*;

/** Validate the entire worker result before any candidate is persisted. */
public class ProvenanceValidator {

    public void validate(Result result, int maxPages, int maxCharacters) {

        if (result == null ||
            result.pages() == null ||
            result.pages().isEmpty() ||
            result.pages().size() > maxPages ||
            result.candidates() == null ||
            result.classification() == null ||
            result.information() == null ||
            result.candidates().size() > 500) {

            fail("INVALID_RESULT_STRUCTURE");
        }

        Map<Integer, Page> pages = new HashMap<>();
        int total = 0;

        for (Page page : result.pages()) {

            if (page.number() != pages.size() + 1 ||
                page.text() == null ||
                page.text().contains("\u0000") ||
                page.text().length() > 50000 ||
                page.lines() == null ||
                page.method() == null) {

                fail("INVALID_PAGE_STRUCTURE page=" + page.number());
            }

            total += page.text().length();

            if (total > maxCharacters) {
                fail("MAX_CHARACTERS_EXCEEDED");
            }

            pages.put(page.number(), page);

            for (Line line : page.lines()) {

                if (line.start() < 0 ||
                    line.end() < line.start() ||
                    line.end() > page.text().length()) {

                    fail(
                        "INVALID_LINE_OFFSETS page=" + page.number() +
                        " start=" + line.start() +
                        " end=" + line.end() +
                        " textLength=" + page.text().length()
                    );
                }

                validateSource(
                    TextPages.source(page, line),
                    pages,
                    "PAGE_LINE"
                );
            }
        }

        for (Candidate candidate : result.candidates()) {

            validateSource(
                candidate.source(),
                pages,
                "CANDIDATE:" + candidate.originalTestName()
            );

            String evidence = candidate.source().text();

            if (!evidence.contains(candidate.originalTestName())) {
                fail(
                    "CANDIDATE_NAME_NOT_IN_EVIDENCE name=[" +
                    candidate.originalTestName() +
                    "] evidence=[" + evidence + "]"
                );
            }

            if (candidate.originalValue() != null &&
                !evidence.contains(candidate.originalValue())) {

                fail(
                    "CANDIDATE_VALUE_NOT_IN_EVIDENCE value=[" +
                    candidate.originalValue() +
                    "] evidence=[" + evidence + "]"
                );
            }

            if (candidate.originalUnit() != null &&
                !evidence.contains(candidate.originalUnit())) {

                fail(
                    "CANDIDATE_UNIT_NOT_IN_EVIDENCE unit=[" +
                    candidate.originalUnit() +
                    "] evidence=[" + evidence + "]"
                );
            }

            if (candidate.referenceText() != null &&
                !evidence.contains(candidate.referenceText())) {

                fail(
                    "CANDIDATE_REFERENCE_NOT_IN_EVIDENCE reference=[" +
                    candidate.referenceText() +
                    "] evidence=[" + evidence + "]"
                );
            }

            if (candidate.reportDate() != null &&
                (result.information().date() == null ||
                 !candidate.reportDate().toString()
                     .equals(result.information().date().value()))) {

                fail(
                    "CANDIDATE_DATE_MISMATCH date=" +
                    candidate.reportDate()
                );
            }

            if (candidate.providerName() != null &&
                (result.information().provider() == null ||
                 !candidate.providerName()
                     .equals(result.information().provider().value()))) {

                fail(
                    "CANDIDATE_PROVIDER_MISMATCH provider=[" +
                    candidate.providerName() + "]"
                );
            }
        }

        for (Source evidence : result.classification().evidence()) {
            validateSource(
                evidence,
                pages,
                "CLASSIFICATION_EVIDENCE"
            );
        }

        if (result.information().date() != null) {
            validateSource(
                result.information().date().source(),
                pages,
                "DATE"
            );
        }

        if (result.information().provider() != null) {
            validateSource(
                result.information().provider().source(),
                pages,
                "PROVIDER"
            );
        }
    }

    private void validateSource(
        Source source,
        Map<Integer, Page> pages,
        String context
    ) {

        if (source == null) {
            fail(context + " SOURCE_NULL");
        }

        Page page = pages.get(source.page());

        if (page == null) {
            fail(context + " PAGE_NOT_FOUND page=" + source.page());
        }

        if (source.start() < 0 ||
            source.end() < source.start() ||
            source.end() > page.text().length()) {

            fail(
                context +
                " INVALID_SOURCE_OFFSETS page=" + source.page() +
                " start=" + source.start() +
                " end=" + source.end() +
                " textLength=" + page.text().length()
            );
        }

        if (source.method() != page.method()) {
            fail(
                context +
                " METHOD_MISMATCH source=" + source.method() +
                " page=" + page.method()
            );
        }

        String actual =
            page.text().substring(source.start(), source.end());

        if (!actual.equals(source.text())) {
            fail(
                context +
                " SOURCE_TEXT_MISMATCH expected=[" +
                source.text() +
                "] actual=[" + actual + "]"
            );
        }

        if (source.box() != null) {

            var box = source.box();

            if (!Double.isFinite(
                    box.x() +
                    box.y() +
                    box.width() +
                    box.height()
                ) ||
                box.x() < 0 ||
                box.y() < 0 ||
                box.width() < 0 ||
                box.height() < 0 ||
                box.x() + box.width() > 1.000001 ||
                box.y() + box.height() > 1.000001) {

                fail(
                    context +
                    " INVALID_BOX x=" + box.x() +
                    " y=" + box.y() +
                    " width=" + box.width() +
                    " height=" + box.height()
                );
            }
        }
    }

    private static void fail(String reason) {
        throw new ProcessingFailure(
            "INVALID_PROVENANCE:" + reason,
            false
        );
    }
}