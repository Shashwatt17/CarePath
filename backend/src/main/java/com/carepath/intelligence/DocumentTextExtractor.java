package com.carepath.intelligence;
import static com.carepath.intelligence.ExtractionData.*;
import com.carepath.vault.FileValidation;
import java.nio.file.*;
import java.util.*;
import java.io.*;
import javax.imageio.ImageIO;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.rendering.*;
/** Executed only in the bounded child JVM in production. */
public class DocumentTextExtractor {
    private final OcrProvider ocr;private final int maxPages,maxCharacters;
    public DocumentTextExtractor(OcrProvider ocr,int maxPages,int maxCharacters) { this.ocr=ocr;this.maxPages=maxPages;this.maxCharacters=maxCharacters; }
    public List<Page> extract(Path input,String mime,Path temporaryDirectory) {
        List<Page> pages=new ArrayList<>();int characters=0;
        try {
            if(Files.size(input)>20971520) throw new ProcessingFailure("RESOURCE_LIMIT",false);
            if(mime.equals("application/pdf")) {
                try(var pdf=FileValidation.openPdf(Files.readAllBytes(input))) {
                    if(pdf.isEncrypted() || pdf.getNumberOfPages()<1 || pdf.getNumberOfPages()>maxPages) throw new ProcessingFailure("RESOURCE_LIMIT",false);
                    var stripper=new PDFTextStripper();stripper.setSortByPosition(true);stripper.setLineSeparator("\n");
                    for(int i=0;i<pdf.getNumberOfPages();i++) {
                        stripper.setStartPage(i+1);stripper.setEndPage(i+1);String text=stripper.getText(pdf);
                        Page result=TextPages.nativePage(i+1,text);
                        if(TextPages.requiresOcr(text)) {
                            var box=pdf.getPage(i).getMediaBox();double area=(double)box.getWidth()*box.getHeight();
                            if(!Double.isFinite(area) || area<=0 || box.getWidth()>3000 || box.getHeight()>3000) throw new ProcessingFailure("RESOURCE_LIMIT",false);
                            float scale=(float)Math.min(150.0/72,Math.sqrt(4000000/area));
                            var renderer=new PDFRenderer(pdf);renderer.setSubsamplingAllowed(true);
                            var image=renderer.renderImage(i,scale,ImageType.RGB);Path path=temporaryDirectory.resolve("page-"+i+".png");
                            try { ImageIO.write(image,"png",path.toFile());result=ocr.recognize(path,i+1,image.getWidth(),image.getHeight()); }
                            finally { image.flush();Files.deleteIfExists(path); }
                        }
                        characters+=result.text().length();if(result.text().length()>50000 || characters>maxCharacters) throw new ProcessingFailure("RESOURCE_LIMIT",false);
                        pages.add(result);
                    }
                }
            } else if(Set.of("image/png","image/jpeg").contains(mime)) {
                try(var stream=ImageIO.createImageInputStream(input.toFile())) {
                    var readers=ImageIO.getImageReaders(stream);if(!readers.hasNext()) throw new ProcessingFailure("INVALID_DOCUMENT",false);
                    var reader=readers.next();try { reader.setInput(stream);int width=reader.getWidth(0),height=reader.getHeight(0);
                        if(width<=0 || height<=0 || (long)width*height>20000000) throw new ProcessingFailure("RESOURCE_LIMIT",false);
                        // Decode here too, so corrupt originals never go straight to a native OCR process.
                        var image=reader.read(0);if(image==null) throw new ProcessingFailure("INVALID_DOCUMENT",false);image.flush();
                        Page result=ocr.recognize(input,1,width,height);if(result.text().length()>Math.min(maxCharacters,50000)) throw new ProcessingFailure("RESOURCE_LIMIT",false);pages.add(result);
                    } finally { reader.dispose(); }
                }
            } else throw new ProcessingFailure("INVALID_DOCUMENT",false);
            if(pages.stream().allMatch(p->p.text().isBlank())) throw new ProcessingFailure("NO_TEXT",false);
            return pages;
        } catch(IOException | IllegalArgumentException e) { throw new ProcessingFailure("INVALID_DOCUMENT",false); }
    }
}
