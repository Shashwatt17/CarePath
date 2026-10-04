package com.carepath.longitudinal;
import static com.carepath.longitudinal.HistoryDtos.*;
import java.util.*;
import org.springframework.stereotype.Component;
@Component
public class TrendService {
 private final ObservationComparison comparison;
 public TrendService(ObservationComparison comparison) {this.comparison=comparison;}
 public Trend calculate(List<Point> points,boolean truncated) {
  List<Series> series=new ArrayList<>();List<Point> group=new ArrayList<>();Set<String> reasons=new LinkedHashSet<>();
  var dates=new HashMap<java.time.LocalDate,Integer>();for(var p:points)if(p.date()!=null)dates.merge(p.date(),1,Integer::sum);
  for(var p:points) {
   String reason=comparison.eligibility(p);if(p.date()!=null && dates.get(p.date())>1)reason="SAME_DAY_ORDER_UNKNOWN";
   if(reason!=null) {finish(group,series);group.clear();reasons.add(reason);continue;}
   if(!group.isEmpty()) {reason=comparison.reason(group.getLast(),p);if(reason!=null){finish(group,series);group.clear();reasons.add(reason);}}
   group.add(p);
  }
  finish(group,series);if(truncated)reasons.add("BOUNDED_LATEST_500_WINDOW");
  if(series.isEmpty())reasons.add("FEWER_THAN_TWO_COMPARABLE_POINTS");
  reasons.add("REPORT_LEVEL_NUMERIC_COMPARISON_NOT_CLINICAL_EQUIVALENCE");
  return new Trend(List.copyOf(series),List.copyOf(reasons),truncated,500);
 }
 private void finish(List<Point> points,List<Series> result) {
  if(points.size()<2)return;
  Set<String> directions=new HashSet<>();for(int i=1;i<points.size();i++)directions.add(comparison.compare(points.get(i-1),points.get(i)).type());
  String pattern=points.size()<3?"INSUFFICIENT_EVIDENCE":directions.size()==1?directions.iterator().next():"MIXED";
  String text=points.size()<3?"Two comparable points; at least three are needed for a trend summary.":switch(pattern){case "INCREASED"->"Observed increase";case "DECREASED"->"Observed decrease";case "APPROXIMATELY_STABLE"->"Values were approximately stable";default->"Mixed pattern";};
  if(points.size()>=3)text+=" across "+points.size()+" verified readings in uploaded reports. Numerical pattern only.";
  result.add(new Series(points.getFirst().normalizedUnit(),List.copyOf(points),pattern,text));
 }
}
