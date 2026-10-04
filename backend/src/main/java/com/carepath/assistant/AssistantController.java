package com.carepath.assistant;
import static com.carepath.assistant.AssistantDtos.*;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import com.carepath.longitudinal.HistoryDtos.Page;
@RestController
@RequestMapping("/api/v1/assistant")
@Validated
public class AssistantController {
 private final AssistantService assistant;private final SavedQuestionService questions;
 public AssistantController(AssistantService a,SavedQuestionService q){assistant=a;questions=q;}
 @GetMapping("/config") public Map<String,Object> config(){return assistant.config();}
 @PostMapping("/ask") public Answer ask(@Valid @RequestBody Ask ask){return assistant.ask(ask);}
 @GetMapping("/questions") public Page<Saved> list(@RequestParam(defaultValue="0") @Min(0) @Max(10000) int page){return questions.list(page);}
 @GetMapping("/questions/{id}") public Saved get(@PathVariable UUID id){return questions.get(id);}
 @PostMapping("/questions") @ResponseStatus(HttpStatus.CREATED) public Saved save(@Valid @RequestBody Save save){return questions.create(save);}
 @PutMapping("/questions/{id}") public Saved edit(@PathVariable UUID id,@Valid @RequestBody Edit edit){return questions.edit(id,edit);}
 @DeleteMapping("/questions/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID id){questions.delete(id);}
}
