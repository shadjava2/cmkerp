# Sources de vérité par entité

| Entité / champ | Propriétaire coexistence | Notes |
| --- | --- | --- |
| CODE contrat historique | Partagé (immuable) | Pas de renommage sans procédure exceptionnelle |
| DESIGNATION contrat | ERP si write Beach on, sinon ERP local | Conflit si modifié côté CLINIQUE entre-temps |
| `actif` contrat | **ERP uniquement** | Jamais répliqué |
| Identifiants transactions (IDM/IDMVT) | CLINIQUE à la création répliquée | Récupérer clé générée |
| Quantités stock | Indéterminé jusqu’à mapping STOCKn | Écritures off |
| Authentification | ERP | Pas de mots de passe CLINIQUE |
| Signature opération répliquée | Mapping utilisateur ERP→CODE | Bloquer si mapping absent |

Last Write Wins **interdit** sur données financières sans validation.
