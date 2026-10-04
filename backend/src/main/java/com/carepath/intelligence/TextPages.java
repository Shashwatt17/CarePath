package com.carepath.intelligence;
import static com.carepath.intelligence.ExtractionData.*;
import java.util.*;
public final class TextPages {
    private TextPages() {}
    public static Page nativePage(int number,String input) {
        String text=input.replace("\r\n","\n").replace('\r','\n').replace("\u0000","");
        List<Line> lines=new ArrayList<>();int start=0;
        for(String line:text.split("\n",-1)) { lines.add(new Line(start,start+line.length(),null,null));start+=line.length()+1; }
        return new Page(number,text,Method.PDFBOX_TEXT,null,lines);
    }
    public static boolean requiresOcr(String text) {
        // A numeric-heavy text report is still text. Require both useful letters and readable characters.
        return text.codePoints().filter(Character::isLetterOrDigit).count()<40 || text.codePoints().filter(Character::isLetter).count()<15;
    }
    public static Source source(Page page,Line line) { return new Source(page.number(),line.start(),line.end(),page.text().substring(line.start(),line.end()),page.method(),line.box()); }
}
