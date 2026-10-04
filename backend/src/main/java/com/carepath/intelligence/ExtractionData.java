package com.carepath.intelligence;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
/** Candidate-only transport between isolated workers, persistence and owner-scoped APIs. */
public final class ExtractionData {
    private ExtractionData() {}
    public enum Band { HIGH, MEDIUM, LOW }
    public enum Method { PDFBOX_TEXT, TESSERACT_OCR }
    public record Box(double x,double y,double width,double height) {}
    public record Line(int start,int end,Double ocrConfidence,Box box) {}
    public record Page(int number,String text,Method method,Double ocrConfidence,List<Line> lines) {}
    public record Source(int page,int start,int end,String text,Method method,Box box) {}
    public record Classification(String category,Band confidence,String method,List<Source> evidence) {}
    public record Fact(String value,Source source) {}
    public record Information(Fact date,Fact provider,List<String> warnings) {}
    public record Candidate(UUID id,String originalTestName,String originalValue,BigDecimal numericValue,
        String comparator,String originalUnit,BigDecimal referenceLower,BigDecimal referenceUpper,String referenceText,
        String abnormalFlag,LocalDate reportDate,String providerName,Band confidence,List<String> reasons,Source source) {}
    public record Result(List<Page> pages,Classification classification,Information information,List<Candidate> candidates,
        boolean needsReview,boolean ocrUsed,String pipelineVersion) {}
    public record WorkerReply(Result result,String errorCode,boolean retryable) {}
    public record Status(String state,String jobState,int attempts,String errorCode,boolean retryAllowed) {}
    public record View(UUID id,UUID documentId,int revision,Result result) {}
}
