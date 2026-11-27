package cd.shad.erp.cmk.cmkerp.gateway.rdv;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.server.ResponseStatusException;

import cd.shad.erp.cmk.cmkerp.sharedkernel.security.ApiPermissionCatalog;

class PublicRdvServiceTest {
  private PublicRdvService service;
  private NamedParameterJdbcTemplate jdbc;
  private Mailbox mailbox;
  private OffsetDateTime start;

  @BeforeEach
  void setUp() throws Exception {
    var source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
    jdbc = new NamedParameterJdbcTemplate(source);
    var sql = jdbc.getJdbcTemplate();
    for (String table : List.of("erp_medecins", "erp_services", "erp_locations")) {
      sql.execute("CREATE TABLE " + table + " (id BIGINT PRIMARY KEY, nom VARCHAR(50), libelle VARCHAR(50), actif BOOLEAN)");
      sql.update("INSERT INTO " + table + " VALUES (1,'Médecin de test','Consultation',TRUE)");
    }
    sql.execute("CREATE TABLE t_personne (CODE VARCHAR(25) PRIMARY KEY, EMAIL VARCHAR(254), PERSONNE_PHYSIQUE BOOLEAN, DATE_NAISSANCE DATE)");
    sql.update("INSERT INTO t_personne VALUES ('TEST-IPP','patiente@example.test',TRUE,DATE '1990-05-12')");
    String schema = new ClassPathResource("db/migration/V48__erp_rendez_vous_public.sql")
        .getContentAsString(StandardCharsets.UTF_8).replace(" CHARACTER SET ascii COLLATE ascii_bin", "");
    try (var connection = source.getConnection()) {
      ScriptUtils.executeSqlScript(connection, new ByteArrayResource(schema.getBytes(StandardCharsets.UTF_8)));
    }
    var beans = new DefaultListableBeanFactory();
    mailbox = new Mailbox();
    beans.registerSingleton("mail", mailbox);
    beans.registerSingleton("mediline", jdbc);
    service = new PublicRdvService(jdbc, beans.getBeanProvider(NamedParameterJdbcTemplate.class),
        beans.getBeanProvider(JavaMailSender.class), "https://rdv.example.test", "rdv@example.test");
    start = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2).withNano(0);
    service.addSlot(1, 1, 1, start, start.plusMinutes(30), 7L);
    service.addSlot(1, 1, 1, start.plusHours(1), start.plusHours(1).plusMinutes(30), 7L);
  }

  @Test
  void guestCanBookWithoutAccountAndRecoverInTheirOwnAccount() {
    long slot = service.slots().getFirst().id();
    service.sendLink("patiente@example.test", "VISITE", slot, "guest");
    String code = mailbox.token;
    var access = service.verifyLink(code);
    assertStatus(HttpStatus.UNAUTHORIZED, () -> service.verifyLink(code));
    assertStatus(HttpStatus.UNAUTHORIZED, () -> service.session(access.rawToken(), true));
    var guest = service.session(access.rawToken(), false);
    var id = UUID.randomUUID();
    assertNull(service.guestReceipt(guest));
    var booking = service.book(guest, slot, id, "Test", "Patiente", false);
    assertEquals(booking, service.book(guest, slot, id, "Test", "Patiente", false));
    assertEquals(booking, service.guestReceipt(guest));
    var day = booking.debut().atZone(ZoneId.of("Africa/Kinshasa")).toLocalDate();
    assertEquals(booking.reference(), service.staffBookings(day, 1L).getFirst().reference());
    assertTrue(service.staffBookings(day, 2L).isEmpty());
    assertEquals(0, jdbc.getJdbcTemplate().queryForObject("SELECT COUNT(*) FROM erp_rdv_comptes", Integer.class));
    assertEquals(1, service.slots().size());

    // La fenêtre d'envoi d'e-mail est indépendante du cycle de réservation.
    jdbc.getJdbcTemplate().update("UPDATE erp_rdv_liens SET throttle_bucket = -1");
    var account = account("patiente@example.test", "account");
    var other = account("autre@example.test", "other");
    assertEquals(List.of(booking), service.list(account));
    assertTrue(service.list(other).isEmpty());
    assertStatus(HttpStatus.NOT_FOUND, () -> service.update(other, booking.reference(), null));
    long nextSlot = service.slots().getFirst().id();
    var moved = service.update(account, booking.reference(), nextSlot);
    assertEquals(nextSlot, moved.creneauId());
    assertEquals(slot, service.slots().getFirst().id());
    assertEquals("ANNULE", service.update(account, booking.reference(), null).statut());
    assertEquals(2, service.slots().size());
    assertFalse(service.profile(account).dossierLie());
    assertStatus(HttpStatus.UNPROCESSABLE_ENTITY,
        () -> service.linkMediline(other, "TEST-IPP", LocalDate.of(1990, 5, 12), "other"));
    assertStatus(HttpStatus.UNPROCESSABLE_ENTITY,
        () -> service.linkMediline(account, "TEST-IPP", LocalDate.of(1990, 5, 13), "account"));
    assertTrue(service.linkMediline(account, "TEST-IPP", LocalDate.of(1990, 5, 12), "account").dossierLie());
  }

  @Test
  void concurrentRequestsHaveOneWinnerAndOverlappingSchedulesAreRejected() throws Exception {
    long slot = service.slots().getFirst().id();
    var left = account("personne1@example.test", "left");
    var right = account("personne2@example.test", "right");
    var barrier = new CyclicBarrier(2);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var first = executor.submit(bookingAttempt(left, slot, barrier));
      var second = executor.submit(bookingAttempt(right, slot, barrier));
      assertEquals(1, (first.get() ? 1 : 0) + (second.get() ? 1 : 0));
    }
    assertStatus(HttpStatus.CONFLICT,
        () -> service.addSlot(1, 1, 1, start.plusMinutes(15), start.plusMinutes(45), 7L));
    assertTrue(ApiPermissionCatalog.isPublicPath("/api/v1/portail-rdv/creneaux"));
    assertFalse(ApiPermissionCatalog.isPublicPath("/api/v1/dmi/rendez-vous/creneaux"));
    assertEquals(List.of("DMI_RECEPTION_RENDEZ_VOUS"), List.of(ApiPermissionCatalog.match(
        "POST", "/api/v1/dmi/rendez-vous/creneaux").orElseThrow().anyOfPermissions()));
    assertEquals(List.of("DMI_RECEPTION_RENDEZ_VOUS", "DMI_CABINET_RENDEZ_VOUS_VIEW"), List.of(ApiPermissionCatalog
        .match("GET", "/api/v1/dmi/rendez-vous").orElseThrow().anyOfPermissions()));
  }

  private PublicRdvService.Session account(String email, String source) {
    service.sendLink(email, "COMPTE", null, source);
    return service.session(service.verifyLink(mailbox.token).rawToken(), true);
  }

  private Callable<Boolean> bookingAttempt(PublicRdvService.Session session, long slot, CyclicBarrier barrier) {
    return () -> {
      barrier.await();
      try {
        service.book(session, slot, UUID.randomUUID(), "Test", "Patient", false);
        return true;
      } catch (ResponseStatusException conflict) {
        assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());
        return false;
      }
    };
  }

  private static void assertStatus(HttpStatus expected, org.junit.jupiter.api.function.Executable action) {
    assertEquals(expected, assertThrows(ResponseStatusException.class, action).getStatusCode());
  }

  private static final class Mailbox extends JavaMailSenderImpl {
    private String token;

    @Override
    public void send(SimpleMailMessage message) {
      token = message.getText().split("#verification=", 2)[1].split("\\s+", 2)[0];
    }
  }
}
