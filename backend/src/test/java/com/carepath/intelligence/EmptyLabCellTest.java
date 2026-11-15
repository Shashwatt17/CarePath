package com.carepath.intelligence;
import static org.junit.jupiter.api.Assertions.*;
import static com.carepath.intelligence.ExtractionData.*;
import java.util.List;
import org.junit.jupiter.api.Test;
class EmptyLabCellTest {
    Result parse(String rows) {
        var result=new ExtractionPipeline(null).parse(List.of(TextPages.nativePage(1,"LAB REPORT\nReport date: 18/Jul/2026\n"+rows)));
        new ProvenanceValidator().validate(result,20,250000);
        return result;
    }
    @Test void emptyTabUnitStaysEmptyInsteadOfBecomingReference() {
        var c=parse("Novel Analyte\t4.6\t\t1 - 7").candidates().getFirst();
        assertEquals("4.6",c.originalValue());assertNull(c.originalUnit());assertEquals("1 - 7",c.referenceText());
        assertTrue(c.reasons().contains("UNIT_MISSING_OR_UNRECOGNIZED"));assertNotEquals(Band.HIGH,c.confidence());
    }
    @Test void emptyReferenceDoesNotBecomeUnitOrFlag() {
        var c=parse("Glucose\t92.4\tmg/dL\t\tH").candidates().getFirst();
        assertEquals("mg/dL",c.originalUnit());assertNull(c.referenceText());assertEquals("HIGH",c.abnormalFlag());
    }
    @Test void blankValueIsRetainedWithoutInventingNumber() {
        var c=parse("Glucose\t\tmg/dL\t70 - 140").candidates().getFirst();
        assertNull(c.originalValue());assertNull(c.numericValue());assertEquals("mg/dL",c.originalUnit());assertEquals(Band.LOW,c.confidence());
    }
    @Test void explicitPipesAndAdminExclusionRemainSafe() {
        var r=parse("Order Id\t12345\t\t1 - 99999\nAge\t25\t\t0 - 120\nNovel Analyte | 4.6 | | 1 - 7");
        assertEquals(1,r.candidates().size());assertNull(r.candidates().getFirst().originalUnit());
    }
    @Test void arbitraryTwoColumnNumbersAreNotNewMeasurements() {
        assertTrue(parse("Invoice  12345\nStreet  48\nNovel Analyte\t4.6").candidates().isEmpty());
    }
}
