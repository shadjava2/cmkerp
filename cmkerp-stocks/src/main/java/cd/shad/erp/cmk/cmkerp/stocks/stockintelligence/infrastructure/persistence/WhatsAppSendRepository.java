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

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.WhatsAppSendDTO;

@Repository
public class WhatsAppSendRepository {

  private static final Logger log = LoggerFactory.getLogger(WhatsAppSendRepository.class);

  private final JdbcTemplate jdbc;

  public WhatsAppSendRepository(@Qualifier("primaryJdbcTemplate") JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<WhatsAppSendDTO> findAll() {
    try {
      return jdbc.query("""
          SELECT id, phone, label, COALESCE(actif, 0) AS actif
          FROM whatsapp_send
          ORDER BY actif DESC, id ASC
          """, (rs, rowNum) -> new WhatsAppSendDTO(
          rs.getLong("id"),
          rs.getString("phone"),
          rs.getString("label"),
          rs.getInt("actif") == 1));
    } catch (DataAccessException e) {
      log.error("Impossible de lire whatsapp_send — vérifiez la migration V17", e);
      return Collections.emptyList();
    }
  }

  public Optional<WhatsAppSendDTO> findById(Long id) {
    try {
      List<WhatsAppSendDTO> rows = jdbc.query("""
          SELECT id, phone, label, COALESCE(actif, 0) AS actif
          FROM whatsapp_send WHERE id = ?
          """, (rs, rowNum) -> new WhatsAppSendDTO(
          rs.getLong("id"),
          rs.getString("phone"),
          rs.getString("label"),
          rs.getInt("actif") == 1), id);
      return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
    } catch (DataAccessException e) {
      return Optional.empty();
    }
  }

  public boolean existsByPhone(String phone, Long excludeId) {
    try {
      Integer count;
      if (excludeId != null) {
        count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM whatsapp_send WHERE phone = ? AND id <> ?",
            Integer.class, phone, excludeId);
      } else {
        count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM whatsapp_send WHERE phone = ?",
            Integer.class, phone);
      }
      return count != null && count > 0;
    } catch (DataAccessException e) {
      return false;
    }
  }

  public Long insert(String phone, String label, boolean actif) {
    jdbc.update(
        "INSERT INTO whatsapp_send (phone, label, actif) VALUES (?, ?, ?)",
        phone, label, actif ? 1 : 0);
    return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
  }

  public boolean update(Long id, String phone, String label, boolean actif) {
    return jdbc.update(
        "UPDATE whatsapp_send SET phone = ?, label = ?, actif = ? WHERE id = ?",
        phone, label, actif ? 1 : 0, id) > 0;
  }

  public boolean deleteById(Long id) {
    return jdbc.update("DELETE FROM whatsapp_send WHERE id = ?", id) > 0;
  }

  public List<String> findActivePhones() {
    try {
      return jdbc.queryForList("""
          SELECT phone FROM whatsapp_send
          WHERE actif = 1 AND phone IS NOT NULL AND TRIM(phone) <> ''
          ORDER BY id
          """, String.class);
    } catch (DataAccessException e) {
      log.error("Impossible de lire whatsapp_send actifs", e);
      return Collections.emptyList();
    }
  }
}
