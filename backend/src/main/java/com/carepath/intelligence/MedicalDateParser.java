package com.carepath.intelligence;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.regex.*;
public class MedicalDateParser {
    public static final Pattern TOKEN=Pattern.compile("(?i)(?<![\\p{L}\\d])(?:\\d{4}-\\d{2}-\\d{2}|\\d{1,2}[ /-][A-Za-z]{3}[ /-]\\d{2,4})(?![\\p{L}\\d])");
    public LocalDate parse(String input) {
        if(input==null)return null;
        for(String pattern:List.of("uuuu-MM-dd","d MMM uuuu","d/MMM/uuuu","d-MMM-uuuu")) {
            var f=new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(pattern).toFormatter(Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT);
            try { var d=LocalDate.parse(input.strip(),f);return d.getYear()>=1900&&d.getYear()<=2100?d:null; } catch(DateTimeParseException ignored) {}
        }
        return null; // Numeric locale dates and two-digit centuries are never guessed.
    }
    public List<String> tokens(String input) { return TOKEN.matcher(input).results().map(MatchResult::group).toList(); }
}
