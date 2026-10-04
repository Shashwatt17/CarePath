package com.carepath.longitudinal;
import static com.carepath.longitudinal.HistoryDtos.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import com.carepath.security.Ownership;
import com.carepath.vault.DocumentRepository;
import com.carepath.foundation.ApiFailure;
@Service
@Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
public class HistoryService {
 private final HistoryRepository repository;private final Ownership ownership;private final DocumentRepository documents;private final TrendService trends;private final ObservationComparison comparison;
 public HistoryService(HistoryRepository repository,Ownership ownership,DocumentRepository documents,TrendService trends,ObservationComparison comparison){this.repository=repository;this.ownership=ownership;this.documents=documents;this.trends=trends;this.comparison=comparison;}
 public static ApiFailure missing(){return new ApiFailure(404,"NOT_FOUND","The requested resource was not found.");}
 private UUID owner(){return ownership.currentOwnerId();}
 private void concept(UUID id){if(!repository.ownsConcept(id,owner()))throw missing();}
 public Page<Concept> concepts(int page,int size){return repository.concepts(owner(),page,size);}
 public Page<Event> events(UUID concept,UUID document,String q,int page,int size){if(concept!=null)concept(concept);if(document!=null)ownership.require(document,documents::findByIdAndOwnerId);return repository.events(owner(),concept,document,q,page,size);}
 public History history(UUID concept,int page,int size){concept(concept);var window=new ArrayList<>(repository.window(owner(),concept));boolean truncated=window.size()>500;if(truncated)window.removeLast();window.sort(Comparator.comparing(Point::date,Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(Point::id));return new History(repository.history(owner(),concept,page,size),trends.calculate(window,truncated));}
 public Point evidence(UUID id){return ownership.require(id,repository::find);}
 public Change compare(UUID previous,UUID current){return comparison.compare(evidence(previous),evidence(current));}
 public Comparison reports(UUID previous,UUID current){
  ownership.require(previous,documents::findByIdAndOwnerId);ownership.require(current,documents::findByIdAndOwnerId);
  var a=repository.report(owner(),previous);var b=repository.report(owner(),current);var x=repository.reportPoints(owner(),previous);var y=repository.reportPoints(owner(),current);
  if(x.size()>500 || y.size()>500)throw new ApiFailure(422,"COMPARISON_LIMIT","Select reports containing at most 500 verified readings.");
  Map<UUID,List<Point>> before=group(x),after=group(y);var keys=new LinkedHashSet<>(before.keySet());keys.addAll(after.keySet());
  boolean context=reportContext(a,b,x,y);long shared=before.keySet().stream().filter(after::containsKey).filter(k->k!=null).count();
  String panelA=repository.panel(owner(),previous),panelB=repository.panel(owner(),current);
  boolean panel=context && panelA!=null && panelA.equals(panelB);
  boolean newScope=context && (panel || shared>=3);
  List<Change> changes=new ArrayList<>();
  for(UUID key:keys){var left=before.getOrDefault(key,List.of());var right=after.getOrDefault(key,List.of());
   if(left.size()>1 || right.size()>1){for(var p:left)changes.add(comparison.unavailable(p,null,"MULTIPLE_READINGS_IN_REPORT"));for(var p:right)changes.add(comparison.unavailable(null,p,"MULTIPLE_READINGS_IN_REPORT"));continue;}
   Point p=left.isEmpty()?null:left.getFirst(),q=right.isEmpty()?null:right.getFirst();
   if(p!=null && q!=null)changes.add(comparison.compare(p,q));
   else {Point present=q==null?p:q;String eligibility=comparison.eligibility(present);changes.add(comparison.presence(p,q,eligibility==null && (q==null?panel:newScope),eligibility!=null?eligibility:"REPORT_COVERAGE_NOT_ESTABLISHED"));}
  }
  changes.sort(Comparator.comparing(Change::concept).thenComparing(Change::type));
  return new Comparison(withPanel(a,panelA),withPanel(b,panelB),List.copyOf(changes),"Selected verified records only. Absence requires matching explicit panel headings and fully reviewed reports. Newly observed requires that scope or at least three shared concepts from the same lab. Unknown assay/specimen context is not proof of clinical equivalence.");
 }
 private Report withPanel(Report r,String panel){return new Report(r.id(),r.filename(),r.category(),r.status(),r.date(),r.provider(),panel,r.fullyReviewed());}
 private Map<UUID,List<Point>> group(List<Point> points){Map<UUID,List<Point>> out=new LinkedHashMap<>();for(var p:points)out.computeIfAbsent(p.conceptId(),k->new ArrayList<>()).add(p);return out;}
 private boolean reportContext(Report a,Report b,List<Point> x,List<Point> y){
  if(!a.fullyReviewed() || !b.fullyReviewed() || !a.status().equals("COMPLETED") || !b.status().equals("COMPLETED") || !a.category().equals("LAB_REPORT") || !b.category().equals("LAB_REPORT") || a.date()==null || b.date()==null || !a.date().isBefore(b.date()) || a.provider()==null || !a.provider().equalsIgnoreCase(Objects.toString(b.provider(),"")))return false;
  return !x.isEmpty() && !y.isEmpty() && x.stream().allMatch(p->Objects.equals(p.date(),a.date()) && Objects.equals(p.provider(),a.provider())) && y.stream().allMatch(p->Objects.equals(p.date(),b.date()) && Objects.equals(p.provider(),b.provider()));
 }
}
