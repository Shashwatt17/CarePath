package com.carepath.care;

import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.regex.*;

public final class FollowUpParser {

    private FollowUpParser() {}

    public record Instruction(
        String text,
        Integer duration,
        String unit,
        LocalDate date,
        String confidence
    ) {}

    private static final Pattern REL = Pattern.compile(
        "(?i)^(review|follow[ -]?up|return|next visit)\\s+(after|in)\\s+(\\d{1,3})\\s+(days?|weeks?|months?)[.!]?$"
    );

    private static final Pattern REPEAT_TEST = Pattern.compile(
        "(?i)^repeat\\s+([A-Za-z0-9][A-Za-z0-9 ./%+()_-]{0,80}?)\\s+(?:test|testing)\\s+(?:after|in)\\s+(\\d{1,3})\\s+(days?|weeks?|months?)[.!]?$"
    );

    private static final Pattern DATE = Pattern.compile(
        "(?i)^(review|follow[ -]?up|return|next visit) on (\\d{4}-\\d{2}-\\d{2}|\\d{1,2} [A-Za-z]+ \\d{4})[.!]?$"
    );

    public static Optional<Instruction> parse(String line, LocalDate anchor) {

        String text = line.strip();

        if (text.length() > 200) {
            return Optional.empty();
        }

        var m = REL.matcher(text);

        if (m.matches()) {
            return relative(
                text,
                Integer.parseInt(m.group(3)),
                m.group(4),
                anchor
            );
        }

        m = REPEAT_TEST.matcher(text);

        if (m.matches()) {
            return relative(
                text,
                Integer.parseInt(m.group(2)),
                m.group(3),
                anchor
            );
        }

        m = DATE.matcher(text);

        if (m.matches()) {
            try {
                String value = m.group(2);

                LocalDate d = value.contains("-")
                    ? LocalDate.parse(value)
                    : LocalDate.parse(
                        value,
                        DateTimeFormatter
                            .ofPattern("d MMMM uuuu", Locale.ENGLISH)
                            .withResolverStyle(ResolverStyle.STRICT)
                    );

                return Optional.of(
                    new Instruction(text, null, null, d, "HIGH")
                );

            } catch (DateTimeException e) {

                return Optional.of(
                    new Instruction(text, null, null, null, "LOW")
                );
            }
        }

        return Optional.empty();
    }

    private static Optional<Instruction> relative(
        String text,
        int duration,
        String rawUnit,
        LocalDate anchor
    ) {

        if (duration < 1 || duration > 365) {
            return Optional.empty();
        }

        String upper = rawUnit.toUpperCase(Locale.ROOT);

        String unit =
            upper.startsWith("DAY") ? "DAY" :
            upper.startsWith("WEEK") ? "WEEK" :
            "MONTH";

        LocalDate date = null;

        if (anchor != null) {
            date = switch (unit) {
                case "DAY" -> anchor.plusDays(duration);
                case "WEEK" -> anchor.plusWeeks(duration);
                default -> anchor.plusMonths(duration);
            };
        }

        return Optional.of(
            new Instruction(
                text,
                duration,
                unit,
                date,
                anchor == null ? "LOW" : "HIGH"
            )
        );
    }
}