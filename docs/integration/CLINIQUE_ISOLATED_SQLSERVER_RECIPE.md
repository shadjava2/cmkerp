# Recette SQL Server isolée — Lots 0 et 1

**Objectif :** valider INSERT / UPDATE / retry / timeout / UNCERTAIN / concurrence / idempotence / reprise `SUSPENDED` **sans** toucher Beach ni CH de production.

## État actuel

| Élément | Situation |
| --- | --- |
| Beach production | Lecture + écriture TCONTRAT gated (`WRITE_ENTITIES=TCONTRAT`) |
| Recette isolée | **Non disponible** sur ce poste (`NO_DOCKER` / pas d’instance SQL Server locale dédiée) |
| Clôture officielle Lots 0 / 1 | **Bloquée** jusqu’à exécution de cette recette |

## Option A — Docker (recommandée)

```powershell
docker pull mcr.microsoft.com/mssql/server:2022-latest
docker run -e "ACCEPT_EULA=Y" -e "MSSQL_SA_PASSWORD=Your_strong_Passw0rd" `
  -p 14333:1433 --name cmk-clinic-recipe -d mcr.microsoft.com/mssql/server:2022-latest
```

Schéma minimal (lecture/écriture TCONTRAT uniquement) :

```sql
CREATE DATABASE CLINIQUE_RECIPE;
GO
USE CLINIQUE_RECIPE;
GO
CREATE TABLE dbo.TCONTRAT (
  CODE nchar(3) NOT NULL PRIMARY KEY,
  DESIGNATION nvarchar(50) NULL
);
```

Configurer le gateway de recette (profil dédié, **jamais** les credentials Beach/CH prod) :

```
CMK_CLINIQUE_DB_URL=jdbc:sqlserver://127.0.0.1:14333;databaseName=CLINIQUE_RECIPE;encrypt=false;trustServerCertificate=true
CMK_CLINIQUE_DB_USER=sa
CMK_CLINIQUE_DB_PASSWORD=Your_strong_Passw0rd
CLINIC_BEACH_SYNC_ENABLED=true
CLINIC_BEACH_WRITE_ENABLED=true
CLINIC_BEACH_WRITE_ENTITIES=TCONTRAT
CLINIC_CH_WRITE_ENABLED=false
```

## Option B — Instance SQL Server Express locale

1. Installer SQL Server Express + sqlcmd.
2. Créer `CLINIQUE_RECIPE` + `dbo.TCONTRAT` (script ci-dessus).
3. Pointer `CMK_CLINIQUE_DB_*` vers cette instance.
4. Garder Beach/CH hors de portée.

## Scénarios obligatoires

1. INSERT ERP → outbox PENDING → worker → vérifié SYNCED.
2. UPDATE désignation → même parcours.
3. Rollback métier MySQL → aucune intention orpheline.
4. Retry après FAILED.
5. Timeout / résultat UNCERTAIN puis reprise.
6. Redémarrage gateway avec file non vide.
7. Concurrence (deux workers / claim).
8. Conflit fingerprint inbound.
9. Idempotence (même `operation_id`).
10. Reprise des `SUSPENDED` après extension temporaire de `WRITE_ENTITIES` en recette uniquement.

## Interdits

- Aucun DDL / DML de recette sur Beach ou CH production.
- Aucune extension de `WRITE_ENTITIES` en production sans accord explicite.
- Ne pas déclarer Lots 0 / 1 **CLOS** tant que cette recette n’a pas produit un PV.

## Blocage actuel (poste de développement)

Docker / SQL Server local dédié absents. Procédure ci-dessus à exécuter dès qu’une instance isolée est provisionnée. Le Lot 2 (lecture + import MySQL) **ne dépend pas** de cette recette.
