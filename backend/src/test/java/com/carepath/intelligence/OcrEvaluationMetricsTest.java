package com.carepath.intelligence;
import static com.carepath.intelligence.ExtractionData.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
class OcrEvaluationMetricsTest {
    Candidate row(String name,String value,String unit,String date){return new Candidate(UUID.randomUUID(),name,value,null,null,unit,null,null,null,null,date==null?null:LocalDate.parse(date),null,Band.LOW,List.of(),null);}
    @Test void duplicateResultsCannotInflateRecall(){
        var expected=List.of(new OcrEvaluation.Expected("Glucose","91.2","mg/dL","2026-07-18"));
        var m=OcrEvaluation.measure(expected,List.of(row("Glucose","91.2","mg/dL","2026-07-18"),row("Glucose","91.2","mg/dL","2026-07-18")));
        assertEquals(1,m.exactValues());assertEquals(1,m.extraRows());assertEquals(0,m.missingRows());
    }
    @Test void wrongValuesUnitsDatesAndMissingRowsAreCountedSeparately(){
        var expected=List.of(new OcrEvaluation.Expected("A","1.2","mg/dL","2026-07-18"),new OcrEvaluation.Expected("B","2.3","mg/L",null));
        var m=OcrEvaluation.measure(expected,List.of(row("A","12","g/L","2025-07-18"),row("Header","42",null,null)));
        assertEquals(0,m.exactValues());assertEquals(0,m.exactUnits());assertEquals(0,m.exactDates());assertEquals(1,m.missingRows());assertEquals(1,m.extraRows());
    }
    @Test void historicalDatesMatchWithoutChoosingTheClosestValue(){
        var expected=List.of(new OcrEvaluation.Expected("Glucose","90","mg/dL","2026-07-18"),new OcrEvaluation.Expected("Glucose","100","mg/dL","2025-07-18"));
        var m=OcrEvaluation.measure(expected,List.of(row("Glucose","90","mg/dL","2025-07-18"),row("Glucose","100","mg/dL","2026-07-18")));
        assertEquals(0,m.exactValues());assertEquals(2,m.exactDates());assertEquals(0,m.extraRows());
    }
}
