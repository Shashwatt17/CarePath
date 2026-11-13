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
        "(?i)(?:^|\\t)(Report date|Report generated on|Reporting date(?:/time)?|Sample collected on|Sample date|" +
        "Collection date(?:/time)?|Laboratory|Lab|Provider)(?:\\s*:\\s*|\\s+)([^\\t]+)"
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

                if (!match.find()) {
                    String text=source.text().strip();
                    var pageCount=Pattern.compile("(?i)\\bPage\\s+[1-9Il]\\d*\\s*(?:of|[Il]of)\\s+(\\d+)\\b").matcher(text);
                    if(pageCount.find() && Integer.parseInt(pageCount.group(1))>pages.size())warnings.add("POSSIBLE_MISSING_PAGES");
                    if(page.method()==Method.TESSERACT_OCR && line.ocrConfidence()!=null && line.ocrConfidence()<.70)warnings.add("LOW_IMAGE_READABILITY");
                    if(text.matches("(?i)^Date(?:\\s|:|\\|).*") && dates.tokens(text).size()>1)warnings.add("MULTIPLE_DATED_COLUMNS");
                    // A facility-like heading is only a candidate, never an inferred identity.
                    for(String cell:text.split("\\t| {2,}")){
                    String first=cell.split("\\s*\\(",2)[0].strip();
                    if(!first.isEmpty() && Character.isUpperCase(first.codePointAt(0)) && first.length()<160 && first.matches("(?i)^[\\p{L}][\\p{L} .&'-]{1,80}(?:Hospital|Laboratory|Laboratories|Labs|Diagnostics|Healthcare)(?:, [\\p{L} .'-]+)?$"))providers.add(new Fact(first,source));
                    }
                    continue;
                }

                String label = match.group(1)
                    .toLowerCase(Locale.ROOT);

                String value = match.group(2).strip();
                if(Set.of("lab","laboratory","provider").contains(label) && !source.text().contains(":"))continue;

                /*
                 * Report/Reporting dates take precedence over
                 * Sample/Collection dates.
                 */
                if (
                    label.startsWith("report") ||
                    label.startsWith("sample") ||
                    label.startsWith("collection")
                ) {

                    var tokens=dates.tokens(value);
                    var date = tokens.size()==1?dates.parse(tokens.getFirst()):null;

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