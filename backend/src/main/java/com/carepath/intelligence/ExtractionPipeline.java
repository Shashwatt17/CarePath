package com.carepath.intelligence;
import static com.carepath.intelligence.ExtractionData.*;
import java.nio.file.Path;
import java.util.*;
public class ExtractionPipeline {
    public static final String VERSION="deterministic-candidates-v1";
    private final DocumentTextExtractor text;
    public ExtractionPipeline(DocumentTextExtractor text) { this.text=text; }
    public Result extract(Path input,String mime,Path directory) {
        var pages=text.extract(input,mime,directory);return parse(pages);
    }
    public Result parse(List<Page> pages) {
        var classification=new DocumentClassifier().classify(pages);var information=new DocumentInformationExtractor().extract(pages);
        var candidates=classification.category().equals("LAB_REPORT")?new LabObservationExtractor().extract(pages,information):List.<Candidate>of();
        boolean ocr=pages.stream().anyMatch(p->p.method()==Method.TESSERACT_OCR);
        boolean review=classification.confidence()!=Band.HIGH || ocr || !information.warnings().isEmpty() || candidates.stream().anyMatch(c->c.confidence()!=Band.HIGH) || (classification.category().equals("LAB_REPORT") && candidates.isEmpty());
        return new Result(pages,classification,information,candidates,review,ocr,VERSION);
    }
}
