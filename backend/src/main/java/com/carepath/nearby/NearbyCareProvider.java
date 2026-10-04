package com.carepath.nearby;
import java.util.List;
import jakarta.validation.constraints.*;
public interface NearbyCareProvider {
 enum Category { HOSPITAL, CLINIC, PHARMACY, DIAGNOSTIC_CENTER }
 record Search(@NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
  @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
  @Min(100) @Max(20000) int radiusMeters, @NotNull Category category,
  @NotNull @AssertTrue Boolean locationConsent) {}
 record Attribution(String name, String url) {}
 record Facility(String providerId, String name, Category category, String address,
  Long distanceMeters, String phone, Boolean openNow, List<String> hours,
  String website, String directions, String bookingUrl, List<Attribution> attributions) {}
 boolean available();
 List<Facility> search(Search search);
}
