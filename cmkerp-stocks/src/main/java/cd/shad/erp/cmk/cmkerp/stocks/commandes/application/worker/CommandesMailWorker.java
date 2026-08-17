package cd.shad.erp.cmk.cmkerp.stocks.commandes.application.worker;

import cd.shad.erp.cmk.cmkerp.stocks.commandes.domain.repository.CommandesRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import jakarta.mail.internet.MimeMessage;

/**
 * Traite cmd_mail_log PENDING : un mail par fournisseur, CC = destinataires actifs de mailingsend.
 */
@Component
@Slf4j
public class CommandesMailWorker {

  private final CommandesRepository repo;
  private final Optional<JavaMailSender> mailSender;

  public CommandesMailWorker(CommandesRepository repo, ObjectProvider<JavaMailSender> mailSenderProvider) {
    this.repo = repo;
    this.mailSender = Optional.ofNullable(mailSenderProvider.getIfAvailable());
  }

  @Scheduled(fixedDelayString = "${cmkerp.commandes.mail.poll-ms:30000}")
  public void processPendingMails() {
    if (mailSender.isEmpty()) {
      return;
    }
    List<String> cc = repo.listActiveMailingSendEmails();
    List<Map<String, Object>> pending = repo.findPendingMails(20);
    for (Map<String, Object> row : pending) {
      Long id = ((Number) row.get("id")).longValue();
      try {
        String to = String.valueOf(row.get("destinataire"));
        MimeMessage message = mailSender.get().createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
        helper.setTo(to);
        List<String> ccFiltered = cc.stream()
            .filter(e -> e != null && !e.equalsIgnoreCase(to))
            .distinct()
            .collect(Collectors.toList());
        if (!ccFiltered.isEmpty()) {
          helper.setCc(ccFiltered.toArray(String[]::new));
        }
        helper.setSubject(String.valueOf(row.get("sujet")));
        Object corps = row.get("corps");
        helper.setText(corps != null ? corps.toString() : "", false);
        mailSender.get().send(message);
        repo.markMailSent(id);
        log.info("Mail commandes #{} envoyé à {} (cc={})", id, to, ccFiltered.size());
      } catch (Exception e) {
        log.warn("Échec envoi mail commandes #{}: {}", id, e.getMessage());
        repo.markMailFailed(id, e.getMessage());
      }
    }
  }
}
