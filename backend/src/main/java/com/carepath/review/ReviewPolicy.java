package com.carepath.review;
import static com.carepath.review.ReviewDtos.*;
import com.carepath.intelligence.*;
import com.carepath.terminology.*;
import com.carepath.foundation.ApiFailure;
import java.util.*;
import java.math.*;
import org.springframework.stereotype.Component;
@Component
public class ReviewPolicy {
    private final Terminology terms;private final UnitNormalizer units;
    public ReviewPolicy(Terminology terms,UnitNormalizer units) { this.terms=terms;this.units=units; }
    public Fields original(ExtractionData.Candidate c) { return new Fields(c.originalTestName(),c.originalValue(),c.originalUnit(),c.referenceText(),c.reportDate(),null); }
    public Preview preview(Fields f,boolean selectionAllowed) {
        var mapping=terms.normalize(f.testName());
        if(f.conceptId()!=null) {
            if(!selectionAllowed) throw invalid();
            var concept=terms.byId(f.conceptId()).orElseThrow(ReviewPolicy::invalid);
            mapping=new Terminology.Mapping(f.testName(),"USER_SELECTED",concept,List.of(),"HUMAN_SELECTION_V1");
        }
        List<String> warnings=new ArrayList<>();BigDecimal number=null;String comparator=null;
        var match=java.util.regex.Pattern.compile("^([<>]=?|[≤≥]|=)?([+-]?\\d{1,9}(?:\\.\\d{1,8})?)$").matcher(f.value()==null?"":f.value().strip());
        if(match.matches()) {number=new BigDecimal(match.group(2));comparator=match.group(1);if("≤".equals(comparator)) comparator="<=";if("≥".equals(comparator)) comparator=">=";}
        else warnings.add("NUMERIC_VALUE_REQUIRED");
        if(number!=null && number.signum()<0) warnings.add("NEGATIVE_LAB_VALUE");
        var range=new ReferenceRangeParser().parse(f.referenceRange());
        if(!range.valid()) warnings.add("INVALID_REFERENCE_RANGE");
        if(mapping.concept()==null) warnings.add(mapping.status());
        var normalized=units.normalize(mapping.concept(),number,f.unit(),comparator);
        if(!Set.of("SAME_UNIT","CONVERTED").contains(normalized.status())) warnings.add(normalized.status());
        if(f.date()==null) warnings.add("DATE_NOT_AVAILABLE");
        String derived="NOT_COMPARABLE";
        if(number!=null && (comparator==null || comparator.equals("=")) && normalized.value()!=null && range.lower()!=null && range.upper()!=null && range.valid())
            derived=number.compareTo(range.lower())<0?"BELOW":number.compareTo(range.upper())>0?"ABOVE":"WITHIN";
        boolean valid=number!=null && number.signum()>=0 && range.valid() && !mapping.status().equals("AMBIGUOUS")
            && (mapping.concept()==null || !Set.of("MISSING_UNIT","UNSUPPORTED_UNIT").contains(normalized.status()));
        return new Preview(mapping,normalized,comparator,number,range.lower(),range.upper(),derived,List.copyOf(warnings),valid);
    }
    public Decision decision(ExtractionData.Candidate c) {
        Preview p=preview(original(c),false);List<String> reasons=new ArrayList<>(p.warnings());
        if(c.confidence()!=ExtractionData.Band.HIGH || c.source().method()==ExtractionData.Method.TESSERACT_OCR) reasons.add("SOURCE_REVIEW_REQUIRED");
        reasons.addAll(c.reasons());
        boolean eligible=p.canVerify() && p.mapping().concept()!=null && p.normalized().value()!=null && reasons.isEmpty();
        return new Decision(eligible,reasons.stream().distinct().toList(),p);
    }
    public static ApiFailure invalid() { return new ApiFailure(422,"REVIEW_VALIDATION","Check the value, concept, unit and supplied reference range."); }
}
