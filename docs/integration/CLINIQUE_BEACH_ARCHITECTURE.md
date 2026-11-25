# Architecture coexistence CLINIQUE Beach

## Principe

CMK ERP = source opérationnelle MySQL (`erp_*`).  
CLINIQUE Beach = application historique SQL Server (schéma figé).  
Réplication = **transactional outbox** MySQL → worker → DML paramétré Beach (flags off par défaut).

## Composants livrés (Lot 0 + preuve TCONTRAT)

| Composant | Rôle |
| --- | --- |
| `ClinicLegacySyncProperties` | Feature flags |
| `ClinicLegacyWriteGuard` | Refus écritures / CH |
| `erp_contrats` | Référentiel types de contrats |
| `erp_sync_outbox` | Journal de réplication |
| `ClinicContratLegacyReader` | SELECT TCONTRAT |
| `ErpContratService` | CRUD MySQL + import + enqueue |
| UI Contrats / Sync | Design System ERP |

## Hors périmètre écritures

- CLINIQUE CH
- Stock / TMEDICAMENT / TPRESTATION (lecture & recette avant DML)
- DDL SQL Server

## Feuille de route

Lot 0 Protection → Lot 1 référentiels simples (Contrats ✓ amorce) → Lot 2 relationnels → Lot 3 mappings → Lot 4 stock → Lot 5 facturation → Lot 6 supervision dashboard.
