package com.carepath.search;

import com.carepath.longitudinal.HistoryDtos.Page;
import com.carepath.security.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/search")
public class SearchController {
    public enum Kind { ALL, DOCUMENT, OBSERVATION, SYMPTOM, APPOINTMENT }
    public record Query(@NotNull @Size(max=80) String q, @NotNull Kind kind,
        LocalDate from, LocalDate to, @Size(max=80) String provider,
        @Min(0) @Max(10000) int page) {}
    public record Result(Kind kind, UUID id, String title, LocalDate date, String provider,
        String summary, UUID documentId, UUID candidateId) {}
    private final SearchService service;
    public SearchController(SearchService service) { this.service=service; }
    // Search terms can reveal health information: JSON body, never a URL query string.
    @PostMapping public Page<Result> search(@Valid @RequestBody Query query) { return service.search(query); }
}
