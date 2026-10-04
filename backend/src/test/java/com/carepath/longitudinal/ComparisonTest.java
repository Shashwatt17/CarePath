package com.carepath.longitudinal;
import static com.carepath.longitudinal.HistoryDtos.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.time.*;
import java.math.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
class ComparisonTest {
 static final UUID CONCEPT=UUID.fromString("1ae1add1-f6bd-56a7-b9a4-6b91de411054");
 final ObservationComparison engine=new ObservationComparison(new StabilityPolicy(new BigDecimal("0.01"),""));
 Point point(String value,int day){return point(value,day,"g/dL","12","16","Lab",CONCEPT,"CONFIRMED",null);}
 Point point(String value,int day,String unit,String low,String high,String lab,UUID concept,String verification,String comparator){
  BigDecimal n=value==null?null:new BigDecimal(value);Reading r=new Reading(value,n,unit,low==null?"See note":low+" - "+high,low==null?null:new BigDecimal(low),high==null?null:new BigDecimal(high),"HIGH");
  return new Point(UUID.randomUUID(),concept,"Hemoglobin",day==0?null:LocalDate.of(2026,1,day),lab,null,null,"v1","EXACT",unit==null?"MISSING_UNIT":"SAME_UNIT",comparator,verification,Instant.EPOCH,r,r,n,unit,new Evidence(UUID.randomUUID(),UUID.randomUUID(),"SYNTHETIC.pdf",2,"Hb | "+value,0,7,"PDFBOX_TEXT"));
 }
 @ParameterizedTest @CsvSource({"11.3,10.4,DECREASED","19,31,INCREASED","245,247,APPROXIMATELY_STABLE","140,140,APPROXIMATELY_STABLE","0,0,APPROXIMATELY_STABLE","0,1,INCREASED","-10,-5,INCREASED","-5,-10,DECREASED"})
 void deterministicDirections(String a,String b,String expected){assertEquals(expected,engine.compare(point(a,1),point(b,2)).type());}
 @Test void deltaAndZeroPercentage(){var c=engine.compare(point("11.3",1),point("10.4",2));assertEquals(new BigDecimal("-0.9"),c.absoluteDelta());assertEquals(new BigDecimal("-7.96"),c.percentageDelta());assertNull(engine.compare(point("0",1),point("10",2)).percentageDelta());assertEquals(new BigDecimal("50.00"),engine.compare(point("-10",1),point("-5",2)).percentageDelta());}
 @Test void configurableBoundaryBeforeRounding(){assertEquals("APPROXIMATELY_STABLE",engine.compare(point("99",1),point("100",2)).type());assertEquals("INCREASED",engine.compare(point("98.99999999",1),point("100",2)).type());var strict=new ObservationComparison(new StabilityPolicy(BigDecimal.ZERO,""));assertEquals("INCREASED",strict.compare(point("245",1),point("247",2)).type());var overridden=new ObservationComparison(new StabilityPolicy(BigDecimal.ZERO,CONCEPT+"=0.01"));assertEquals("APPROXIMATELY_STABLE",overridden.compare(point("245",1),point("247",2)).type());}
 @Test void policyValidation(){assertThrows(IllegalArgumentException.class,()->new StabilityPolicy(new BigDecimal("-0.1"),""));assertThrows(IllegalArgumentException.class,()->new StabilityPolicy(new BigDecimal("0.5"),""));}
 @ParameterizedTest @CsvSource({"11,12,ENTERED_REFERENCE_INTERVAL","12,11,EXITED_REFERENCE_INTERVAL","13,14,REMAINED_INSIDE","10,11,REMAINED_OUTSIDE"})
 void referenceOwnSuppliedBounds(String a,String b,String expected){assertEquals(expected,engine.compare(point(a,1),point(b,2)).referenceTransition());}
 @Test void changedOrMissingRangesAbstain(){assertEquals("REFERENCE_INTERVALS_DIFFER",engine.compare(point("11",1),point("12",2,"g/dL","10","16","Lab",CONCEPT,"CONFIRMED",null)).referenceTransition());assertEquals("INSUFFICIENT_EVIDENCE",engine.compare(point("11",1),point("12",2,"g/dL",null,null,"Lab",CONCEPT,"CONFIRMED",null)).referenceTransition());}
 @Test void differentLabConceptDateUnitAndUnverifiedAbstain(){
  var a=point("12",1);var bad=List.of(point("13",2,"mmol/L","12","16","Lab",CONCEPT,"CONFIRMED",null),point("13",2,"g/dL","12","16","Other lab",CONCEPT,"CONFIRMED",null),point("13",2,"g/dL","12","16","Lab",UUID.randomUUID(),"CONFIRMED",null),point("13",2,"g/dL","12","16","Lab",CONCEPT,"PENDING",null),point("13",0),point("13",1),point(null,2),point("13",2,"g/dL","12","16","Lab",CONCEPT,"CONFIRMED","<"));
  for(int i=0;i<bad.size();i++){var c=engine.compare(a,bad.get(i));assertEquals("INSUFFICIENT_EVIDENCE",c.type());assertNull(c.absoluteDelta());}
 }
 @Test void sourcesPreservedExactly(){var a=point("12",1);var b=point("10",2);var result=engine.compare(a,b);assertSame(a,result.previous());assertSame(b,result.current());assertEquals(2,result.previous().evidence().page());}
 @Test void trendThreePointsMixedStableAndTwo(){var t=new TrendService(engine);assertEquals("DECREASED",t.calculate(List.of(point("12.1",1),point("11.3",2),point("10.4",3)),false).series().getFirst().pattern());assertEquals("MIXED",t.calculate(List.of(point("12",1),point("11",2),point("13",3)),false).series().getFirst().pattern());assertEquals("APPROXIMATELY_STABLE",t.calculate(List.of(point("140",1),point("140",2),point("140",3)),false).series().getFirst().pattern());assertEquals("INSUFFICIENT_EVIDENCE",t.calculate(List.of(point("12",1),point("13",2)),false).series().getFirst().pattern());}
 @Test void noChartBridgingSameDayOrMissingEvidence(){var t=new TrendService(engine);assertTrue(t.calculate(List.of(point("12",1),point("13",1),point("11",2)),false).series().isEmpty());assertTrue(t.calculate(List.of(point("12",1),point(null,2),point("11",3)),false).series().isEmpty());assertTrue(t.calculate(List.of(point("12",0)),false).limitations().contains("DATE_MISSING"));}
 @Test void safeNormalizationUsesEffectiveSourceRangeWithoutBorrowingOtherUnits(){
  var a=point("12",1);var raw=point("130",2,"g/L","120","160","Lab",CONCEPT,"CORRECTED",null);
  var b=new Point(raw.id(),raw.conceptId(),raw.concept(),raw.date(),raw.provider(),null,null,"v1","ALIAS_MATCH","CONVERTED",null,raw.verificationStatus(),raw.verifiedAt(),raw.original(),raw.effective(),new BigDecimal("13"),"g/dL",raw.evidence());
  var change=engine.compare(a,b);assertEquals("INCREASED",change.type());assertEquals(new BigDecimal("1"),change.absoluteDelta());assertEquals("INSUFFICIENT_EVIDENCE",change.referenceTransition());assertEquals("130",change.current().original().text());
 }
 @Test void contextChangesOrUnknownUnitDoNotCreateNumericalClaims(){
  var a=point("12",1);var p=point("13",2);var b=new Point(p.id(),p.conceptId(),p.concept(),p.date(),p.provider(),"plasma","assay",p.normalizationVersion(),p.mappingStatus(),p.unitStatus(),null,p.verificationStatus(),p.verifiedAt(),p.original(),p.effective(),p.normalizedValue(),p.normalizedUnit(),p.evidence());
  assertEquals("REPORT_CONTEXT_DIFFERS",engine.compare(a,b).reason());assertEquals("INSUFFICIENT_EVIDENCE",engine.compare(a,point("13",2,null,"12","16","Lab",CONCEPT,"CONFIRMED",null)).type());
 }

}
