package com.carepath.assistant;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
@Component
public class RecordSafety {
 public static final String VERSION="record-safety-en-v1";
 public static final List<String> REFERENCES=List.of("https://www.nhs.uk/conditions/heart-attack/","https://www.nhs.uk/conditions/stroke/symptoms/","https://www.londonambulance.nhs.uk/calling-us/calling-999/");
 public static final String URGENT="If these symptoms are happening now, seek emergency medical help now through your local emergency service. Do not wait for a record explanation. CarePath cannot assess emergencies.";
 public static final String BOUNDARY="CarePath can explain verified records, but cannot diagnose a disease, choose medication or recommend a dose or treatment change. Discuss those decisions with a qualified clinician.";
 private static final Pattern URGENT_PATTERN=Pattern.compile("severe (?:difficulty breathing|chest pain|uncontrolled bleeding)|can(?:not|'t) breathe|loss of consciousness|unconscious|not responding|signs of (?:a )?stroke|face droop|slurred speech|heavy bleeding|bleeding (?:won't|will not) stop");
 private static final Pattern UNSAFE=Pattern.compile("diagnos|what disease|do i have|medicat|medicine|dosage|\\bdose\\b|prescri|treatment|\\b(?:start|stop|take|increase|decrease) (?:my |the )?(?:pills|tablets|insulin)\\b");
 public static String normalize(String input){return Normalizer.normalize(input,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).replace('’','\'').replaceAll("\\s+"," ").strip();}
 public String classify(String question){String q=normalize(question);if(URGENT_PATTERN.matcher(q).find())return "URGENT";if(UNSAFE.matcher(q).find())return "BOUNDARY";return "RECORD";}
}
