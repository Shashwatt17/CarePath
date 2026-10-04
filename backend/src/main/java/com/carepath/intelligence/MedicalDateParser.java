package com.carepath.intelligence;
import java.time.*;
import java.time.format.*;
import java.util.*;
public class MedicalDateParser {
    public LocalDate parse(String input) {
        for(var format:List.of(DateTimeFormatter.ISO_LOCAL_DATE,DateTimeFormatter.ofPattern("d MMM uuuu",Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT))) {
            try { return LocalDate.parse(input.strip(),format); } catch(DateTimeParseException ignored) {}
        }
        return null; // Ambiguous numeric locale dates are not guessed.
    }
}
