package com.carepath.review;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import com.carepath.intelligence.ExtractionData;
import com.carepath.terminology.*;
public final class ReviewDtos {
    private ReviewDtos() {}
    public record Fields(@NotBlank @Size(max=160) String testName,@NotBlank @Size(max=80) String value,
        @Size(max=50) String unit,@Size(max=255) String referenceRange,LocalDate date,UUID conceptId) {}
    public record Confirm(@Min(0) long version) {}
    public record Correct(@Min(0) long version,@jakarta.validation.Valid @NotNull Fields fields,
        @AssertTrue boolean sourceReviewed,@NotBlank @Size(min=10,max=500) String reason) {}
    public record Reject(@Min(0) long version,@Size(max=500) String reason) {}
    public record Preview(Terminology.Mapping mapping,UnitNormalizer.Normalized normalized,String comparator,
        BigDecimal number,BigDecimal referenceLow,BigDecimal referenceHigh,String derivedRangeStatus,List<String> warnings,boolean canVerify) {}
    public record Decision(boolean canConfirm,List<String> reasons,Preview preview) {}
    public record CandidateView(UUID id,UUID documentId,String filename,String state,long version,ExtractionData.Candidate extracted,Decision decision,UUID observationId) {}
    public record Queue(List<CandidateView> items,long total,int page,int size) {}
    public record Observation(UUID id,UUID documentId,UUID candidateId,String verificationStatus,Instant verifiedAt,
        ExtractionData.Candidate original,Fields verified,Preview normalization,ExtractionData.Source source,String correctionReason) {}
    public record ActionResult(CandidateView candidate,Observation observation) {}
}
