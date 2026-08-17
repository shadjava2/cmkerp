package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.persistence;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.MailingSendDTO;

/**
 * Lecture et écriture des destinataires email depuis la table {@code mailingsend}.
 */
@Repository
public class MailingSendRepository {

  private static final Logger log = LoggerFactory.getLogger(MailingSendRepository.class);

  private final JdbcTemplate jdbc;

  public MailingSendRepository(@Qualifier("primaryJdbcTemplate") JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<MailingSendDTO> findAll() {
    try {
      return jdbc.query("""
          SELECT id, mail, COALESCE(actif, 0) AS actif
          FROM mailingsend
          ORDER BY actif DESC, id ASC
          """, (rs, rowNum) -> new MailingSendDTO(
          rs.getLong("id"),
          rs.getString("mail"),
          rs.getInt("actif") == 1));
    } catch (DataAccessException e) {
      log.error("Impossible de lire mailingsend — vérifiez la migration V14", e);
      return Collections.emptyList();
    }
  }

  public Optional<MailingSendDTO> findById(Long id) {
    try {
      List<MailingSendDTO> rows = jdbc.query("""
          SELECT id, mail, COALESCE(actif, 0) AS actif
          FROM mailingsend WHERE id = ?
          """, (rs, rowNum) -> new MailingSendDTO(
          rs.getLong("id"),
          rs.getString("mail"),
          rs.getInt("actif") == 1), id);
      return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
    } catch (DataAccessException e) {
      log.error("Erreur lecture mailingsend id={}", id, e);
      return Optional.empty();
    }
  }

  public boolean existsByMail(String mail, Long excludeId) {
    try {
      Integer count;
      if (excludeId != null) {
        count = jdbc.queryForObject("""
            SELECT COUNT(*) FROM mailingsend
            WHERE LOWER(TRIM(mail)) = ? AND id <> ?
            """, Integer.class, mail, excludeId);
      } else {
        count = jdbc.queryForObject("""
            SELECT COUNT(*) FROM mailingsend
            WHERE LOWER(TRIM(mail)) = ?
            """, Integer.class, mail);
      }
      return count != null && count > 0;
    } catch (DataAccessException e) {
      return false;
    }
  }

  public Long insert(String mail, boolean actif) {
    Long nextId = jdbc.queryForObject(
        "SELECT COALESCE(MAX(id), 0) + 1 FROM mailingsend", Long.class);
    jdbc.update(
        "INSERT INTO mailingsend (id, mail, actif) VALUES (?, ?, ?)",
        nextId, mail, actif ? 1 : 0);
    return nextId;
  }

  public boolean update(Long id, String mail, boolean actif) {
    return jdbc.update(
        "UPDATE mailingsend SET mail = ?, actif = ? WHERE id = ?",
        mail, actif ? 1 : 0, id) > 0;
  }

  public boolean deleteById(Long id) {
    return jdbc.update("DELETE FROM mailingsend WHERE id = ?", id) > 0;
  }

  /**
   * Tous les emails actifs (actif = 1) pour envoi matin et soir.
   */
  public List<String> findActiveEmails() {
    try {
      String sql = """
          SELECT mail FROM mailingsend
          WHERE actif = 1
            AND mail IS NOT NULL
            AND TRIM(mail) <> ''
          ORDER BY id
          """;
      return jdbc.queryForList(sql, String.class);
    } catch (DataAccessException e) {
      log.error("Impossible de lire mailingsend — vérifiez la table et la migration V14", e);
      return Collections.emptyList();
    }
  }
}
