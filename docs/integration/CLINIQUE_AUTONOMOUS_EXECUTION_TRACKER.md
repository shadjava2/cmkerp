# CLINIQUE — Autonomous Execution Tracker

Dernière mise à jour : 2026-03-25 (reprise post-V32)

| ID | Lot | Description | État | Résultat / blocage |
| --- | --- | --- | --- | --- |
| A0–A6 | 0–4 | Audit, V32, libellés, sélecteurs | DONE | Voir commits e1a8b20 / a284f41 |
| A5b | 4 | PatientPicker | DONE | 1fdb813 |
| R1 | rel | Source de vérité erp_* CREATE/UPDATE + mediline_ipp + V33 | DONE | IDs relationnels ; pont referentials |
| R2 | 4 | Mapping utilisateurs API+UI | DONE | `/mappings/users` |
| R3 | 5 | Mapping pharmacies API+UI | DONE | `/mappings/pharmacies` |
| R4 | 6 | Poste prestations ERP | DONE | LOCAL_ONLY ; Beach off |
| R5 | 5 | Poste médicaments + produit search | DONE | Pas de double stock |
| R6 | dash | Dashboard snapshot relationnel | DONE | `/dashboard/snapshot` |
| R7 | rbac | CREATE perms + ApiPermissionCatalog | DONE | ADMIN grant live |
| R8 | docs | Rapport maître consolidé | DONE | ce fichier + MASTER |
| BLK | 0/1 | Recette SQL Server isolée | BLOCKED | Pas d’env isolé |
| BLK | beach | WRITE hors TCONTRAT | BLOCKED | Autorisation métier requise |
| OPEN | stock | Mapping STOCK1…n | OPEN | Correspondances non inventées |
| OPEN | idmvt | Règles IDMVT / tarification historique | OPEN | Décision métier |

## Volumes vérifiés

Ayants 30412 · Sociétés 484 · Tarifs 1977 · Services 43 · Prod MATCHED 1966
