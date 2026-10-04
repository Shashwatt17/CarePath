package com.carepath.vault;
import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
@ConfigurationProperties("carepath.vault") @Validated
public record VaultProperties(@NotBlank String root, @Min(1) @Max(20971520) int maxBytes) {}
