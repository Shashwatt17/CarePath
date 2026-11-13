package com.carepath.intelligence;
import static com.carepath.intelligence.ExtractionData.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
class LabLayoutTest {
    Result parse(String text){return new ExtractionPipeline(null).parse(List.of(TextPages.nativePage(1,"LAB REPORT\n"+text)));}
    @Test void administrativeColumnsAreNotMeasurements(){
        var r=parse("Age/Gender\t: 25Y /Male\tSample Collected On\t: 18/Jul/2026\nOrder Id\t: 98765\tSample Received On\t: 18/Jul/2026\nCustomer Since\t: 17/Jul/2026\tSample Temperature\t: Maintained\nUnfamiliar Analyte\t12.4\tmg/dL\t1 - 20");
        assertEquals(1,r.candidates().size());assertEquals("Unfamiliar Analyte",r.candidates().getFirst().originalTestName());
    }
    @Test void rightColumnDatesAndFacilityFooterKeepEvidence(){
        var r=parse("Referrer\tSelf\tReport Generated On\t: 18/Jul/2026 03:29PM\nstate-of-the-art labs\nDocument code: SYNTHETIC\tExample Labs (A Unit of Example Services)\nGlucose\t90\tmg/dL\t70 - 140");
        assertEquals("2026-07-18",r.information().date().value());assertEquals("Example Labs",r.information().provider().value());new ProvenanceValidator().validate(r,20,250000);
    }
    @Test void reportDateTakesPrecedenceOverCollectionDate(){
        var r=parse("Sample Collected On : 17/Jul/2026 12:00PM\nReport Generated On : 18/Jul/2026 01:00PM");
        assertEquals("2026-07-18",r.information().date().value());
    }
    @Test void wrappedReferenceCategoriesRemainVerbatimAndDoNotBecomeAnInterval(){
        var r=parse("Cholesterol\t180\tmg/dL\tDesirable: <190\nMethod: Enzymatic\tBorderline: 190 -\n229\nHigh: >=230\nGlucose\t90\tmg/dL\t70 - 140");
        assertEquals(2,r.candidates().size());var c=r.candidates().getFirst();
        assertEquals("Desirable: <190\nBorderline: 190 -\n229\nHigh: >=230",c.referenceText());assertNull(c.referenceUpper());assertNull(c.referenceLower());assertEquals(Band.LOW,c.confidence());
        assertFalse(c.referenceText().contains("Method"));assertTrue(c.source().text().contains("Method"));new ProvenanceValidator().validate(r,20,250000);
    }
    @Test void nextReadingDoesNotLeakIntoPreviousReference(){
        var r=parse("Glucose\t90\tmg/dL\t70 - 140\nHigh Sensitivity CRP\t2\tmg/L\t0 - 5\nMethod: Example");
        assertEquals("70 - 140",r.candidates().getFirst().referenceText());assertEquals(2,r.candidates().size());
    }
    @Test void ambiguousNumericPunctuationIsNotSilentlyRepaired(){
        var c=parse("LDL / HDL Ratio\t2:34\tRatio\tDesirable: 0.5-3.0").candidates().getFirst();
        assertEquals("2:34",c.originalValue());assertNull(c.numericValue());assertTrue(c.reasons().contains("AMBIGUOUS_NUMERIC_PUNCTUATION"));
    }
    @Test void assembledReferenceRejectsInventedOrReorderedFragments(){
        assertTrue(ProvenanceValidator.referenceInEvidence("Low: <10\nHigh: >20","Low: <10\nMethod: Example\tHigh: >20",true));
        assertFalse(ProvenanceValidator.referenceInEvidence("High: >20\nLow: <10","Low: <10\nHigh: >20",true));
        assertFalse(ProvenanceValidator.referenceInEvidence("Low: <10\nHigh: >99","Low: <10\nHigh: >20",true));
        assertFalse(ProvenanceValidator.referenceInEvidence("Low: <10\nHigh: >20","Low: <10\nMethod: Example\tHigh: >20",false));
    }
}
