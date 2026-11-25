package cd.shad.erp.cmk.cmkerp.gateway.facturation;

import java.time.LocalDateTime;

public record ErpContratRecord(
    Long id,
    String code,
    String designation,
    boolean actif,
    String sourceSystem,
    String legacyCode,
    String syncStatus,
    LocalDateTime lastSyncedAt,
    int version,
    LocalDateTime datecreate,
    LocalDateTime dateupdate) {}
