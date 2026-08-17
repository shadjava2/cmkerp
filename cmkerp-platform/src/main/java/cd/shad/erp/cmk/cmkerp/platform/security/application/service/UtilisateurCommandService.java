package cd.shad.erp.cmk.cmkerp.platform.security.application.service;

import cd.shad.erp.cmk.cmkerp.platform.dto.request.ChangePasswordRequest;
import cd.shad.erp.cmk.cmkerp.platform.dto.request.UtilisateurRequest;
import cd.shad.erp.cmk.cmkerp.platform.dto.response.UtilisateurResponse;
import cd.shad.erp.cmk.cmkerp.platform.events.IDomainEventPublisher;
import cd.shad.erp.cmk.cmkerp.platform.events.UserEvent;
import cd.shad.erp.cmk.cmkerp.platform.mapper.UtilisateurMapper;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.Utilisateur;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.RoleRepository;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.UtilisateurRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Command Service pour la gestion des utilisateurs (écriture uniquement).
 *
 * <p>Ce service contient toutes les opérations de modification (commands) liées aux utilisateurs.
 * Toutes les méthodes modifient l'état du système et peuvent publier des événements.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UtilisateurCommandService {

    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final UtilisateurMapper utilisateurMapper;
    private final PasswordEncoder passwordEncoder;
    private final IDomainEventPublisher eventPublisher;
    @Qualifier("primaryJdbcTemplate")
    private final JdbcTemplate jdbcTemplate;

    /**
     * Crée un nouvel utilisateur.
     *
     * <p>Valide l'unicité du username et l'existence du rôle,
     * puis crée l'utilisateur avec un mot de passe par défaut si nécessaire.
     */
    public UtilisateurResponse create(UtilisateurRequest request, Long currentUserId) {
        log.debug("Création d'un nouvel utilisateur: {}", request.getUsername());

        // Vérifier l'unicité du username
        if (utilisateurRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new BusinessException("Un utilisateur avec ce nom d'utilisateur existe déjà");
        }

        // Vérifier que le rôle existe
        if (request.getFkRole() != null) {
            roleRepository.findById(request.getFkRole())
                    .orElseThrow(() -> NotFoundException.entity("Role", request.getFkRole()));
        }

        // Convertir le DTO en entité
        Utilisateur utilisateur = utilisateurMapper.toEntity(request);

        // 🎯 CRITICAL: Toujours générer un mot de passe par défaut lors de la création
        // Si initPassword est true, générer un mot de passe aléatoire sécurisé
        // Si initPassword est false, utiliser le mot de passe par défaut "12345678"
        String defaultPassword;
        if (Boolean.TRUE.equals(request.getInitPassword())) {
            // Mot de passe aléatoire sécurisé pour initPassword = true
            String randomPassword = generateRandomPassword();
            defaultPassword = passwordEncoder.encode(randomPassword);
        } else {
            // Mot de passe par défaut "12345678" pour initPassword = false
            defaultPassword = passwordEncoder.encode("12345678");
        }
        utilisateur.setMotDePasse(defaultPassword);

        // Définir les champs techniques
        utilisateur.setUserCreatedId(currentUserId);
        utilisateur.setDateCreate(LocalDateTime.now());

        // Sauvegarder via le repository
        int rows = utilisateurRepository.save(utilisateur);
        if (rows == 0) {
            throw new BusinessException("Échec de la création de l'utilisateur");
        }

        // Récupérer l'utilisateur créé avec son ID
        Utilisateur created = utilisateurRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("Erreur lors de la récupération de l'utilisateur créé"));

        // Publier l'événement
        eventPublisher.publishUserEvent(UserEvent.created(created.getId(), created.getUsername()));

        log.info("Utilisateur créé avec succès: ID={}, username={}", created.getId(), created.getUsername());
        return utilisateurMapper.toResponse(created);
    }

    /**
     * Met à jour un utilisateur existant.
     *
     * <p>Valide l'unicité du username si celui-ci change,
     * et l'existence du rôle si celui-ci change.
     */
    public UtilisateurResponse update(Long id, UtilisateurRequest request, Long currentUserId) {
        log.debug("Mise à jour de l'utilisateur ID: {}", id);

        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> NotFoundException.entity("Utilisateur", id));

        // Vérifier l'unicité du username si celui-ci change
        if (request.getUsername() != null && !request.getUsername().equals(utilisateur.getUsername())) {
            if (utilisateurRepository.findByUsername(request.getUsername()).isPresent()) {
                throw new BusinessException("Un utilisateur avec ce nom d'utilisateur existe déjà");
            }
        }

        // Vérifier que le rôle existe si celui-ci change
        if (request.getFkRole() != null && !request.getFkRole().equals(utilisateur.getFkRole())) {
            roleRepository.findById(request.getFkRole())
                    .orElseThrow(() -> NotFoundException.entity("Role", request.getFkRole()));
        }

        // Mettre à jour l'entité
        utilisateurMapper.updateEntityFromRequest(request, utilisateur);

        // Définir les champs techniques
        utilisateur.setUserUpdatedId(currentUserId);
        utilisateur.setDateUpdate(LocalDateTime.now());

        // Sauvegarder via le repository
        int rows = utilisateurRepository.update(utilisateur);
        if (rows == 0) {
            throw new BusinessException("Échec de la mise à jour de l'utilisateur");
        }

        // Publier l'événement
        eventPublisher.publishUserEvent(UserEvent.updated(utilisateur.getId(), utilisateur.getUsername()));

        log.info("Utilisateur mis à jour avec succès: ID={}", utilisateur.getId());
        return utilisateurMapper.toResponse(utilisateur);
    }

    /**
     * Change le mot de passe d'un utilisateur.
     *
     * <p>Valide que l'utilisateur existe, puis met à jour son mot de passe.
     */
    public void changePassword(Long id, ChangePasswordRequest request, Long currentUserId) {
        log.debug("Changement de mot de passe pour l'utilisateur ID: {}", id);

        // Vérifier que l'utilisateur existe
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> NotFoundException.entity("Utilisateur", id));

        // Encoder le nouveau mot de passe
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());

        // Mettre à jour le mot de passe
        int rows = utilisateurRepository.updatePassword(id, encodedPassword);
        if (rows == 0) {
            throw new BusinessException("Échec du changement de mot de passe");
        }

        // Publier l'événement
        eventPublisher.publishUserEvent(UserEvent.passwordChanged(utilisateur.getId(), utilisateur.getUsername()));

        log.info("Mot de passe changé avec succès pour l'utilisateur ID: {}", id);
    }

    /**
     * Réinitialise le mot de passe d'un utilisateur (admin uniquement).
     *
     * <p>Définit le mot de passe par défaut "12345678", le définit comme mot de passe
     * de l'utilisateur, et met initPassword à false pour forcer l'utilisateur à changer
     * son mot de passe à sa prochaine connexion.
     *
     * @param id ID de l'utilisateur dont le mot de passe doit être réinitialisé
     * @param currentUserId ID de l'utilisateur administrateur effectuant l'opération
     */
    public void resetPassword(Long id, Long currentUserId) {
        log.debug("Réinitialisation du mot de passe pour l'utilisateur ID: {}", id);

        // Vérifier que l'utilisateur existe
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> NotFoundException.entity("Utilisateur", id));

        // Utiliser le mot de passe par défaut "12345678"
        String defaultPassword = "12345678";
        String encodedPassword = passwordEncoder.encode(defaultPassword);

        // Mettre à jour le mot de passe et définir initPassword à false
        utilisateur.setMotDePasse(encodedPassword);
        utilisateur.setInitPassword(false);
        utilisateur.setUserUpdatedId(currentUserId);
        utilisateur.setDateUpdate(LocalDateTime.now());

        // Sauvegarder via le repository
        int rows = utilisateurRepository.update(utilisateur);
        if (rows == 0) {
            throw new BusinessException("Échec de la réinitialisation du mot de passe");
        }

        // Publier l'événement
        eventPublisher.publishUserEvent(UserEvent.passwordReset(utilisateur.getId(), utilisateur.getUsername()));

        log.info("Mot de passe réinitialisé avec succès pour l'utilisateur ID: {} (username: {}) avec le mot de passe par défaut",
                id, utilisateur.getUsername());
    }

    /**
     * Génère un mot de passe aléatoire sécurisé.
     *
     * <p>Le mot de passe généré contient :
     * <ul>
     * <li>Au moins une majuscule</li>
     * <li>Au moins une minuscule</li>
     * <li>Au moins un chiffre</li>
     * <li>Au moins un caractère spécial</li>
     * <li>Longueur totale de 12 caractères</li>
     * </ul>
     *
     * @return un mot de passe aléatoire sécurisé
     */
    private String generateRandomPassword() {
        String uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowercase = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String special = "!@#$%^&*()_+-=[]{}|;:,.<>?";
        String allChars = uppercase + lowercase + digits + special;

        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(12);

        // Garantir au moins un caractère de chaque type
        password.append(uppercase.charAt(random.nextInt(uppercase.length())));
        password.append(lowercase.charAt(random.nextInt(lowercase.length())));
        password.append(digits.charAt(random.nextInt(digits.length())));
        password.append(special.charAt(random.nextInt(special.length())));

        // Remplir le reste avec des caractères aléatoires
        for (int i = password.length(); i < 12; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }

        // Mélanger les caractères pour éviter un pattern prévisible
        char[] passwordArray = password.toString().toCharArray();
        for (int i = passwordArray.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[j];
            passwordArray[j] = temp;
        }

        return new String(passwordArray);
    }

    /**
     * Supprime un utilisateur.
     *
     * <p>Vérifie que l'utilisateur existe avant de le supprimer,
     * puis publie un événement de suppression.
     *
     * @param id l'ID de l'utilisateur à supprimer
     * @param currentUserId l'ID de l'utilisateur qui effectue la suppression
     * @throws NotFoundException si l'utilisateur n'existe pas
     * @throws BusinessException si la suppression échoue
     */
    public void delete(Long id, Long currentUserId) {
        log.debug("Suppression de l'utilisateur ID: {}", id);

        try {
            // Vérifier que l'utilisateur existe
            Utilisateur utilisateur = utilisateurRepository.findById(id)
                    .orElseThrow(() -> NotFoundException.entity("Utilisateur", id));

            String username = utilisateur.getUsername();

            // Supprimer l'utilisateur
            int rows = utilisateurRepository.deleteById(id);
            if (rows == 0) {
                throw new BusinessException("Échec de la suppression de l'utilisateur");
            }

            // Publier l'événement
            eventPublisher.publishUserEvent(UserEvent.deleted(id, username));

            log.info("Utilisateur supprimé avec succès: ID: {} (username: {})", id, username);
        } catch (DataIntegrityViolationException e) {
            // Erreur de contrainte de clé étrangère (l'utilisateur est référencé ailleurs)
            log.warn("Impossible de supprimer l'utilisateur ID: {} - contrainte de clé étrangère: {}", id, e.getMessage());
            throw new BusinessException(
                    "Impossible de supprimer cet utilisateur car il est référencé dans d'autres données. " +
                    "Veuillez supprimer les références avant de supprimer l'utilisateur."
            );
        }
    }
}
