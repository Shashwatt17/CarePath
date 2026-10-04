package com.carepath.assistant;
import static com.carepath.assistant.AssistantDtos.*;
public interface HealthExplanationProvider {
 boolean available();
 ModelOutput explain(Context context) throws ProviderFailure;
 final class ProviderFailure extends RuntimeException {
  private final String code;
  public ProviderFailure(String code){super("Explanation provider unavailable");this.code=code;}
  public String code(){return code;}
 }
}
