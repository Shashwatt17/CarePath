package com.carepath.intelligence;

import static org.junit.jupiter.api.Assertions.*;
import static com.carepath.intelligence.ExtractionData.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class DatedComparisonTest {
    List<Candidate> read(String dates) {
        var page=TextPages.nativePage(1,"Test name|"+dates+"|Unit\nRandom glucose|93.1|102|mg/dL");
        return new LabObservationExtractor().extract(List.of(page),new Information(null,null,List.of()));
    }
    @Test void datesFollowPrintedColumnOrder() {
        var rows=read("15/Jan/2026|12/Mar/2026");
        assertEquals(List.of("93.1","102"),rows.stream().map(Candidate::originalValue).toList());
        assertEquals(List.of("2026-01-15","2026-03-12"),rows.stream().map(c->c.reportDate().toString()).toList());
    }
    @Test void damagedDateStaysUnknown() {
        var rows=read("12/Mar/2026|154 an2026");
        assertEquals(2,rows.size());assertNotNull(rows.getFirst().reportDate());assertNull(rows.getLast().reportDate());
    }
}
