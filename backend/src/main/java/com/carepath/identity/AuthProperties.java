package com.carepath.identity;

import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("carepath.auth")
public record AuthProperties(
    @NotBlank String issuer, @NotBlank String audience,
    @Min(60) @Max(900) long accessSeconds,
    @Min(300) @Max(2592000) long refreshSeconds,
    @Min(10) @Max(14) int bcryptCost,
    @NotBlank String privateKeyPath, @NotBlank String publicKeyPath,
    boolean cookieSecure, @NotBlank String frontendOrigin,
    @NotBlank @Size(min=32) String rateKey,
    @Min(1) @Max(1000) int ipLimit, @Min(1) @Max(100) int accountLimit,
    @Min(1) @Max(3600) int rateWindowSeconds) {}
