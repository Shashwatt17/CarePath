package com.carepath.longitudinal;
import java.math.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
/** Numeric display tolerance, never a clinical significance threshold. */
@Component
public class StabilityPolicy {
 public static final String VERSION="numeric-history-v1";
 private final BigDecimal relative;private final Map<UUID,BigDecimal> overrides=new HashMap<>();
 public StabilityPolicy(@Value("${carepath.history.relative-tolerance:0.01}") BigDecimal relative,@Value("${carepath.history.concept-tolerances:}") String configured) {
  validate(relative);this.relative=relative;
  if(!configured.isBlank()) for(String entry:configured.split(";")) {var pair=entry.split("=",-1);if(pair.length!=2)throw new IllegalArgumentException("Invalid history tolerance configuration");var v=new BigDecimal(pair[1].strip());validate(v);overrides.put(UUID.fromString(pair[0].strip()),v);}
  if(overrides.size()>1000)throw new IllegalArgumentException("Too many history policies");
 }
 private static void validate(BigDecimal v) {if(v.signum()<0 || v.compareTo(new BigDecimal("0.1"))>0)throw new IllegalArgumentException("History relative tolerance must be between 0 and 0.1");}
 public String version(UUID concept){return VERSION+";relative="+overrides.getOrDefault(concept,relative).toPlainString();}
 public String direction(UUID concept,BigDecimal before,BigDecimal after) {
  BigDecimal delta=after.subtract(before),scale=before.abs().max(after.abs());
  return delta.abs().compareTo(scale.multiply(overrides.getOrDefault(concept,relative)))<=0?"APPROXIMATELY_STABLE":delta.signum()>0?"INCREASED":"DECREASED";
 }
}
