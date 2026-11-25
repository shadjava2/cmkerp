# Rapport d’import — TCONTRAT

## État

| Élément | Statut |
| --- | --- |
| Dry Run API | `POST /api/v1/facturation/contrats/import/dry-run` |
| Import MySQL | `POST /api/v1/facturation/contrats/import` |
| Écriture retour Beach | Désactivée (`write-enabled=false`) |
| Boucle sync | Évitée (origine IMPORT_CLINIQUE) |

Exécuter le Dry Run depuis UI Synchronisation CLINIQUE ou Contrats avant tout import massif.
Documenter les totaux analysés / new / existing / modified dans la session de recette.
