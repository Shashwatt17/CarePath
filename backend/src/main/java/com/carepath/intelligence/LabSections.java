package com.carepath.intelligence;
import java.util.Locale;
/** Administrative labels are not measurements, regardless of column spacing. */
final class LabSections {
    static boolean administrative(String text) {
        String first=text.strip().split("\\t|\\|",2)[0].strip().toLowerCase(Locale.ROOT);
        return first.matches("^(?:patient(?: name| id)?|age(?:/gender| / gender)?|gender|sex|order id|barcode|customer since|referred by|ref doctor|sample (?:type|temperature|collected on|received on)|report (?:status|generated on)|method|machine|page|test performed at)(?:\\s*:.*|\\s+[:0-9].*|)$");
    }
}
