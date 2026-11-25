# Preuve d’alimentation du catalogue RBAC

Date : **2026-11-25**  
Base cible mesurée : `cmkerp-v24prod` @ `127.0.0.1:32768`

## Diagnostic

| Élément | Constat |
| --- | --- |
| Flyway | **désactivé** (`application-dev.yml` → `flyway.enabled: false`) |
| Migrations V25/V26/V27 | présentes dans le dépôt, **non exécutées** automatiquement |
| Avant correction | **16** permissions (436–451 uniquement) |
| Cause | `RbacSchemaInitializer` phase 2/3 non effectif sur le processus runtime jusqu’à seed SQL manuel + V27 classpath |

## Après application de `scripts/rbac-seed-full-catalog.sql`

| Indicateur | Valeur mesurée |
| --- | ---: |
| `COUNT(*)` permissions | **136** |
| Associations ADMIN (`fkRole=5`) | **136** |
| Permissions manquantes ADMIN | **0** |

### Descendants par parent (extrait)

| Parent | Nb enfants |
| --- | ---: |
| MODULE_STOCK | 30 |
| MODULE_ADMINISTRATION | 17 |
| SIDEBAR_ACCUEIL | 13 |
| SIDEBAR_CABINET | 12 |
| MODULE_DMI (SIDEBAR_*) | 11 |
| MODULE_GMAO | 10 |
| SIDEBAR_RECEPTION | 9 |

## Commandes

```powershell
$env:MYSQL_PWD = '<secret>'
& 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe' `
  --protocol=TCP --host=127.0.0.1 --port=32768 --user=shad `
  --database=cmkerp-v24prod `
  --execute="source C:/Users/HP/Videos/STSDEV/cmkerp/scripts/rbac-seed-full-catalog.sql"
Remove-Item Env:MYSQL_PWD
```

Redémarrer le gateway pour que `RbacSchemaInitializer` recharge aussi V27 au prochain boot :

```powershell
cd c:\Users\HP\Videos\STSDEV\cmkerp
.\run-gateway.ps1
```

Console :

```powershell
cd c:\Users\HP\Videos\STSDEV\consolenotification
npm run desktop:dev
```

Puis : Administration → Rôles & permissions → développer MODULE_DMI / SIDEBAR_ACCUEIL / SIDEBAR_CABINET.
