package com.carepath.assistant;
import static com.carepath.assistant.AssistantDtos.*;
import java.util.*;
import org.springframework.stereotype.Component;
/** Model chooses only approved complete phrasings; no arbitrary medical prose crosses this boundary. */
@Component
public class ExplanationValidator {
 public List<String> validate(Context context,ModelOutput output) {
  if(output==null || output.selections()==null || output.selections().size()!=context.facts().size())throw invalid();
  Map<String,Fact> allowed=new HashMap<>();for(Fact f:context.facts())allowed.put(f.id(),f);
  List<String> result=new ArrayList<>();Set<String> used=new HashSet<>();
  for(Selection choice:output.selections()){
   if(choice==null)throw invalid();Fact f=allowed.get(choice.factId());
   if(f==null || !used.add(f.id()) || choice.phrasing()<0 || choice.phrasing()>=f.approvedPhrasings().size() || !f.evidenceIds().equals(choice.evidenceIds()))throw invalid();
   result.add(f.approvedPhrasings().get(choice.phrasing()));
  }
  return List.copyOf(result);
 }
 private HealthExplanationProvider.ProviderFailure invalid(){return new HealthExplanationProvider.ProviderFailure("INVALID_OUTPUT");}
}
