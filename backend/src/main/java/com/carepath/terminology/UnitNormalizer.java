package com.carepath.terminology;
import java.math.*;
import java.util.UUID;
import org.springframework.stereotype.Component;
@Component
public class UnitNormalizer {
    public record Normalized(@com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) BigDecimal value,String unit,String status,UUID ruleId,String version) {}
    private final Terminology terminology;
    public UnitNormalizer(Terminology terminology) { this.terminology=terminology; }
    public Normalized normalize(Terminology.Concept concept,BigDecimal value,String unit,String comparator) {
        if(concept==null) return absent("UNKNOWN_CONCEPT");
        if(unit==null || unit.isBlank()) return absent("MISSING_UNIT");
        var rule=terminology.rule(concept.id(),unit.strip());if(rule.isEmpty()) return absent("UNSUPPORTED_UNIT");
        if(value==null) return absent("INVALID_VALUE");
        if(comparator!=null && !comparator.equals("=")) return absent("COMPARATOR_NOT_EXACT");
        var r=rule.get();BigDecimal converted=value.multiply(r.factor());
        // Powers of ten are exact scale changes. Molecular factor is approximate: retain no more
        // significant digits than the source or published four-significant-digit factor, HALF_EVEN.
        if(r.factor().stripTrailingZeros().unscaledValue().abs().compareTo(BigInteger.ONE)!=0)
            converted=converted.round(new MathContext(Math.max(1,Math.min(value.precision(),r.factor().stripTrailingZeros().precision())),RoundingMode.HALF_EVEN));
        return new Normalized(converted,r.target(),r.source().equals(r.target())?"SAME_UNIT":"CONVERTED",r.id(),r.version());
    }
    private Normalized absent(String status) { return new Normalized(null,null,status,null,"carepath-units-v1"); }
}
