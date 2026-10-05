package com.carepath.intelligence;

import static com.carepath.intelligence.ExtractionData.*;

import java.util.*;
import java.util.regex.*;

public class DocumentInformationExtractor {

    private final MedicalDateParser dates = new MedicalDateParser();

    /*
     * Accept controlled real-world variants while keeping extraction
     * deterministic and evidence-backed.
     */
    private static final Pattern INFORMATION_PATTERN = Pattern.compile(
        "(?i)^(Report date|Reporting date(?:/time)?|Sample date|" +
        "Collection date(?:/time)?|Laboratory|Lab|Provider):\\s*(.+)$"
    );

    public Information extract(List<Page> pages) {

        List<Fact> reports = new ArrayList<>();
        List<Fact> samples = new ArrayList<>();
        List<Fact> providers = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (Page page : pages) {
            for (Line line : page.lines()) {

                var source = TextPages.source(page, line);
                var match = INFORMATION_PATTERN.matcher(
                    source.text().strip()
                );

                if (!match.matches()) {
                    continue;
                }

                String label = match.group(1)
                    .toLowerCase(Locale.ROOT);

                String value = match.group(2).strip();

                /*
                 * Report/Reporting dates take precedence over
                 * Sample/Collection dates.
                 */
                if (
                    label.startsWith("report") ||
                    label.startsWith("sample") ||
                    label.startsWith("collection")
                ) {

                    var date = dates.parse(value);

                    if (date == null) {
                        warnings.add("UNPARSED_DATE");
                        continue;
                    }

                    var fact = new Fact(
                        date.toString(),
                        source
                    );

                    if (label.startsWith("report")) {
                        reports.add(fact);
                    } else {
                        samples.add(fact);
                    }

                } else {

                    if (value.length() <= 255) {
                        providers.add(
                            new Fact(value, source)
                        );
                    } else {
                        warnings.add("PROVIDER_TOO_LONG");
                    }
                }
            }
        }

        return new Information(
            unique(
                reports.isEmpty() ? samples : reports,
                "CONFLICTING_DATES",
                warnings
            ),
            unique(
                providers,
                "CONFLICTING_PROVIDERS",
                warnings
            ),
            warnings.stream()
                .distinct()
                .toList()
        );
    }

    private Fact unique(
        List<Fact> facts,
        String warning,
        List<String> warnings
    ) {

        if (
            facts.stream()
                .map(Fact::value)
                .distinct()
                .count() > 1
        ) {
            warnings.add(warning);
            return null;
        }

        return facts.isEmpty()
            ? null
            : facts.getFirst();
    }
}