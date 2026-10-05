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

## Pools Hikari (anti-saturation)

| Pool | Défaut prod | Idle | Timeouts | Notes |
|---|---|---|---|---|
| **Primaire** | max 25 / minIdle 5 | recycle 5 min | conn 20s | `CMK_PRIMARY_DB_MAX_POOL`, `CMK_PRIMARY_DB_MIN_IDLE` |
| **Mediline** | max 5 (hard-cap **8**) | **0** | conn 8s, idle 60s, lifetime 10m | boot non bloquant |
| **CLINIQUE** | max 3 (hard-cap **5**) | **0** | idem + `readOnly=true` | SQL Express : rester bas |

Politique code : `ExternalHikariSupport` (plafonds, fail-fast, `initializationFailTimeout=-1`).

Overrides utiles :

```env
CMK_PRIMARY_DB_MAX_POOL=25
CMK_PRIMARY_DB_MIN_IDLE=5
CMK_MEDILINE_DB_MAX_POOL=5
CMK_CLINIQUE_DB_MAX_POOL=3
```

## Diagnostic UI / API

- `GET /api/v1/datasources/status` (JWT requis) — sonde les 3 bases + stats pool (`active`/`idle`/`total`/`max`/`waiting`), toujours HTTP 200.
- Accueil console (`/portail`) : panneau « Connexions bases de données ».
- Mediline / CLINIQUE : `initializationFailTimeout=-1` → **ne bloquent jamais** le démarrage.
