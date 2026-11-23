package com.carepath.intelligence;

import com.carepath.extraction.*;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;
import static com.carepath.intelligence.ExtractionData.*;

/** Optional second readings use the existing OCR word coordinates and worker raster. */
final class LocalCellCheck {
    private final BufferedImage page;
    private final ValueReReader reader;
    private final long deadline = System.nanoTime() + Duration.ofSeconds(3).toNanos();
    private int attempts;
    LocalCellCheck(BufferedImage page,String command,Path directory) {
        this.page=page;
        reader=new ValueReReader(new TesseractCliOcrEngine(command,Duration.ofMillis(700),directory));
    }
    CellCheck inspect(String row,String word,Rectangle rect,double confidence) {
        String[] cells=row.split("\t",-1);
        if(attempts>=8 || System.nanoTime()>=deadline || cells.length!=4 ||
            !cells[1].equals(word) || !word.matches("[0-9]{1,9}(?:\\.[0-9]{1,8})?") ||
            !cells[0].matches("[\\p{L}][\\p{L} ()/._%-]{1,159}") ||
            !cells[2].matches(".*[\\p{L}%].*") || !cells[3].matches(".*[0-9].*"))return null;
        attempts++;
        var range=new ReferenceRangeParser().parse(cells[3]);
        BigDecimal upper=range.upper();
        if(cells[3].matches("<[0-9]{1,9}(?:\\.[0-9]{1,8})?")) upper=new BigDecimal(cells[3].substring(1));
        var decision=new ValueReviewPolicy().decide(reader.reRead(page,rect,word,confidence,deadline),
            new DecimalPlausibilityChecker().check(word,range.lower(),upper));
        return new CellCheck(new Box(rect.x/(double)page.getWidth(),rect.y/(double)page.getHeight(),
            rect.width/(double)page.getWidth(),rect.height/(double)page.getHeight()),decision.readFromImage(),decision.computedAlternatives());
    }
}
