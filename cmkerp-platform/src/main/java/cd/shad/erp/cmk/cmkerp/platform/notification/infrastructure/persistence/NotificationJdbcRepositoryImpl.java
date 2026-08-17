package cd.shad.erp.cmk.cmkerp.platform.notification.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import cd.shad.erp.cmk.cmkerp.platform.notification.domain.model.Notification;
import cd.shad.erp.cmk.cmkerp.platform.notification.domain.repository.NotificationRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.repository.jdbc.AbstractJdbcRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Implémentation JDBC du repository Notification.
 */
@Repository
@Slf4j
public class NotificationJdbcRepositoryImpl extends AbstractJdbcRepository implements NotificationRepository {

    public NotificationJdbcRepositoryImpl(
            @Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate,
            @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedJdbcTemplate) {
        super(jdbcTemplate, namedJdbcTemplate);
    }

    private static final RowMapper<Notification> NOTIFICATION_MAPPER = (rs, rowNum) -> Notification.builder()
            .id(rs.getLong("id"))
            .fkUtilisateur(rs.getLong("fkUtilisateur"))
            .typeNotification(rs.getString("type_notification"))
            .statut(rs.getString("statut"))
            .sujet(rs.getString("sujet"))
            .contenu(rs.getString("contenu"))
            .adresseDestinataire(rs.getString("adresse_destinataire"))
            .dateProgrammee(rs.getTimestamp("date_programmee") != null
                ? rs.getTimestamp("date_programmee").toLocalDateTime()
                : null)
            .dateEnvoi(rs.getTimestamp("date_envoi") != null
                ? rs.getTimestamp("date_envoi").toLocalDateTime()
                : null)
            .reponse(rs.getString("reponse"))
            .dateCreate(rs.getTimestamp("datecreate") != null
                ? rs.getTimestamp("datecreate").toLocalDateTime()
                : null)
            .dateUpdate(rs.getTimestamp("dateupdate") != null
                ? rs.getTimestamp("dateupdate").toLocalDateTime()
                : null)
            .userCreatedId(rs.getLong("usercreateid"))
            .userUpdatedId(rs.getLong("userupdateid"))
            .build();

    @Override
    public Optional<Notification> findById(Long id) {
        String sql = "SELECT * FROM notifications WHERE id = ?";
        return queryForOptional(sql, NOTIFICATION_MAPPER, id);
    }

    @Override
    public List<Notification> findByUtilisateur(Long utilisateurId) {
        String sql = "SELECT * FROM notifications WHERE fkUtilisateur = ? ORDER BY datecreate DESC";
        return queryForList(sql, NOTIFICATION_MAPPER, utilisateurId);
    }

    @Override
    public List<Notification> findByStatut(String statut) {
        String sql = "SELECT * FROM notifications WHERE statut = ? ORDER BY datecreate DESC";
        return queryForList(sql, NOTIFICATION_MAPPER, statut);
    }

    @Override
    public List<Notification> findByUtilisateurAndStatut(Long utilisateurId, String statut) {
        String sql = "SELECT * FROM notifications WHERE fkUtilisateur = ? AND statut = ? ORDER BY datecreate DESC";
        return queryForList(sql, NOTIFICATION_MAPPER, utilisateurId, statut);
    }

    @Override
    public List<Notification> findAll() {
        String sql = "SELECT * FROM notifications ORDER BY datecreate DESC";
        return queryForList(sql, NOTIFICATION_MAPPER);
    }

    @Override
    public int save(Notification notification) {
        String sql = "INSERT INTO notifications (fkUtilisateur, type_notification, statut, sujet, contenu, adresse_destinataire, date_programmee, datecreate, usercreateid) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        LocalDateTime now = notification.getDateCreate() != null ? notification.getDateCreate() : LocalDateTime.now();

        return update(sql,
            notification.getFkUtilisateur(),
            notification.getTypeNotification(),
            notification.getStatut(),
            notification.getSujet(),
            notification.getContenu(),
            notification.getAdresseDestinataire(),
            notification.getDateProgrammee() != null ? java.sql.Timestamp.valueOf(notification.getDateProgrammee()) : null,
            java.sql.Timestamp.valueOf(now),
            notification.getUserCreatedId());
    }

    @Override
    public int update(Notification notification) {
        String sql = "UPDATE notifications SET fkUtilisateur = ?, type_notification = ?, statut = ?, sujet = ?, contenu = ?, "
                + "adresse_destinataire = ?, date_programmee = ?, date_envoi = ?, reponse = ?, dateupdate = ?, userupdateid = ? WHERE id = ?";

        LocalDateTime now = notification.getDateUpdate() != null ? notification.getDateUpdate() : LocalDateTime.now();

        return update(sql,
            notification.getFkUtilisateur(),
            notification.getTypeNotification(),
            notification.getStatut(),
            notification.getSujet(),
            notification.getContenu(),
            notification.getAdresseDestinataire(),
            notification.getDateProgrammee() != null ? java.sql.Timestamp.valueOf(notification.getDateProgrammee()) : null,
            notification.getDateEnvoi() != null ? java.sql.Timestamp.valueOf(notification.getDateEnvoi()) : null,
            notification.getReponse(),
            java.sql.Timestamp.valueOf(now),
            notification.getUserUpdatedId(),
            notification.getId());
    }
}

