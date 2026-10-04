package com.carepath.care;
import com.fasterxml.jackson.annotation.JsonFormat;import java.time.*;import java.util.*;import jakarta.validation.constraints.*;
public final class CareDtos {
 private CareDtos(){}
 public enum Frequency { ONCE, OCCASIONAL, DAILY, CONSTANT }
 public enum AppointmentStatus { SCHEDULED, COMPLETED, CANCELLED }
 public record SymptomInput(@NotBlank @Size(max=160) String name,@NotNull Instant startedAt,Instant resolvedAt,@Min(1) @Max(10) int severity,@NotNull Frequency frequency,@Size(max=2000) String notes,@Min(0) long version){}
 public record Symptom(UUID id,String name,Instant startedAt,Instant resolvedAt,int severity,String frequency,String notes,String status,Instant createdAt,Instant updatedAt,long version){}
 public record AppointmentInput(@NotBlank @Size(max=255) String providerName,@Size(max=160) String specialty,@NotNull @JsonFormat(without=JsonFormat.Feature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE) OffsetDateTime startsAt,@NotBlank @Size(max=80) String timeZone,@Size(max=500) String location,@Size(max=40) String phone,@Size(max=500) String externalUrl,@Size(max=2000) String notes,LocalDate followUpDate,@NotNull @Size(max=20) List<@NotNull UUID> documentIds,@NotNull @Size(max=20) List<@NotNull UUID> symptomIds,@NotNull @Size(max=20) List<@NotNull UUID> questionIds,@NotNull @Size(max=5) List<@NotNull Integer> offsets,@Min(0) long version){}
 public record Appointment(UUID id,String providerName,String specialty,Instant startsAt,String timeZone,String location,String phone,String externalUrl,String notes,String status,LocalDate followUpDate,long version,List<UUID> documentIds,List<UUID> symptomIds,List<UUID> questionIds,List<Integer> offsets){}
 public record Action(@NotNull AppointmentStatus status,@Min(0) long version){}
 public record FollowAction(@NotBlank @Pattern(regexp="CONFIRM|IGNORE") String action,@JsonFormat(without=JsonFormat.Feature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE) OffsetDateTime confirmedAt,@Size(max=80) String timeZone,@NotNull @Size(max=5) List<@NotNull Integer> offsets,@Min(0) long version){}
 public record FollowUp(UUID id,UUID documentId,int page,String filename,String instruction,LocalDate anchorDate,Integer duration,String unit,LocalDate suggestedDate,LocalDate confirmedDate,Instant confirmedAt,String timeZone,String confidence,String status,long version){}
 public record Reminder(UUID id,String sourceType,UUID sourceId,Instant scheduledAt,int offsetMinutes,String status,Instant deliveredAt){}
 public record Notice(UUID id,String title,String message,Instant createdAt,Instant readAt,String sourceType,UUID sourceId){}
}
