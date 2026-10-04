package com.carepath.longitudinal;
import static com.carepath.longitudinal.HistoryDtos.*;
import java.util.UUID;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@RestController @Validated @RequestMapping("/api/v1/history")
public class HistoryController {
 private final HistoryService service;
 public HistoryController(HistoryService service){this.service=service;}
 @GetMapping("/events") public Page<Event> events(@RequestParam(required=false) UUID conceptId,@RequestParam(required=false) UUID documentId,@RequestParam(defaultValue="") @Size(max=80) String q,@RequestParam(defaultValue="0") @Min(0) @Max(10000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return service.events(conceptId,documentId,q,page,size);}
 @GetMapping("/concepts") public Page<Concept> concepts(@RequestParam(defaultValue="0") @Min(0) @Max(10000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return service.concepts(page,size);}
 @GetMapping("/concepts/{id}") public History history(@PathVariable UUID id,@RequestParam(defaultValue="0") @Min(0) @Max(10000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return service.history(id,page,size);}
 @GetMapping("/observations/{id}/evidence") public Point evidence(@PathVariable UUID id){return service.evidence(id);}
 @GetMapping("/compare") public Change compare(@RequestParam UUID previous,@RequestParam UUID current){return service.compare(previous,current);}
 @GetMapping("/changes") public Comparison changes(@RequestParam UUID previousReport,@RequestParam UUID currentReport){return service.reports(previousReport,currentReport);}
}
