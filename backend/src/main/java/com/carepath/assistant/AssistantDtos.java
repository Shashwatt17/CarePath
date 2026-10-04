package com.carepath.assistant;
import java.util.*;
import java.time.Instant;
import jakarta.validation.constraints.*;
import com.carepath.longitudinal.HistoryDtos.Point;
public final class AssistantDtos {
 private AssistantDtos() {}
 public record Ask(@NotBlank @Size(max=1000) String question, UUID conceptId, UUID observationId, UUID previousReport, UUID currentReport, boolean useAi) {}
 public record Fact(String id,String kind,String text,List<String> evidenceIds,List<String> approvedPhrasings) {}
 public record Source(String id,Point observation) {}
 public record Suggestion(String text,List<UUID> observationIds) {}
 public record Answer(String mode,String providerStatus,String message,String uncertainty,List<Fact> facts,List<Source> evidence,List<String> explanation,List<Suggestion> suggestedQuestions,List<String> referenceUrls) {}
 public record Context(String intent,List<Fact> facts) {}
 public record Selection(String factId,int phrasing,List<String> evidenceIds) {}
 public record ModelOutput(List<Selection> selections) {}
 public record Save(@NotBlank @Size(max=1000) String text,@NotNull @Size(max=12) List<@NotNull UUID> observationIds) {}
 public record Edit(@NotBlank @Size(max=1000) String text,@Min(0) long version) {}
 public record Saved(UUID id,String text,String sourceType,Instant createdAt,Instant updatedAt,long version,boolean evidenceMissing,List<Point> evidence) {}
}
