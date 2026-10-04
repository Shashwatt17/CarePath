package com.carepath.intelligence;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/documents/{id}")
public class ProcessingController {
    private final ProcessingService service;
    public ProcessingController(ProcessingService service) { this.service=service; }
    @PostMapping("/process") public ResponseEntity<ExtractionData.Status> process(@PathVariable UUID id) { return ResponseEntity.accepted().body(service.request(id,false)); }
    @PostMapping("/retry") public ResponseEntity<ExtractionData.Status> retry(@PathVariable UUID id) { return ResponseEntity.accepted().body(service.request(id,true)); }
    @GetMapping("/processing-status") public ExtractionData.Status status(@PathVariable UUID id) { return service.status(id); }
    @GetMapping("/extraction") public ExtractionData.View extraction(@PathVariable UUID id) { return service.extraction(id); }
    @GetMapping("/extraction/evidence/{candidate}") public ExtractionData.Source evidence(@PathVariable UUID id,@PathVariable UUID candidate) { return service.evidence(id,candidate); }
}
