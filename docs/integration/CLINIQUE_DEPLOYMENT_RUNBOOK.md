# Runbook déploiement coexistence CLINIQUE

## Prérequis

1. Sauvegarde MySQL + procédure restauration DBA Beach.
2. Compte SQL Server sans DDL (lecture + DML whitelist ultérieur).
3. Flags écriture **false** en production jusqu’à autorisation.

## Commandes locales

```powershell
cd c:\Users\HP\Videos\STSDEV\cmkerp
.\scripts\apply-rbac-v30-clinic-contrats.ps1
bash scripts/clinic-legacy-compliance-check.sh
bash scripts/rbac-compliance-check.sh
# Redémarrer gateway — seed RBAC au boot si Flyway off
```

## Activation progressive

Par entité : Contrats → Services → …  
Jamais activer stock / facturation financière sans recette Lot 4–5.
