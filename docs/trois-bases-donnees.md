# Trois bases de données — cmkerp

## Rôles

| Rôle | Base | Moteur | Accès cmkerp |
|---|---|---|---|
| **Primaire** | `cmkerp-v24prod` | MySQL | lecture / écriture métier + Flyway (uniquement ici) |
| **Mediline** | `production` | MySQL | optionnel, JDBC, pas de Flyway, pas de JOIN cross-base |
| **CLINIQUE** | `CLINIQUE` | SQL Server (`SVR-THALIA\SQLEXPRESS`) | optionnel, **lecture seule**, pas de Flyway |

Ne pas brancher le PostgreSQL PACS. Ne jamais DROP / UPDATE / DELETE / DDL sur Mediline ni CLINIQUE depuis cmkerp.

## Variables d'environnement

```env
CMK_PRIMARY_DB_URL=...
CMK_PRIMARY_DB_USER=...
CMK_PRIMARY_DB_PASSWORD=...

CMK_MEDILINE_DB_URL=jdbc:mysql://192.168.100.254:3310/production?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
CMK_MEDILINE_DB_USER=...
CMK_MEDILINE_DB_PASSWORD=...

CMK_CLINIQUE_DB_URL=jdbc:sqlserver://192.168.100.254;instanceName=SQLEXPRESS;databaseName=CLINIQUE;encrypt=false;trustServerCertificate=true
CMK_CLINIQUE_DB_USER=cmk_readonly
CMK_CLINIQUE_DB_PASSWORD=...
```

URL Mediline / CLINIQUE **vides** → l'app démarre avec des stubs (listes vides).

## Auth SQL Server

SSMS utilise souvent l'authentification Windows. Depuis un conteneur Linux Docker, il faut un **login SQL Server** (idéalement en lecture seule) sur `CLINIQUE`. `integratedSecurity=true` n'est pas supporté dans ce setup.

## Beans

- `primaryDataSource` / `primaryJdbcTemplate` (`@Primary`)
- `medilineDataSource` / `medilineJdbcTemplate` (si URL non vide)
- `cliniqueDataSource` / `cliniqueJdbcTemplate` (si URL non vide, `readOnly=true`)

Repositories à injecter dans les services de sync :

- `MedilinePersonneRepository`
- `CliniqueLookupRepository`

## Diagnostic UI / API

- `GET /api/v1/datasources/status` (JWT requis) — sonde les 3 bases, toujours HTTP 200.
- Accueil console (`/portail`) : panneau « Connexions bases de données ».
- Mediline / CLINIQUE : `initializationFailTimeout=-1` → **ne bloquent jamais** le démarrage.
