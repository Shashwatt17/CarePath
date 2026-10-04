package com.carepath.review;
import static com.carepath.review.ReviewDtos.*;
import com.carepath.terminology.Terminology;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@RestController @Validated @RequestMapping("/api/v1")
public class ReviewController {
    private final ReviewService service;private final Terminology terms;
    public ReviewController(ReviewService service,Terminology terms) {this.service=service;this.terms=terms;}
    @GetMapping("/terminology/concepts") public List<Terminology.Concept> concepts() {return terms.concepts();}
    @GetMapping("/review/candidates") public ReviewDtos.Queue list(@RequestParam(required=false) UUID documentId,@RequestParam(defaultValue="PENDING_REVIEW") String state,@RequestParam(defaultValue="0") @Min(0) @Max(10000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {return service.list(documentId,state.isBlank()?null:state,page,size);}
    @GetMapping("/review/candidates/{id}") public CandidateView get(@PathVariable UUID id) {return service.get(id);}
    @PostMapping("/review/candidates/{id}/preview") public Preview preview(@PathVariable UUID id,@Valid @RequestBody Fields fields) {return service.preview(id,fields);}
    @PostMapping("/review/candidates/{id}/confirm") public ActionResult confirm(@PathVariable UUID id,@Valid @RequestBody Confirm input) {return service.act(id,"CONFIRM",input.version(),null,null,input);}
    @PostMapping("/review/candidates/{id}/correct") public ActionResult correct(@PathVariable UUID id,@Valid @RequestBody Correct input) {return service.act(id,"CORRECT",input.version(),input.fields(),input.reason(),input);}
    @PostMapping("/review/candidates/{id}/reject") public ActionResult reject(@PathVariable UUID id,@Valid @RequestBody Reject input) {return service.act(id,"REJECT",input.version(),null,input.reason(),input);}
    @GetMapping("/observations/{id}") public Observation observation(@PathVariable UUID id) {return service.observation(id);}
}
