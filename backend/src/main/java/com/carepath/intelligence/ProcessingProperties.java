package com.carepath.intelligence;
import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
@ConfigurationProperties("carepath.processing") @Validated
public record ProcessingProperties(@NotBlank String tempRoot,@NotBlank String tesseractCommand,
    @Min(5) @Max(300) int timeoutSeconds,@Min(1) @Max(60) int ocrTimeoutSeconds,
    @Min(1) @Max(200) int maxPages,@Min(1000) @Max(500000) int maxCharacters,boolean workerEnabled) {}
