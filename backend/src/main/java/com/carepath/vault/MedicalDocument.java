package com.carepath.vault;
import java.util.UUID;
/** Internal persistence aggregate; never serialized to clients. JDBC is used for owner-scoped transactions. */
public record MedicalDocument(UUID ownerId,String storageKey,DocumentDtos.View view) {}
