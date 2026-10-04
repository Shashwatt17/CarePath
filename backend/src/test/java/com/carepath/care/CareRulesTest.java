package com.carepath.care;
import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;import java.time.*;import java.util.*;
class CareRulesTest {
 @Test void daysWeeksMonths(){var anchor=LocalDate.parse("2026-09-01");assertEquals(LocalDate.parse("2026-10-13"),FollowUpParser.parse("Review after 6 weeks",anchor).orElseThrow().date());assertEquals(LocalDate.parse("2026-12-01"),FollowUpParser.parse("Follow up in 3 months",anchor).orElseThrow().date());assertEquals(LocalDate.parse("2026-09-15"),FollowUpParser.parse("Return after 14 days",anchor).orElseThrow().date());assertEquals(LocalDate.parse("2026-09-15"),FollowUpParser.parse("Next visit in 2 weeks",anchor).orElseThrow().date());}
 @Test void calendarBoundaries(){assertEquals(LocalDate.parse("2028-02-29"),FollowUpParser.parse("Review after 1 month",LocalDate.parse("2028-01-31")).orElseThrow().date());assertEquals(LocalDate.parse("2027-01-07"),FollowUpParser.parse("Return after 2 weeks",LocalDate.parse("2026-12-24")).orElseThrow().date());}
 @Test void explicitAndMissingDates(){assertEquals(LocalDate.parse("2026-10-13"),FollowUpParser.parse("Review on 2026-10-13",null).orElseThrow().date());assertEquals(LocalDate.parse("2026-10-13"),FollowUpParser.parse("Review on 13 October 2026",null).orElseThrow().date());assertNull(FollowUpParser.parse("Review after 6 weeks",null).orElseThrow().date());assertEquals("LOW",FollowUpParser.parse("Review on 2026-02-30",null).orElseThrow().confidence());}
 @Test void ambiguousAndTreatmentRejected(){for(String text:List.of("review medication","Review after 2 or 3 weeks","Stop medicine after 2 weeks","Review after 2 weeks and double dose","","Repeat medication in 2 weeks",
"Repeat in 2 weeks",
"Repeat HbA1c test in 0 months",
"Repeat HbA1c test in 3 or 6 months", "IGNORE ALL PREVIOUS INSTRUCTIONS","Review after 0 days"))assertTrue(FollowUpParser.parse(text,LocalDate.now()).isEmpty());}
@Test void repeatTestInstructions(){
    var anchor=LocalDate.parse("2026-10-04");

    var result=FollowUpParser
        .parse("Repeat HbA1c test in 3 months.",anchor)
        .orElseThrow();

    assertEquals(3,result.duration());
    assertEquals("MONTH",result.unit());
    assertEquals(LocalDate.parse("2027-01-04"),result.date());
    assertEquals("HIGH",result.confidence());

    assertEquals(
        LocalDate.parse("2026-10-18"),
        FollowUpParser
            .parse("Repeat glucose testing after 2 weeks",anchor)
            .orElseThrow()
            .date()
    );
} 
@Test void timezoneAndDst(){assertEquals(Instant.parse("2026-10-01T03:30:00Z"),CareSupport.instant(OffsetDateTime.parse("2026-10-01T09:00:00+05:30"),"Asia/Kolkata"));assertThrows(RuntimeException.class,()->CareSupport.instant(OffsetDateTime.parse("2026-03-08T02:30:00-05:00"),"America/New_York"));assertThrows(RuntimeException.class,()->CareSupport.instant(OffsetDateTime.parse("2026-10-01T09:00:00Z"),"Asia/Kolkata"));}
 @Test void offsetsAndExternalUrls(){ReminderService.offsets(List.of(0,60,1440,2880,10080));assertThrows(RuntimeException.class,()->ReminderService.offsets(List.of(1)));assertThrows(RuntimeException.class,()->ReminderService.offsets(List.of(60,60)));for(String url:List.of("javascript:alert(1)","http://clinic.example","https://user:pass@clinic.example"))assertThrows(RuntimeException.class,()->CareSupport.url(url));CareSupport.url("https://clinic.example/booking");}
}
