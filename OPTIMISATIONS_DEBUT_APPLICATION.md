# Optimisations - Démarrage Application

## Problèmes identifiés et résolus

### 1. ❌ ClassNotFoundException: CmkBaseException / BusinessException

**Problème** :

- Les classes d'exception du `shared-kernel` (`CmkBaseException`, `BusinessException`, etc.) n'étaient pas accessibles par le `RestartClassLoader` de Spring DevTools
- `GlobalExceptionHandler` utilise ces classes dans `@ExceptionHandler(...)`
- Le `LazyInitializationBeanFactoryPostProcessor` essaie d'introspecter `GlobalExceptionHandler` pour détecter des méthodes `@Scheduled`, ce qui nécessite de charger toutes les classes référencées dans les annotations
- Le RestartClassLoader ne peut pas charger ces classes car elles sont dans un module Maven externe

**Solution** :

- **Désactivation du lazy initialization en DEV** : Le lazy initialization cause plus de problèmes qu'il n'en résout avec les projets multi-modules Maven
- Les classes du `shared-kernel` restent exclues du restart classloader (chargées depuis le classloader principal)
- Le lazy initialization est désactivé pour éviter l'introspection prématurée qui nécessite le chargement des classes du shared-kernel

**Fichier modifié** : `cmkerp/cmkerp-gateway/src/main/resources/application-dev.yml`

```yaml
spring:
  main:
    lazy-initialization: false # Désactivé pour éviter ClassNotFoundException avec shared-kernel
```

### 2. ⚠️ Warnings Redis (INFO) - Optimisé

**Problème** :

- Messages INFO répétitifs de Spring Data Redis qui essaie d'identifier les repositories
- Ces messages sont normaux (Spring Data Redis ignore les repositories JPA), mais polluent les logs

**Solution** :

- Réduction du niveau de log pour `RepositoryConfigurationExtensionSupport` de INFO à WARN
- Ces messages ne seront plus affichés en INFO, seulement en WARN si vraiment nécessaire

**Fichier modifié** : `cmkerp/cmkerp-gateway/src/main/resources/application-dev.yml`

```yaml
logging:
  level:
    org.springframework.data.redis.repository.configuration.RepositoryConfigurationExtensionSupport: WARN
```

## Résultat attendu

✅ Application démarre sans erreur `ClassNotFoundException`
✅ Logs plus propres (moins de warnings Redis INFO)
✅ Restart DevTools fonctionne correctement

## Notes

- Les warnings Redis sont **normaux** - Spring Data Redis essaie de déterminer si les repositories sont des repositories Redis, mais comme ils sont des repositories JPA, il les ignore correctement
- Le lazy initialization est désactivé en DEV car il cause des problèmes avec les projets multi-modules Maven où les classes du `shared-kernel` ne sont pas accessibles par le RestartClassLoader
- Les classes du `shared-kernel` restent exclues du restart classloader et sont chargées depuis le classloader principal (plus stable)
- En production, le lazy initialization peut être réactivé si nécessaire (pas de DevTools en production)
