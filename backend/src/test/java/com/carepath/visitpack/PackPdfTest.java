package com.carepath.visitpack;
import static com.carepath.visitpack.PackDtos.*;
import java.util.*;import java.time.*;import java.nio.file.*;
import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;
import org.apache.pdfbox.Loader;import org.apache.pdfbox.text.PDFTextStripper;
class PackPdfTest {
 @Test void unicodeAndMarkupArePlainData()throws Exception {
  var c=new Content("SYNTHETIC DEMO DATA - NOT A REAL PATIENT","Café • μg/L — <script>alert(1)</script> हिन्दी",null,1,List.of());
  byte[] bytes=new PackPdf().render(c,Instant.EPOCH);assertEquals("%PDF-",new String(bytes,0,5,java.nio.charset.StandardCharsets.US_ASCII));
  try(var d=Loader.loadPDF(bytes)){String t=new PDFTextStripper().getText(d);assertTrue(t.contains("Café"));assertTrue(t.contains("μg/L"));assertTrue(t.contains("<script>"));assertTrue(t.contains("[U+"));assertNull(d.getDocumentCatalog().getOpenAction());assertNull(d.getDocumentCatalog().getNames());assertEquals("CarePath Doctor Visit Pack",d.getDocumentInformation().getTitle());}
 }
 @Test void longUnbrokenTextAndMultiplePages()throws Exception {
  var items=new ArrayList<Item>();for(int i=0;i<12;i++)items.add(new Item(Type.MANUAL_QUESTION,"Question "+(i+1),List.of(new Field("User draft","LongWord".repeat(100)+" Finishes here.")),List.of()));
  byte[] bytes=new PackPdf().render(new Content("SYNTHETIC DEMO DATA - NOT A REAL PATIENT","Layout stress fixture",null,1,items),Instant.EPOCH);
  try(var d=Loader.loadPDF(bytes)){assertTrue(d.getNumberOfPages()>3);String text=new PDFTextStripper().getText(d);assertTrue(text.contains("Question 12"));assertTrue(text.contains("Page 1 of "+d.getNumberOfPages()));assertEquals(12,text.split("Finishes here",-1).length-1);}
  Files.createDirectories(Path.of("target/phase9-pdf"));Files.write(Path.of("target/phase9-pdf/multipage.pdf"),bytes);
 }
 @Test void pdfPageLimitFailsSafely(){var items=new ArrayList<Item>();for(int i=0;i<500;i++)items.add(new Item(Type.MANUAL_QUESTION,"Question",List.of(new Field("User draft","Large note ".repeat(100))),List.of()));assertThrows(com.carepath.foundation.ApiFailure.class,()->new PackPdf().render(new Content("Synthetic","Resource bounds",null,1,items),Instant.EPOCH));}
}
