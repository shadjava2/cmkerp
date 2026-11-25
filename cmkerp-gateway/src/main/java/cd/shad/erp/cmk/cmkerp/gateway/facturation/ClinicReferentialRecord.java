package cd.shad.erp.cmk.cmkerp.gateway.facturation;

import java.time.LocalDateTime;

public record ClinicReferentialRecord(
    long id,
    String referentialKey,
    String naturalKey,
    String label,
    String payloadJson,
    String syncFingerprint,
    boolean actif,
    String sourceSystem,
    String legacyKey,
    String syncStatus,
    LocalDateTime lastSyncedAt,
    int version,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {
}
