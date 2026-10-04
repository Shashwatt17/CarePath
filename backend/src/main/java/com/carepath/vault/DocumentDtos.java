package com.carepath.vault;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
public final class DocumentDtos {
    private DocumentDtos() {}
    public enum Category { LAB_REPORT,PRESCRIPTION,DIAGNOSTIC_REPORT,DISCHARGE_SUMMARY,VACCINATION_RECORD,DOCTOR_NOTE,REFERRAL,MEDICAL_BILL,OTHER }
    public enum State { UPLOADED,PROCESSING,NEEDS_REVIEW,COMPLETED,FAILED }
    public record Metadata(@NotNull Category documentType, LocalDate documentDate,
        @Size(max=255) String providerName, @NotNull @Size(max=12) List<@NotBlank @Size(max=40) String> tags) {}
    public record Update(@NotNull @Min(0) Long version, @NotNull @jakarta.validation.Valid Metadata metadata) {}
    public record View(UUID id,String originalFilename,Category documentType,LocalDate documentDate,
        String providerName,String mimeType,long byteSize,String sha256,State status,Instant uploadedAt,
        Instant createdAt,Instant updatedAt,long version,int pageCount,List<String> tags) {}
    public record Page(List<View> items,long total,int page,int size) {}
    public record Filters(Category type,State status,String filename,String provider,LocalDate from,LocalDate to,
        LocalDate uploadedFrom,LocalDate uploadedTo,int page,int size) {}
}
