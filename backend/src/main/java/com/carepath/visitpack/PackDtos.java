package com.carepath.visitpack;

import java.time.*;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public final class PackDtos {
    private PackDtos() {}
    public enum Type { SYMPTOM, OBSERVATION, CHANGE, DOCUMENT, APPOINTMENT, FOLLOW_UP, SAVED_QUESTION, MANUAL_QUESTION }
    public record Selection(@NotNull Type type, UUID sourceId, UUID otherId, boolean includeNotes,
                            @Size(max=1000) String questionText) {}
    public record Input(@NotBlank @Size(max=160) String title, @NotBlank @Size(max=2000) String reasonForVisit,
                        UUID appointmentId, LocalDate packDate, @NotNull @Size(max=40) List<@Valid Selection> items,
                        @Min(0) long version) {}
    public record Generate(@Min(0) long version,@NotBlank @Pattern(regexp="[a-f0-9]{64}") String previewHash) {}
    public record Field(String label,String value) {}
    public record Evidence(UUID documentId,UUID observationId,String filename,int page,String snippet) {}
    public record Item(Type type,String heading,List<Field> fields,List<Evidence> evidence) {}
    public record Content(String title,String reasonForVisit,LocalDate packDate,int revision,List<Item> items) {}
    public record Preview(Content content,String hash) {}
    public record Pack(UUID id,String title,String reasonForVisit,UUID appointmentId,LocalDate packDate,String status,
                       int revision,long version,Instant createdAt,Instant updatedAt,Instant generatedAt,
                       UUID previousPackId,List<Selection> selections,Content snapshot) {}
    public record Summary(UUID id,String title,String status,int revision,long version,Instant createdAt,Instant generatedAt) {}
}
