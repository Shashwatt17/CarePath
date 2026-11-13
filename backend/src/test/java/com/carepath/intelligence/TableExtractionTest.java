package com.carepath.intelligence;
import static com.carepath.intelligence.ExtractionData.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
class TableExtractionTest {
    Result parse(String text){return new ExtractionPipeline(null).parse(List.of(TextPages.nativePage(1,"LAB REPORT\n"+text)));}
    @Test void separateDatedColumnsNeverMergeAndKeepDateEvidence(){
        var r=parse("Date\t18/Jul/2026 12/Sep/2025\tUnit\tReference\nGlucose\t91.3\t104\tmg/dL\t70 - 140");
        assertEquals(2,r.candidates().size());
        assertEquals(List.of("91.3","104"),r.candidates().stream().map(Candidate::originalValue).toList());
        assertEquals(List.of(LocalDate.of(2026,7,18),LocalDate.of(2025,9,12)),r.candidates().stream().map(Candidate::reportDate).toList());
        assertTrue(r.candidates().stream().noneMatch(c->c.reasons().contains("CONFLICTING_VALUES")));
        new ProvenanceValidator().validate(r,20,250000);
    }
    @Test void ambiguousCenturyAndMissingDateColumnRemainUnknown(){
        var r=parse("Date\t18/Jul/2026 12/Sep/25\tUnit\tReference\nGlucose\t91.3\t104\tmg/dL\t70 - 140");
        assertNull(r.candidates().get(1).reportDate());assertTrue(r.candidates().get(1).reasons().contains("TABLE_DATE_UNRESOLVED"));
        var mismatch=parse("Date\t18/Jul/2026 12/Sep/2025\tUnit\tReference\nGlucose\t91.3\tmg/dL\t70 - 140");
        assertNull(mismatch.candidates().getFirst().reportDate());
    }
    @Test void tsvColumnGapsAndDecimalArePreserved(){
        String tsv="5\t1\t1\t1\t1\t1\t10\t20\t40\t10\t95\tIron\n"+
        "5\t1\t1\t1\t1\t2\t100\t20\t30\t10\t90\t28.6\n"+
        "5\t1\t1\t1\t1\t3\t210\t20\t30\t10\t70\tug/dL\n"+
        "5\t1\t1\t1\t1\t4\t280\t20\t40\t10\t90\t37-145\n";
        var page=TesseractOcr.parse(tsv,1,400,100);
        assertEquals("Iron\t28.6\tug/dL\t37-145\n",page.text());
        var candidates=new LabObservationExtractor().extract(List.of(page),new Information(null,null,List.of()));
        assertEquals(new BigDecimal("28.6"),candidates.getFirst().numericValue());assertEquals(Band.LOW,candidates.getFirst().confidence());
    }
    @Test void columnMajorOcrJoinsCellsWithoutMergingMethodLabel(){
        String tsv="5\t1\t1\t1\t1\t1\t10\t20\t40\t12\t95\tIron\n"+
        "5\t1\t1\t1\t2\t1\t10\t34\t65\t8\t90\tCalculated\n"+
        "5\t1\t2\t1\t1\t1\t100\t21\t30\t12\t90\t28.6\n"+
        "5\t1\t3\t1\t1\t1\t210\t21\t35\t12\t90\tug/dL\n"+
        "5\t1\t4\t1\t1\t1\t280\t21\t50\t12\t90\t37-145\n";
        var page=TesseractOcr.parse(tsv,1,400,100);
        assertEquals("Iron\t28.6\tug/dL\t37-145\nCalculated\n",page.text());
    }
    @Test void digitContaminatedUnitCannotBecomeReliableNumber(){
        var r=parse("Glucose 934 4o2 mgid) 70-140");
        assertNull(r.candidates().getFirst().numericValue());
    }

    @Test void realOcrKeepsSyntheticHistoricalColumnsSeparate(@org.junit.jupiter.api.io.TempDir java.nio.file.Path directory) throws Exception {
        var image=new java.awt.image.BufferedImage(900,1600,java.awt.image.BufferedImage.TYPE_INT_RGB);
        var g=image.createGraphics();g.setColor(java.awt.Color.WHITE);g.fillRect(0,0,900,1600);g.setColor(java.awt.Color.BLACK);g.setFont(new java.awt.Font("SansSerif",java.awt.Font.PLAIN,18));
        g.drawString("LAB REPORT",30,40);g.drawString("Laboratory: Example Lab",30,80);
        g.drawString("Date",30,140);g.drawString("18/Jul/2026",300,140);g.drawString("12/Sep/2025",450,140);g.drawString("Unit",640,140);g.drawString("Reference",760,140);
        g.drawString("Glucose",30,190);g.drawString("91.3",300,190);g.drawString("104",450,190);g.drawString("mg/dL",640,190);g.drawString("70 - 140",760,190);g.dispose();
        var path=directory.resolve("synthetic.png");javax.imageio.ImageIO.write(image,"png",path.toFile());image.flush();
        var pipeline=new ExtractionPipeline(new DocumentTextExtractor(new TesseractOcr("tesseract",30),20,250000));
        var result=pipeline.extract(path,"image/png",directory);
        assertEquals(List.of("91.3","104"),result.candidates().stream().map(Candidate::originalValue).toList());
        assertTrue(result.candidates().stream().allMatch(c->c.confidence()==Band.LOW));
        new ProvenanceValidator().validate(result,20,250000);
    }
    @Test void damagedReferenceIsPreservedWithoutInventingAnInterval(){
        var result=parse("Iron\t28.6\tug/dL\t37 . 145");
        assertEquals("37 . 145",result.candidates().getFirst().referenceText());
        assertNull(result.candidates().getFirst().referenceLower());
        assertTrue(result.candidates().getFirst().reasons().contains("REFERENCE_RANGE_UNREADABLE"));
    }
    @Test void spacedFooterIsNotAReading(){
        assertTrue(parse("Test performed at\t055 Example Centre\tPage 1 of 9").candidates().isEmpty());
    }
    @Test void metadataDatesAndMissingPagesAreExplicit(){
        var r=parse("Reporting Date/Time 18/Jul/2026 08:30PM\nExample Hospital, Example City\nPage 1 of 9\nIron\t28.6\tug/dL\t37-145");
        assertEquals("2026-07-18",r.information().date().value());
        assertEquals("Example Hospital, Example City",r.information().provider().value());
        assertTrue(r.information().warnings().contains("POSSIBLE_MISSING_PAGES"));
    }
}
