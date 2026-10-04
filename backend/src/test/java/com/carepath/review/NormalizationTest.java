package com.carepath.review;
import com.carepath.identity.AuthTestSupport;
import com.carepath.terminology.*;
import java.math.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;
class NormalizationTest extends AuthTestSupport {
    @Autowired Terminology terms;@Autowired UnitNormalizer units;@Autowired ReviewPolicy policy;
    @ParameterizedTest @ValueSource(strings={"Hb","HGB","Hemoglobin","Haemoglobin"," hGb ","HEMOGLOBIN"})
    void knownHemoglobinTerms(String term) {var m=terms.normalize(term);assertEquals("Hemoglobin",m.concept().name());assertEquals(term,m.originalTerm());assertNull(m.concept().standardIdentifier());}
    @Test void exactAndAliasDistinguished() {assertEquals("EXACT",terms.normalize("Hemoglobin").status());assertEquals("ALIAS_MATCH",terms.normalize("Hb").status());}
    @ParameterizedTest @ValueSource(strings={"Hembglobin","Fast glucose","Fasting Plasma Glucose","UNKNOWN","Vitamin D3"})
    void unknownOrContextualNamesNeverFuzzyMapped(String term) {assertEquals("UNMAPPED",terms.normalize(term).status());assertNull(terms.normalize(term).concept());}
    @Test void ambiguityNotCollapsed() {var m=terms.normalize("urea/bun");assertEquals("AMBIGUOUS",m.status());assertNull(m.concept());assertEquals(2,m.alternatives().size());}
    @Test void clinicalDistinctionsAndWhitespacePreserved() {assertNotEquals(terms.normalize("Urea").concept().id(),terms.normalize("BUN").concept().id());assertNotEquals(terms.normalize("Hemoglobin").concept().id(),terms.normalize("HbA1c").concept().id());assertEquals("RBC count",terms.normalize("red   blood cell count").concept().name());assertEquals("Glucose",terms.normalize("Glucose").concept().name());}
    @Test void dictionaryScopeAndNoInventedIdentifiers() {assertEquals(23,terms.concepts().size());assertTrue(terms.concepts().stream().allMatch(c->c.standardIdentifier()==null && c.identifierSystem()==null));}
    UnitNormalizer.Normalized unit(String concept,String value,String unit) {return units.normalize(terms.normalize(concept).concept(),new BigDecimal(value),unit,null);}
    @Test void sameUnitRetainsExactValueAndScale() {var n=unit("Hb","10.40","g/dL");assertEquals(new BigDecimal("10.40"),n.value());assertEquals("SAME_UNIT",n.status());}
    @Test void deterministicScaleConversion() {var original=new BigDecimal("104");var n=units.normalize(terms.normalize("Hb").concept(),original,"g/L",null);assertEquals(0,new BigDecimal("10.4").compareTo(n.value()));assertEquals("g/dL",n.unit());assertEquals(new BigDecimal("104"),original);}
    @Test void conceptDependentMolecularConversion() {assertEquals(new BigDecimal("5.0"),unit("Glucose","90","mg/dL").value());assertEquals(new BigDecimal("90"),unit("Creatinine","90","mg/dL").value());assertNull(unit("Sodium","90","mg/dL").value());}
    @Test void incompatibleAndUnsupportedUnitsAbstain() {for(String u:List.of("mmol/L","mystery","G/DL")) {var n=unit("Hb","10.4",u);assertNull(n.value());assertNull(n.unit());assertEquals("UNSUPPORTED_UNIT",n.status());}}
    @Test void missingUnitAndConceptAbstain() {assertEquals("MISSING_UNIT",unit("Hb","10.4",null).status());assertEquals("UNKNOWN_CONCEPT",unit("Unknown","10.4","g/dL").status());}
    @Test void comparatorNotTurnedIntoExactPoint() {var n=units.normalize(terms.normalize("Hb").concept(),new BigDecimal("10"),"g/dL","<");assertEquals("COMPARATOR_NOT_EXACT",n.status());assertNull(n.value());}
    @Test void roundingUsesSourceSignificanceAndHalfEven() {assertEquals(new BigDecimal("277.6"),unit("Glucose","5000","mg/dL").value());assertEquals(new BigDecimal("832.6"),unit("Glucose","15000","mg/dL").value());assertEquals(new BigDecimal("9.99"),unit("Glucose","180","mg/dL").value());assertEquals(new BigDecimal("0.56"),unit("Glucose","10","mg/dL").value());assertEquals(new BigDecimal("5.000"),unit("Glucose","90.075662","mg/dL").value());}
    @Test void rangeComparisonUsesSuppliedOriginalUnitsBeforeRounding() {var p=policy.preview(new ReviewDtos.Fields("Glucose","90","mg/dL","90 - 100",null,null),true);assertEquals("WITHIN",p.derivedRangeStatus());var q=policy.preview(new ReviewDtos.Fields("Glucose","89.999","mg/dL","90 - 100",null,null),true);assertEquals("BELOW",q.derivedRangeStatus());}
    @Test void textualMissingAndReversedRanges() {var p=policy.preview(new ReviewDtos.Fields("Hb","10","g/dL","See laboratory note",null,null),true);assertEquals("NOT_COMPARABLE",p.derivedRangeStatus());assertNull(p.referenceLow());assertFalse(policy.preview(new ReviewDtos.Fields("Hb","10","g/dL","16 - 12",null,null),true).canVerify());}
    @Test void negativeOrMalformedValueFailsVerification() {for(String v:List.of("-1","NaN","1e100","10.4junk","l.8?")) assertFalse(policy.preview(new ReviewDtos.Fields("Hb",v,"g/dL",null,null,null),true).canVerify());}
}
