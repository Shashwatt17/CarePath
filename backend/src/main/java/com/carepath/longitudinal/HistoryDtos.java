package com.carepath.longitudinal;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonFormat;
public final class HistoryDtos {
 private HistoryDtos() {}
 public record Reading(String text,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal number,String unit,String reference,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal low,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal high,String sourceFlag) {}
 public record Evidence(UUID documentId,UUID candidateId,String filename,int page,String text,int start,int end,String method) {}
 public record Point(UUID id,UUID conceptId,String concept,LocalDate date,String provider,String specimen,String method,String normalizationVersion,String mappingStatus,String unitStatus,String comparator,String verificationStatus,Instant verifiedAt,Reading original,Reading effective,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal normalizedValue,String normalizedUnit,Evidence evidence) {}
 public record Page<T>(List<T> items,long total,int page,int size) {}
 public record Concept(UUID id,String name,long count) {}
 public record Event(String type,UUID id,LocalDate date,String title,Point observation,UUID documentId,Integer sourcePage,String status,Instant occursAt) {
  public Event(String type,UUID id,LocalDate date,String title,Point observation,UUID documentId){this(type,id,date,title,observation,documentId,null,null,null);}
 }
 public record Change(UUID conceptId,String concept,String type,Point previous,Point current,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal previousValue,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal currentValue,String unit,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal absoluteDelta,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal percentageDelta,String referenceTransition,String reason,String explanation,String policyVersion) {}
 public record Series(String unit,List<Point> points,String pattern,String explanation) {}
 public record Trend(List<Series> series,List<String> limitations,boolean truncated,int windowLimit) {}
 public record History(Page<Point> history,Trend trend) {}
 public record Report(UUID id,String filename,String category,String status,LocalDate date,String provider,String panel,boolean fullyReviewed) {}
 public record Comparison(Report previousReport,Report currentReport,List<Change> changes,String scopeNote) {}
}
