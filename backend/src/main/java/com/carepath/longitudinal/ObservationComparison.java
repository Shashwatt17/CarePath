package com.carepath.longitudinal;
import static com.carepath.longitudinal.HistoryDtos.*;
import java.math.*;
import java.util.*;
import org.springframework.stereotype.Component;
@Component
public class ObservationComparison {
 private final StabilityPolicy policy;
 public ObservationComparison(StabilityPolicy policy) {this.policy=policy;}
 public String eligibility(Point p) {
  if(!Set.of("CONFIRMED","CORRECTED").contains(p.verificationStatus()))return "UNVERIFIED";
  if(p.conceptId()==null || !Set.of("EXACT","ALIAS_MATCH","USER_SELECTED").contains(Objects.toString(p.mappingStatus(),"")))return "CONCEPT_UNAVAILABLE";
  if(p.date()==null)return "DATE_MISSING";
  if(p.comparator()!=null && !p.comparator().equals("="))return "THRESHOLD_NOT_EXACT";
  if(p.normalizedValue()==null || p.normalizedUnit()==null || !Set.of("SAME_UNIT","CONVERTED").contains(Objects.toString(p.unitStatus(),"")))return "COMPATIBLE_UNITS_UNAVAILABLE";
  if(p.provider()==null || p.provider().isBlank())return "LAB_CONTEXT_MISSING";
  return null;
 }
 public String reason(Point a,Point b) {
  String x=eligibility(a),y=eligibility(b);if(x!=null)return x;if(y!=null)return y;
  if(!a.conceptId().equals(b.conceptId()))return "DIFFERENT_CONCEPTS";
  if(a.evidence().documentId().equals(b.evidence().documentId()))return "SAME_SOURCE_REPORT";
  if(!a.date().isBefore(b.date()))return "DATES_NOT_STRICTLY_ORDERED";
  if(!Objects.equals(a.normalizedUnit(),b.normalizedUnit()))return "COMPATIBLE_UNITS_UNAVAILABLE";
  if(!Objects.equals(a.normalizationVersion(),b.normalizationVersion()))return "NORMALIZATION_VERSION_CHANGED";
  if(!a.provider().strip().equalsIgnoreCase(b.provider().strip()) || !Objects.equals(a.specimen(),b.specimen()) || !Objects.equals(a.method(),b.method()))return "REPORT_CONTEXT_DIFFERS";
  return null;
 }
 public Change compare(Point a,Point b) {
  var why=reason(a,b);if(why!=null)return unavailable(a,b,why);
  var previous=a.normalizedValue();var current=b.normalizedValue();var delta=current.subtract(previous);
  var percentage=previous.signum()==0?null:delta.multiply(new BigDecimal("100")).divide(previous.abs(),2,RoundingMode.HALF_EVEN);
  var type=policy.direction(b.conceptId(),previous,current);String phrase=switch(type){case "INCREASED"->"increased";case "DECREASED"->"decreased";default->"was approximately stable";};
  return new Change(b.conceptId(),b.concept(),type,a,b,previous,current,b.normalizedUnit(),delta,percentage,reference(a,b),"COMPARABLE_NUMERIC_ONLY",b.concept()+" "+phrase+" from "+previous.toPlainString()+" to "+current.toPlainString()+" "+b.normalizedUnit()+" between the selected reports. This is a numerical comparison, not clinical significance.",policy.version(b.conceptId()));
 }
 public Change unavailable(Point a,Point b,String reason) {Point p=b==null?a:b;return new Change(p.conceptId(),p.concept(),"INSUFFICIENT_EVIDENCE",a,b,null,null,null,null,null,"INSUFFICIENT_EVIDENCE",reason,"This measurement cannot be compared reliably: "+reason.toLowerCase(Locale.ROOT).replace('_',' ')+".",policy.version(p.conceptId()));}
 public Change presence(Point a,Point b,boolean allowed,String reason) {if(!allowed)return unavailable(a,b,reason);var p=b==null?a:b;return new Change(p.conceptId(),p.concept(),b==null?"PREVIOUSLY_TRACKED_NOT_PRESENT":"NEWLY_OBSERVED",a,b,null,null,null,null,null,"INSUFFICIENT_EVIDENCE","SELECTED_RECORDS_ONLY",b==null?"Previously tracked in the selected earlier panel; not present among verified readings in the selected later panel. This does not establish a clinical change.":"Newly observed among verified readings in these two selected uploaded records. This does not mean a new condition.",policy.version(p.conceptId()));}
 private String reference(Point a,Point b) {
  var x=a.effective();var y=b.effective();
  if(x.number()==null || y.number()==null || x.low()==null || x.high()==null || y.low()==null || y.high()==null || !Objects.equals(x.unit(),y.unit()) || x.low().compareTo(x.high())>0 || y.low().compareTo(y.high())>0)return "INSUFFICIENT_EVIDENCE";
  // Deliberately abstain if supplied bounds or source-unit representation differ.
  if(x.low().compareTo(y.low())!=0 || x.high().compareTo(y.high())!=0)return "REFERENCE_INTERVALS_DIFFER";
  boolean before=x.number().compareTo(x.low())>=0 && x.number().compareTo(x.high())<=0;
  boolean after=y.number().compareTo(y.low())>=0 && y.number().compareTo(y.high())<=0;
  return before==after?(after?"REMAINED_INSIDE":"REMAINED_OUTSIDE"):(after?"ENTERED_REFERENCE_INTERVAL":"EXITED_REFERENCE_INTERVAL");
 }
}
