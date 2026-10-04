package com.carepath.assistant;
import static com.carepath.assistant.AssistantDtos.*;
import static com.carepath.longitudinal.HistoryDtos.*;
import java.util.*;
import java.util.regex.*;
import java.time.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import com.carepath.longitudinal.*;
import com.carepath.terminology.Terminology;
import com.carepath.security.Ownership;
import com.carepath.foundation.ApiFailure;
@Service
@Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
public class StructuredRetrieval {
 public record Retrieved(Context context,List<Source> sources,List<Suggestion> suggestions,String uncertainty) {}
 private final HistoryService history;private final HistoryRepository repository;private final ObservationComparison comparisons;private final TrendService trends;private final Terminology terms;private final Ownership ownership;private final JdbcTemplate jdbc;
 public StructuredRetrieval(HistoryService h,HistoryRepository r,ObservationComparison c,TrendService t,Terminology terms,Ownership o,JdbcTemplate jdbc){history=h;repository=r;comparisons=c;trends=t;this.terms=terms;ownership=o;this.jdbc=jdbc;}
 public Retrieved retrieve(Ask ask){
  String q=RecordSafety.normalize(ask.question());int scopes=(ask.conceptId()!=null?1:0)+(ask.observationId()!=null?1:0)+(ask.previousReport()!=null || ask.currentReport()!=null?1:0);
  if(scopes>1 || (ask.previousReport()==null)!=(ask.currentReport()==null))throw new ApiFailure(400,"VALIDATION_FAILED","Select one record scope.");
  Builder b=new Builder();
  if(ask.observationId()!=null){b.reading(history.evidence(ask.observationId()));return b.result("OBSERVATION");}
  if(ask.previousReport()!=null){addComparison(b,history.reports(ask.previousReport(),ask.currentReport()));return b.result("REPORT_COMPARISON");}
  if(ask.conceptId()==null && (q.contains("report") || q.contains("between")) && (q.contains("chang") || q.contains("differ"))){
   List<UUID> selected=selectReports(q);if(selected.size()!=2){b.uncertainty="Select two dated reports explicitly; a unique chronological pair could not be established.";return b.result("REPORT_COMPARISON");}
   addComparison(b,history.reports(selected.get(0),selected.get(1)));return b.result("REPORT_COMPARISON");
  }
  UUID concept=ask.conceptId();
  if(concept==null){Set<UUID> matches=new HashSet<>();String[] words=q.replaceAll("[^\\p{L}\\p{N} ]"," ").split("\\s+");for(int i=0;i<words.length;i++)for(int length=1;length<=4 && i+length<=words.length;length++){var m=terms.normalize(String.join(" ",Arrays.copyOfRange(words,i,i+length)));if(m.concept()!=null)matches.add(m.concept().id());}if(matches.size()==1)concept=matches.iterator().next();}
  if(concept==null){b.uncertainty="Choose one verified measurement or two reports. Your question does not identify a supported record scope.";return b.result("INSUFFICIENT_EVIDENCE");}
  UUID owner=ownership.currentOwnerId();if(ask.conceptId()!=null && !repository.ownsConcept(concept,owner))throw HistoryService.missing();
  List<Point> all=repository.window(owner,concept);boolean bounded=all.size()>12;List<Point> points=new ArrayList<>(all.subList(0,Math.min(12,all.size())));points.sort(Comparator.comparing(Point::date,Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(Point::id));
  var months=months(q);var years=years(q);if(!months.isEmpty() || !years.isEmpty())points.removeIf(p->p.date()==null || !months.isEmpty() && !months.contains(p.date().getMonthValue()) || !years.isEmpty() && !years.contains(p.date().getYear()));
  if(points.isEmpty()){b.uncertainty="Your uploaded records do not contain enough verified information to answer that.";return b.result("INSUFFICIENT_EVIDENCE");}
  for(Point p:points)b.reading(p);
  if(q.contains("chang") || q.contains("trend") || q.contains("compar")){
   for(int i=1;i<points.size();i++)b.change(comparisons.compare(points.get(i-1),points.get(i)));
   var trend=trends.calculate(points,bounded);for(Series s:trend.series())if(s.points().size()>=3)b.fact("TREND",s.explanation(),s.points());
   if(points.size()<2 || trend.series().isEmpty())b.uncertainty="There are not enough comparable verified observations to establish a trend. Review the dates, units and report context.";
  }
  if(bounded)b.uncertainty+=" Only the latest 12 verified readings were considered; use Timeline for full history.";
  return b.result(q.contains("where") || q.contains("which")?"SOURCE":"CONCEPT");
 }
 private Set<Integer> years(String q){Set<Integer> out=new HashSet<>();var matcher=Pattern.compile("\\b(?:19|20)\\d{2}\\b").matcher(q);while(matcher.find())out.add(Integer.parseInt(matcher.group()));return out;}
 private Set<Integer> months(String q){Set<Integer> out=new LinkedHashSet<>();for(Month m:Month.values())if(Pattern.compile("\\b"+m.name().toLowerCase(Locale.ROOT)+"\\b").matcher(q).find())out.add(m.getValue());return out;}
 private List<UUID> selectReports(String q){
  var months=months(q);String sql="SELECT document_id,MIN(observed_date) AS d FROM trusted_medical_observation WHERE owner_id=? GROUP BY document_id HAVING MIN(observed_date)=MAX(observed_date) AND COUNT(observed_date)=COUNT(*) ORDER BY d DESC,document_id LIMIT 100";
  record ReportDate(UUID id,LocalDate date){}var rows=jdbc.query(sql,(r,n)->new ReportDate(r.getObject(1,UUID.class),r.getObject(2,LocalDate.class)),ownership.currentOwnerId());
  var years=years(q);if(!years.isEmpty())rows=rows.stream().filter(r->years.contains(r.date().getYear())).toList();
  if(!months.isEmpty()){rows=rows.stream().filter(r->months.contains(r.date().getMonthValue())).toList();if(rows.size()!=2 || months.size()!=2)return List.of();}
  if(rows.size()<2 || rows.get(0).date().equals(rows.get(1).date()) || rows.size()>2 && rows.get(1).date().equals(rows.get(2).date()))return List.of();
  return List.of(rows.get(1).id(),rows.get(0).id());
 }
 private void addComparison(Builder b,Comparison c){Set<UUID> ids=new HashSet<>();for(Change change:c.changes()){if(change.previous()!=null)ids.add(change.previous().id());if(change.current()!=null)ids.add(change.current().id());}if(ids.size()>12){b.uncertainty="These reports exceed the 12-reading assistant context limit. Select a measurement or use What Changed?.";return;}for(Change change:c.changes())b.change(change);b.uncertainty=c.scopeNote();}
 public boolean stillCurrent(List<Source> sources){UUID owner=ownership.currentOwnerId();for(Source s:sources)if(!repository.find(s.observation().id(),owner).filter(s.observation()::equals).isPresent())return false;return true;}
 private static class Builder {
  List<Fact> facts=new ArrayList<>();Map<UUID,Source> sources=new LinkedHashMap<>();List<Suggestion> suggestions=new ArrayList<>();String uncertainty="Numerical facts do not establish a diagnosis or clinical significance. No external medical reference ranges are added.";
  String source(Point p){return sources.computeIfAbsent(p.id(),id->new Source("e"+(sources.size()+1),p)).id();}
  void fact(String kind,String text,List<Point> points){List<String> refs=points.stream().map(this::source).toList();facts.add(new Fact("f"+(facts.size()+1),kind,text,refs,List.of(text,"Your selected verified records show: "+text)));}
  void reading(Point p){String date=p.date()==null?"date not supplied":p.date().toString();String value=p.effective().text();String unit=Objects.toString(p.effective().unit(),"(unit not supplied)");fact("READING",p.concept()+": "+value+" "+unit+" ("+date+").",List.of(p));if(suggestions.size()<3)suggestions.add(new Suggestion("What should I understand about my recorded "+p.concept()+" result, and is there anything I should discuss further?",List.of(p.id())));}
  void change(Change c){List<Point> points=new ArrayList<>();if(c.previous()!=null)points.add(c.previous());if(c.current()!=null)points.add(c.current());fact(c.type(),c.explanation(),points);if(!c.referenceTransition().equals("INSUFFICIENT_EVIDENCE"))fact("REFERENCE_COMPARISON",c.concept()+": supplied-range comparison is "+c.referenceTransition().toLowerCase(Locale.ROOT).replace('_',' ')+". Source abnormal flags are separate.",points);if(suggestions.size()<3)suggestions.add(new Suggestion("Could we discuss my "+c.concept()+" records and the reported numerical outcome ("+c.type().toLowerCase(Locale.ROOT).replace('_',' ')+") in my clinical context?",points.stream().map(Point::id).toList()));}
  Retrieved result(String intent){return new Retrieved(new Context(intent,List.copyOf(facts)),List.copyOf(sources.values()),List.copyOf(suggestions),uncertainty);}
 }
}
