package com.carepath.identity;
import java.util.UUID;
public record CarePrincipal(UUID userId, UUID sessionId) {}
