package com.carepath.sharing;

import java.time.*;
import java.util.*;
import jakarta.validation.constraints.*;
import com.carepath.visitpack.PackDtos;

public final class ShareDtos {
    private ShareDtos() {}
    public record Create(@NotNull UUID packId, int expiryMinutes) {}
    public record Access(@NotNull @Size(max=100) String token) {}
    public record Summary(UUID id, UUID packId, int revision, Instant createdAt, Instant expiresAt,
                          Instant revokedAt, String status, long version) {}
    // Raw capability is returned only once at creation, never by management/list APIs.
    public record Created(Summary share, String token) {}
    public record Citation(String filename, int page, String snippet) {}
    public record Item(PackDtos.Type type, String heading, List<PackDtos.Field> fields, List<Citation> evidence) {}
    // Explicit anonymous projection: no source IDs, owner IDs, selections or navigation URLs.
    public record Snapshot(String title, String reasonForVisit, LocalDate packDate, int revision,
                           Instant generatedAt, Instant expiresAt, List<Item> items) {}
}
