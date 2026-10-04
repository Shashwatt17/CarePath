package com.carepath.visitpack;

import static com.carepath.visitpack.PackDtos.*;
import java.io.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.Semaphore;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.springframework.stereotype.Component;
import com.carepath.foundation.ApiFailure;

/** Text-only local renderer. No HTML parser, file references, external URLs, scripts or attachments. */
@Component
public class PackPdf {
    public static final String NOTICE="CarePath organizes user-provided records and does not provide a medical diagnosis.";
    private final Semaphore slots=new Semaphore(2);
    public byte[] render(Content content,Instant prepared) {
        if(!slots.tryAcquire())throw new ApiFailure(429,"PACK_PDF_BUSY","PDF generation is busy. Please retry.");
        try(var document=new PDDocument();var output=new ByteArrayOutputStream()) {
            var info=new PDDocumentInformation();info.setTitle("CarePath Doctor Visit Pack");info.setCreator("CarePath");document.setDocumentInformation(info);
            try(var regular=PackPdf.class.getResourceAsStream("/fonts/DejaVuSans.ttf");var bold=PackPdf.class.getResourceAsStream("/fonts/DejaVuSans-Bold.ttf")) {
                var writer=new Writer(document,PDType0Font.load(document,regular),PDType0Font.load(document,bold));
                writer.paragraph("DOCTOR VISIT PACK",20,true,8);
                writer.paragraph(content.title(),13,true,5);
                writer.paragraph("Prepared: "+prepared.atOffset(ZoneOffset.UTC)+" | Revision "+content.revision(),9,false,4);
                if(content.packDate()!=null)writer.paragraph("Visit context date: "+content.packDate(),9,false,4);
                writer.paragraph("Historical snapshot of deliberately selected information. Source records may later change.",9,false,12);
                writer.section("REASON FOR VISIT");writer.paragraph(content.reasonForVisit(),10,false,12);
                for(int i=0;i<content.items().size();i++) {
                    var item=content.items().get(i);writer.section(section(item.type())+" | "+item.heading());
                    for(int j=0;j<item.fields().size();j++) {var f=item.fields().get(j);writer.paragraph(f.label()+": "+f.value(),10,false,4);}
                    for(int j=0;j<item.evidence().size();j++) {
                        var e=item.evidence().get(j);writer.paragraph("Source: "+e.filename()+" - Page "+e.page(),9,true,3);
                        writer.paragraph("Evidence: "+e.snippet(),9,false,4);
                    }
                    writer.space(9);
                }
                writer.section("IMPORTANT");writer.paragraph(NOTICE,9,false,4);
                writer.paragraph("Symptoms and questions are user-reported. Numerical changes do not establish clinical significance. Originals are not embedded.",9,false,4);
                if(writer.escaped)writer.paragraph("Unsupported glyphs are preserved as [U+codepoint] text. Consult the source for original script.",8,false,4);
                writer.finish();
            }
            document.save(output);if(output.size()>5*1024*1024)throw limit();return output.toByteArray();
        } catch(IOException e){throw new ApiFailure(503,"PACK_PDF_UNAVAILABLE","The PDF could not be generated. Please retry.");}
        finally {slots.release();}
    }
    static ApiFailure limit(){return new ApiFailure(422,"PACK_PDF_LIMIT","Reduce selected items or long notes before generating.");}
    public static String section(Type type) {
        return switch(type) {case SYMPTOM->"USER-REPORTED SYMPTOMS";case OBSERVATION->"VERIFIED RECORD SUMMARY";case CHANGE->"OBSERVED CHANGES";case DOCUMENT->"SELECTED DOCUMENTS";case APPOINTMENT->"APPOINTMENT CONTEXT";case FOLLOW_UP->"CONFIRMED FOLLOW-UPS";case SAVED_QUESTION,MANUAL_QUESTION->"QUESTIONS FOR CLINICIAN";};
    }
    private static final class Writer {
        private final PDDocument doc;private final PDFont regular,bold;private PDPageContentStream stream;
        private final long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(8);
        private float y;private boolean escaped;
        private static final float LEFT=48,WIDTH=499,MIN_Y=65;
        Writer(PDDocument d,PDFont r,PDFont b)throws IOException {doc=d;regular=r;bold=b;page();}
        void check(){if(System.nanoTime()>deadline||doc.getNumberOfPages()>40)throw limit();}
        void page()throws IOException {
            if(stream!=null)stream.close();check();if(doc.getNumberOfPages()>=40)throw limit();
            var page=new PDPage(PDRectangle.A4);doc.addPage(page);stream=new PDPageContentStream(doc,page);y=775;
            draw("CAREPATH",LEFT,809,9,bold);stream.setStrokingColor(0.75f);stream.moveTo(LEFT,798);stream.lineTo(547,798);stream.stroke();
        }
        void ensure(float required)throws IOException{check();if(y-required<MIN_Y)page();}
        void space(float size)throws IOException{ensure(size);y-=size;}
        void section(String text)throws IOException{ensure(62);paragraph(text,11,true,7);}
        String safe(String text,PDFont font)throws IOException {
            StringBuilder result=new StringBuilder();
            for(int offset=0;offset<text.length();) {
                int cp=text.codePointAt(offset);offset+=Character.charCount(cp);
                if(cp=='\n'||cp=='\r'||cp=='\t'){result.append(' ');continue;}
                if(Character.isISOControl(cp)||Character.getType(cp)==Character.FORMAT){escaped=true;result.append(String.format("[U+%04X]",cp));continue;}
                String ch=new String(Character.toChars(cp));
                try{font.encode(ch);result.append(ch);}catch(IllegalArgumentException e){escaped=true;result.append(String.format("[U+%04X]",cp));}
            }
            return result.toString();
        }
        void paragraph(String text,int size,boolean strong,int after)throws IOException {
            PDFont font=strong?bold:regular;String safe=safe(text,font);StringBuilder line=new StringBuilder();
            // Measure glyphs directly. Long unbroken filenames/notes wrap without clipping.
            for(int i=0;i<safe.length();) {
                check();int cp=safe.codePointAt(i);i+=Character.charCount(cp);String ch=new String(Character.toChars(cp));
                if(font.getStringWidth(line.toString()+ch)/1000*size>WIDTH&&!line.isEmpty()) {
                    int split=line.lastIndexOf(" ");
                    if(split>line.length()/2) {String remainder=line.substring(split+1);line.setLength(split);emit(line.toString(),size,font);line=new StringBuilder(remainder);}
                    else {emit(line.toString(),size,font);line.setLength(0);}
                }
                line.append(ch);
            }
            if(!line.isEmpty())emit(line.toString(),size,font);y-=after;
        }
        void emit(String line,int size,PDFont font)throws IOException{ensure(size+5);draw(line.strip(),LEFT,y,size,font);y-=size+5;}
        void draw(String text,float x,float y,int size,PDFont font)throws IOException{stream.setNonStrokingColor(0.13f);stream.beginText();stream.setFont(font,size);stream.newLineAtOffset(x,y);stream.showText(text);stream.endText();}
        void finish()throws IOException {
            stream.close();int total=doc.getNumberOfPages();
            for(int i=0;i<total;i++) {
                stream=new PDPageContentStream(doc,doc.getPage(i),PDPageContentStream.AppendMode.APPEND,true,true);
                draw("Private - selected records | Page "+(i+1)+" of "+total,LEFT,35,8,regular);stream.close();
            }
        }
    }
}
