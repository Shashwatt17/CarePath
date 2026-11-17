package com.carepath.vision;
import com.carepath.intelligence.ExtractionData.Box;
import java.util.*;
/** Model proposals never become extraction candidates or verified observations. */
public final class VisionData {
    private VisionData() {}
    public record Reading(String testName,String value,String unit,String referenceRange,String date,String column,Box box) {}
    public record Output(List<Reading> readings,boolean unreadable) {}
    public record Checked(Reading reading,List<String> warnings) {}
    public record Result(List<Checked> readings,boolean unreadable,String model,String policyVersion) {}
    public record PageRoute(int page,List<String> reasons) {}
    public record Status(boolean available,String provider,String model,String consentVersion,List<PageRoute> pages) {}
    public record Request(UUID extractionId,int page,boolean consent,String consentVersion) {}
    public record View(UUID extractionId,int page,String state,Result result) {}
    public static final String POLICY="vision-review-v1";
    public static final String CONSENT="external-page-v1";
}
