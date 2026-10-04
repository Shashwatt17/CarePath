package com.carepath.intelligence;
import java.math.BigDecimal;
import java.util.regex.Pattern;
public class ReferenceRangeParser {
    private static final Pattern RANGE=Pattern.compile("^([+-]?\\d{1,9}(?:\\.\\d{1,8})?)\\s*[-–]\\s*([+-]?\\d{1,9}(?:\\.\\d{1,8})?)$");
    public record Range(BigDecimal lower,BigDecimal upper,String text,boolean valid) {}
    public Range parse(String input) {
        if(input==null || input.isBlank() || input.equals("-")) return new Range(null,null,null,true);
        String text=input.strip();var match=RANGE.matcher(text);
        if(!match.matches()) return new Range(null,null,text,true);
        var low=new BigDecimal(match.group(1));var high=new BigDecimal(match.group(2));
        return low.compareTo(high)<=0?new Range(low,high,text,true):new Range(null,null,text,false);
    }
}
