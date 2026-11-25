# Guidelines de développement RBAC

## Checklist — Nouvelle fonctionnalité

1. Fonctionnalité inventoriée dans `RBAC_INVENTORY.md`
2. Permission unique déclarée ou réutilisée
3. Parent hiérarchique correctement défini
4. Migration SQL / seed `RbacSchemaInitializer`
5. Attribution automatique ADMIN vérifiée (`GET /api/v1/rbac/admin/integrity`)
6. Visibilité frontend conditionnée
7. Route frontend protégée
8. Endpoint et service backend sécurisés
9. Révocation pendant une session prise en charge
10. Tests d'autorisation exécutés
11. Documentation RBAC actualisée

## Convention de nommage

`MODULE_DOMAINE_RESSOURCE_ACTION` (ex. `DMI_CABINET_CONSULTATION_CREATE`)

- `description` : libellé français pour l'UI admin
- Jamais d'id numérique dans le code applicatif

## Backend

```java
@RequirePermission("DMI_CABINET_CONSULTATION_CREATE")
public ResponseEntity<?> create(...) { ... }
```

Ou :

```java
permissionAuthorization.requirePermission(userId, "DMI_FACTURATION_REFUND");
```

## Frontend

```tsx
const { can } = usePermissions()
{can('DMI_RECEPTION_PATIENT_CREATE') && <CreatePatientButton />}

<PermissionGate permission="ADMIN_ROLES_MANAGE">
  <RolesLink />
</PermissionGate>
```

## Événements

| Événement | ADMIN | Autres rôles |
| --- | --- | --- |
| Ajout module / permission | Auto | Non (décision admin) |
| Suppression permission | Retrait auto | Nettoyage associations |
| Modification droits | Interdite | Dynamique via UI |
| Révocation session | N/A | 403 + refresh UI |
