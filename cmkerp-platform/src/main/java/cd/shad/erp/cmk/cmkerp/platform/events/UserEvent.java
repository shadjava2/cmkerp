package cd.shad.erp.cmk.cmkerp.platform.events;

import lombok.Getter;

/**
 * Événement lié aux utilisateurs (création, modification, etc.).
 *

 */
@Getter
public class UserEvent extends DomainEvent {

    public enum UserEventType {
        USER_CREATED,
        USER_UPDATED,
        USER_DELETED,
        USER_LOCKED,
        USER_UNLOCKED,
        PASSWORD_CHANGED,
        PASSWORD_RESET
    }

    private final Long userId;
    private final String username;
    private final UserEventType type;

    public UserEvent(Long userId, String username, UserEventType type) {
        super("USER_EVENT");
        this.userId = userId;
        this.username = username;
        this.type = type;
    }

    /**
     * Factory method pour créer un événement de création d'utilisateur.
     */
    public static UserEvent created(Long userId, String username) {
        return new UserEvent(userId, username, UserEventType.USER_CREATED);
    }

    /**
     * Factory method pour créer un événement de mise à jour d'utilisateur.
     */
    public static UserEvent updated(Long userId, String username) {
        return new UserEvent(userId, username, UserEventType.USER_UPDATED);
    }

    /**
     * Factory method pour créer un événement de changement de mot de passe.
     */
    public static UserEvent passwordChanged(Long userId, String username) {
        return new UserEvent(userId, username, UserEventType.PASSWORD_CHANGED);
    }

    /**
     * Factory method pour créer un événement de réinitialisation de mot de passe.
     */
    public static UserEvent passwordReset(Long userId, String username) {
        return new UserEvent(userId, username, UserEventType.PASSWORD_RESET);
    }

    /**
     * Factory method pour créer un événement de suppression d'utilisateur.
     */
    public static UserEvent deleted(Long userId, String username) {
        return new UserEvent(userId, username, UserEventType.USER_DELETED);
    }
}

