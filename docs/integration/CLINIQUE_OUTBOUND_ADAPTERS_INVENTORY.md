# Inventaire des adaptateurs sortants CLINIQUE Beach

Date : **2026-11-26**  
Whitelist production : **`WRITE_ENTITIES=TCONTRAT` uniquement**

| Entité | Reader legacy | INSERT Beach | UPDATE Beach | Intention outbox | Worker exécute | Prod autorisée |
| --- | --- | --- | --- | --- | --- | --- |
| TCONTRAT | Oui | Oui | Oui | Oui | Oui | **Oui** |
| TSERVICE / TIMPUTATION / TLOCATION | Oui | Non | Non | Générique possible | Non | Non |
| TSOCIETE / TCLASSE / TTARIF / TAYANT | Oui | Non | Non | Non démontré | Non | Non |
| TPRESTATION | Oui | Non | Non | Oui (SUSPENDED / BLOCKED_*) | Non | Non |
| TMEDICAMENT | Oui | Non | Non | Oui (SUSPENDED / BLOCKED_*) | Non | Non |
| TSTOCK | Oui (projection) | Non | Non | N/A | Non | Non |

**Rappel :** une intention `SUSPENDED` n’est **pas** une réplication implémentée.

API de supervision : `GET /api/v1/facturation/adapters/outbound`
