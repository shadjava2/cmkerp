package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.ConsumptionTrend;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.EmailDeliveryResultDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockIntelligenceAnalysisDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockIntelligenceMultiSnapshotDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockIntelligenceReportType;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockIntelligenceSnapshotDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockProductCategory;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockProductInsightDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.config.ConditionalOnStockIntelligenceEnabled;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.config.StockIntelligenceProperties;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.persistence.MailingSendRepository;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.persistence.StockIntelligenceEmailLogRepository;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.persistence.StockIntelligenceSnapshotRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@ConditionalOnStockIntelligenceEnabled
@RequiredArgsConstructor
@Slf4j
public class StockIntelligenceEmailService {

  private final ObjectProvider<JavaMailSender> mailSenderProvider;
  private final StockIntelligenceProperties properties;
  private final MailingSendRepository mailingSendRepository;
  private final StockIntelligenceSnapshotRepository snapshotRepository;
  private final StockIntelligenceEmailLogRepository emailLogRepository;
  private final StockIntelligenceExcelService excelService;
  private final StockIntelligenceReportNarrativeService narrativeService;

  @Value("${cmkerp.email.max-retries:3}")
  private int maxRetries;

  @Value("${cmkerp.email.retry-backoff-ms:1000}")
  private long retryBackoffMs;

  @Value("${spring.mail.username:}")
  private String fromEmail;

  @Value("${spring.mail.host:}")
  private String mailHost;

  public boolean isMailConfigured() {
    return mailSenderProvider.getIfAvailable() != null
        && mailHost != null
        && !mailHost.isBlank();
  }

  public EmailDeliveryResultDTO sendReport(
      StockIntelligenceReportType reportType,
      StockIntelligenceMultiSnapshotDTO snapshot,
      StockIntelligenceAnalysisDTO analysis,
      List<String> recipients,
      Long snapshotId) {

    List<String> toList = resolveRecipients(reportType, recipients);
    if (toList.isEmpty()) {
      log.warn("Aucun destinataire email pour le rapport {} — mailingsend.actif=1 ou saisie manuelle", reportType);
      emailLogRepository.log(reportType.name(), "-", "SKIPPED", snapshotId, "Aucun destinataire");
      return EmailDeliveryResultDTO.empty();
    }

    JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
    if (mailSender == null) {
      log.error("JavaMailSender absent — configurez spring.mail.host / username dans application.yml");
      for (String to : toList) {
        emailLogRepository.log(reportType.name(), to, "FAILED", snapshotId, "SMTP non configuré (spring.mail.*)");
      }
      return new EmailDeliveryResultDTO(toList.size(), 0, toList.size(), toList);
    }

    String periodLabel = reportType == StockIntelligenceReportType.EVENING ? "soir" : "matin";
    if (reportType == StockIntelligenceReportType.ON_DEMAND) {
      periodLabel = "demande";
    }
    String subject = properties.getEmail().getSubjectPrefix() + " Rapport " + periodLabel + " — "
        + snapshot.generatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    String html = buildHtml(reportType, snapshot, analysis);

    byte[] excelBytes;
    String excelFilename;
    try {
      excelBytes = excelService.generateWorkbook(snapshot, reportType);
      excelFilename = excelService.buildFilename(snapshot, reportType);
    } catch (Exception e) {
      log.error("Génération Excel impossible — envoi annulé", e);
      String detail = "Génération Excel: " + e.getMessage();
      for (String to : toList) {
        emailLogRepository.log(reportType.name(), to, "FAILED", snapshotId, detail);
      }
      return new EmailDeliveryResultDTO(toList.size(), 0, toList.size(), toList);
    }

    int sent = 0;
    int failed = 0;
    List<String> failedAddresses = new ArrayList<>();

    for (String to : toList) {
      if (sendWithRetry(mailSender, to, subject, html, excelBytes, excelFilename)) {
        sent++;
        emailLogRepository.log(reportType.name(), to, "SENT", snapshotId, null);
      } else {
        failed++;
        failedAddresses.add(to);
        emailLogRepository.log(reportType.name(), to, "FAILED", snapshotId, "Échec après retries SMTP");
      }
    }

    log.info("Rapport {} — emails: {}/{} envoyés, {} échec(s)", reportType, sent, toList.size(), failed);
    return new EmailDeliveryResultDTO(toList.size(), sent, failed, failedAddresses);
  }

  private boolean sendWithRetry(
      JavaMailSender mailSender,
      String to,
      String subject,
      String html,
      byte[] excelBytes,
      String excelFilename) {

    int attempt = 0;
    while (attempt <= maxRetries) {
      try {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        if (fromEmail != null && !fromEmail.isBlank()) {
          helper.setFrom(fromEmail);
        }
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);
        helper.addAttachment(excelFilename, new ByteArrayResource(excelBytes),
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        mailSender.send(message);
        log.info("Email rapport envoyé -> {}", to);
        return true;
      } catch (Exception e) {
        attempt++;
        if (attempt <= maxRetries) {
          long backoff = retryBackoffMs * (1L << (attempt - 1));
          log.warn("Retry email {} ({}/{}) dans {}ms", to, attempt, maxRetries, backoff, e);
          try {
            Thread.sleep(backoff);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return false;
          }
        } else {
          log.error("Échec définitif email -> {}", to, e);
        }
      }
    }
    return false;
  }

  List<String> resolveRecipients(StockIntelligenceReportType reportType, List<String> recipients) {
    List<String> explicit = normalizeEmails(recipients);
    if (!explicit.isEmpty()) {
      log.info("Destinataires explicites ({}): {}", explicit.size(), explicit);
      return explicit;
    }
    List<String> fromMailingSend = mailingSendRepository.findActiveEmails();
    if (!fromMailingSend.isEmpty()) {
      log.info("Destinataires mailingsend (actif): {} pour rapport {}", fromMailingSend.size(), reportType);
      return fromMailingSend;
    }
    List<String> fromLegacy = reportType == StockIntelligenceReportType.EVENING
        ? snapshotRepository.findEveningRecipientEmails()
        : snapshotRepository.findMorningRecipientEmails();
    if (!fromLegacy.isEmpty()) {
      return fromLegacy;
    }
    if (properties.getEmail().getDefaultRecipient() != null
        && !properties.getEmail().getDefaultRecipient().isBlank()) {
      return List.of(properties.getEmail().getDefaultRecipient().trim());
    }
    return List.of();
  }

  public static List<String> normalizeEmails(List<String> recipients) {
    if (recipients == null || recipients.isEmpty()) {
      return List.of();
    }
    return recipients.stream()
        .map(s -> s != null ? s.trim().toLowerCase() : "")
        .filter(s -> !s.isBlank() && s.contains("@"))
        .distinct()
        .collect(Collectors.toList());
  }

  private String buildHtml(
      StockIntelligenceReportType reportType,
      StockIntelligenceMultiSnapshotDTO snapshot,
      StockIntelligenceAnalysisDTO analysis) {

    String moisCourant = narrativeService.moisEnCoursLabel();
    String moisPrecedent = narrativeService.moisPrecedentLabel();

    StringBuilder sb = new StringBuilder();
    sb.append("<html><body style='font-family:Arial,sans-serif;color:#1e293b;line-height:1.45;'>");
    sb.append("<h2>CMK ERP — ").append(esc(reportType.label())).append("</h2>");
    sb.append("<p><strong>Généré le :</strong> ").append(esc(snapshot.generatedAt().toString())).append("</p>");
    sb.append("<p><strong>Pharmacies centrales :</strong> ").append(snapshot.pharmacies().size()).append("</p>");
    sb.append("<p><em>Détail complet en pièce jointe Excel. Les tableaux ci-dessous comparent ")
        .append(esc(moisCourant)).append(" et ").append(esc(moisPrecedent)).append(".</em></p>");

    appendExpertAnalysisSection(sb, analysis, snapshot);

    sb.append("<h3>Synthèse automatique (données ERP)</h3>");
    sb.append(narrativeService.buildHtmlNarrative(snapshot));

    var g = snapshot.resumeGlobal();
    sb.append("<h3>Synthèse globale (activité mois en cours)</h3><ul>");
    sb.append("<li><strong>Actifs ce mois</strong> (entrée ou sortie) : ").append(g.totalAvecMouvement()).append("</li>");
    sb.append("<li><strong>Inactifs ce mois</strong> — stock OK : ").append(g.totalStockSansMouvement()).append("</li>");
    sb.append("<li><strong>Inactifs ce mois</strong> — rupture / sous seuil : ")
        .append(g.totalRuptureSansMouvement()).append("</li>");
    sb.append("<li><strong>Total en rupture</strong> (tous états) : ").append(g.totalRuptures()).append("</li>");
    sb.append("</ul>");

    sb.append("<h3>Par pharmacie</h3>");
    sb.append("<table border='1' cellpadding='6' cellspacing='0' style='border-collapse:collapse;font-size:13px;'>");
    sb.append("<tr><th>Pharmacie</th><th>Analysés</th><th>Actifs ce mois</th>")
        .append("<th>Inactifs (stock OK)</th><th>Inactifs (rupture)</th><th>Ruptures tot.</th></tr>");
    for (StockIntelligenceSnapshotDTO ph : snapshot.pharmacies()) {
      var r = ph.resume();
      sb.append("<tr>");
      sb.append("<td>").append(esc(ph.pharmacieLabel())).append("</td>");
      sb.append("<td>").append(r.totalProduitsAnalyses()).append("</td>");
      sb.append("<td>").append(r.totalAvecMouvement()).append("</td>");
      sb.append("<td>").append(r.totalStockSansMouvement()).append("</td>");
      sb.append("<td>").append(r.totalRuptureSansMouvement()).append("</td>");
      sb.append("<td>").append(r.totalRuptures()).append("</td>");
      sb.append("</tr>");
    }
    sb.append("</table>");

    for (StockIntelligenceSnapshotDTO ph : snapshot.pharmacies()) {
      appendMovementTable(sb, ph, moisCourant, moisPrecedent, 12);
      appendInactiveTable(sb, ph, moisCourant, StockProductCategory.RUPTURE_SANS_MOUVEMENT,
          " — ruptures sans activité ce mois", 8);
      appendInactiveTable(sb, ph, moisCourant, StockProductCategory.STOCK_SANS_MOUVEMENT,
          " — stock dormant (aucune activité ce mois)", 5);
    }

    sb.append("<p style='color:#64748b;font-size:12px;'>Rapport automatique CMK ERP — décision humaine requise pour les achats.</p>");
    sb.append("</body></html>");
    return sb.toString();
  }

  private void appendExpertAnalysisSection(
      StringBuilder sb,
      StockIntelligenceAnalysisDTO analysis,
      StockIntelligenceMultiSnapshotDTO snapshot) {

    String model = properties.getOpenai().getModel();
    boolean openAiOn = properties.getOpenai().isEnabled();

    if (analysis == null) {
      if (openAiOn) {
        sb.append("<div style='background:#fef3c7;border:1px solid #f59e0b;padding:12px;margin:16px 0;border-radius:6px;'>");
        sb.append("<strong>Agent IA :</strong> activé mais analyse non disponible pour cet envoi ");
        sb.append("(vérifiez la clé dans <code>application-dev-secrets.yml</code> ou les logs gateway).");
        sb.append("</div>");
      } else {
        sb.append("<div style='background:#f1f5f9;border:1px solid #94a3b8;padding:12px;margin:16px 0;border-radius:6px;'>");
        sb.append("<strong>Agent IA désactivé.</strong> Activez OpenAI dans ")
            .append("<code>application-dev-secrets.yml</code> pour l'analyse expert automatique.");
        sb.append("</div>");
      }
      return;
    }

    sb.append("<div style='background:#eff6ff;border-left:4px solid #2563eb;padding:16px;margin:20px 0;border-radius:4px;'>");
    sb.append("<h3 style='margin-top:0;color:#1e40af;'>Analyse expert — agent IA (données réelles ERP)</h3>");
    sb.append("<p style='font-size:12px;color:#475569;margin-bottom:12px;'>")
        .append("Cette section est produite par un agent d'analyse (modèle <strong>")
        .append(esc(model)).append("</strong>) à partir des données stock extraites en direct du CMK ERP ")
        .append("(").append(snapshot.pharmacies().size()).append(" pharmacie(s), ")
        .append(snapshot.resumeGlobal().totalProduitsAnalyses()).append(" produits, horodatage ")
        .append(esc(snapshot.generatedAt().toString())).append("). ")
        .append("Il s'agit d'un avis d'expert algorithmique pour accompagner la décision — ")
        .append("<strong>pas un substitut</strong> au jugement des responsables achats / pharmacie.</p>");

    sb.append("<p><strong>Niveau de risque global :</strong> ")
        .append(riskBadgeHtml(analysis.niveauRisque())).append("</p>");

    if (notBlank(analysis.syntheseExecutive())) {
      sb.append("<p style='font-size:15px;'><strong>Synthèse exécutive :</strong> ")
          .append(esc(analysis.syntheseExecutive())).append("</p>");
    }
    if (notBlank(analysis.resumeDirection())) {
      sb.append("<p><strong>Message direction :</strong> ").append(esc(analysis.resumeDirection())).append("</p>");
    }
    if (notBlank(analysis.commentaireExpert())) {
      sb.append("<p style='background:#fff;padding:10px;border-radius:4px;'>")
          .append(esc(analysis.commentaireExpert())).append("</p>");
    }

    appendBulletSection(sb, "Actions prioritaires — 48 h", analysis.actionsPrioritaires48h());
    appendBulletSection(sb, "Perspectives (7–30 jours)", analysis.perspectives());
    appendBulletSection(sb, "Risques anticipés si inaction", analysis.anticipationRisques());
    appendBulletSection(sb, "Recommandations stratégiques", analysis.recommandations());

    if (analysis.alertes() != null && !analysis.alertes().isEmpty()) {
      sb.append("<h4 style='color:#1e40af;'>Alertes produits</h4>");
      sb.append("<table border='1' cellpadding='5' cellspacing='0' style='border-collapse:collapse;font-size:12px;width:100%;'>");
      sb.append("<tr><th>Produit</th><th>Risque</th><th>Urgence</th><th>Action</th></tr>");
      analysis.alertes().stream().limit(15).forEach(a -> {
        sb.append("<tr>");
        sb.append("<td>").append(esc(a.produit())).append("</td>");
        sb.append("<td>").append(esc(a.risque())).append("</td>");
        sb.append("<td>").append(urgenceBadgeHtml(a.urgence())).append("</td>");
        sb.append("<td>").append(esc(a.action())).append("</td>");
        sb.append("</tr>");
      });
      sb.append("</table>");
      if (analysis.alertes().size() > 15) {
        sb.append("<p><em>").append(analysis.alertes().size() - 15)
            .append(" autres alertes dans l'historique snapshot.</em></p>");
      }
    }

    if (analysis.commentairesParCategorie() != null && !analysis.commentairesParCategorie().isEmpty()) {
      sb.append("<h4 style='color:#1e40af;'>Analyse par segment de stock</h4><ul>");
      analysis.commentairesParCategorie().forEach(c -> {
        sb.append("<li><strong>").append(esc(labelCategorie(c.categorie()))).append(" :</strong> ");
        sb.append(esc(c.analyse()));
        if (c.points_cles() != null && !c.points_cles().isEmpty()) {
          sb.append("<ul>");
          c.points_cles().forEach(p -> sb.append("<li>").append(esc(p)).append("</li>"));
          sb.append("</ul>");
        }
        sb.append("</li>");
      });
      sb.append("</ul>");
    }

    sb.append("</div>");
  }

  private static void appendBulletSection(StringBuilder sb, String title, List<String> items) {
    if (items == null || items.isEmpty()) {
      return;
    }
    sb.append("<h4 style='color:#1e40af;margin-bottom:6px;'>").append(esc(title)).append("</h4><ul>");
    items.forEach(i -> sb.append("<li>").append(esc(i)).append("</li>"));
    sb.append("</ul>");
  }

  private static String labelCategorie(String code) {
    if (code == null) {
      return "Segment";
    }
    return switch (code) {
      case "AVEC_MOUVEMENT" -> "Actifs ce mois";
      case "STOCK_SANS_MOUVEMENT" -> "Stock dormant";
      case "RUPTURE_SANS_MOUVEMENT" -> "Ruptures sans activité";
      default -> code;
    };
  }

  private static String riskBadgeHtml(String niveau) {
    String color = switch (niveau != null ? niveau.toLowerCase() : "") {
      case "critique" -> "#dc2626";
      case "eleve" -> "#ea580c";
      case "modere" -> "#ca8a04";
      default -> "#16a34a";
    };
    return "<span style='background:" + color + ";color:#fff;padding:2px 8px;border-radius:4px;'>"
        + esc(niveau != null ? niveau : "—") + "</span>";
  }

  private static String urgenceBadgeHtml(String urgence) {
    String color = switch (urgence != null ? urgence.toLowerCase() : "") {
      case "critique" -> "#dc2626";
      case "attention" -> "#ea580c";
      default -> "#64748b";
    };
    return "<span style='color:" + color + ";font-weight:bold;'>"
        + esc(urgence != null ? urgence : "info") + "</span>";
  }

  private static boolean notBlank(String s) {
    return s != null && !s.isBlank();
  }

  /** Produits avec entrées ou sorties sur le mois en cours — tri par volume de sorties. */
  private void appendMovementTable(
      StringBuilder sb,
      StockIntelligenceSnapshotDTO snapshot,
      String moisCourant,
      String moisPrecedent,
      int limit) {

    List<StockProductInsightDTO> items = snapshot.produitsParCategorie()
        .getOrDefault(StockProductCategory.AVEC_MOUVEMENT, List.of()).stream()
        .sorted((a, b) -> b.sortiesMoisEnCours().compareTo(a.sortiesMoisEnCours()))
        .toList();
    if (items.isEmpty()) {
      return;
    }
    sb.append("<h3>").append(esc(snapshot.pharmacieLabel()))
        .append(" — mouvements ").append(esc(moisCourant))
        .append(" (").append(items.size()).append(")</h3>");
    sb.append("<table border='1' cellpadding='5' cellspacing='0' style='border-collapse:collapse;font-size:12px;'>");
    sb.append("<tr><th>Produit</th><th>Stock</th>")
        .append("<th>Entrées<br/><small>").append(esc(moisCourant)).append("</small></th>")
        .append("<th>Sorties<br/><small>").append(esc(moisCourant)).append("</small></th>")
        .append("<th>Entrées<br/><small>").append(esc(moisPrecedent)).append("</small></th>")
        .append("<th>Sorties<br/><small>").append(esc(moisPrecedent)).append("</small></th>")
        .append("<th>Tendance</th><th>Dernier mvmt</th><th>J. couv.</th></tr>");
    items.stream().limit(limit).forEach(p -> appendProductMovementRow(sb, p));
    sb.append("</table>");
    if (items.size() > limit) {
      sb.append("<p><em>").append(items.size() - limit)
          .append(" autres produits actifs ce mois dans l'Excel joint.</em></p>");
    }
  }

  private void appendInactiveTable(
      StringBuilder sb,
      StockIntelligenceSnapshotDTO snapshot,
      String moisCourant,
      StockProductCategory category,
      String titleSuffix,
      int limit) {

    List<StockProductInsightDTO> items = snapshot.produitsParCategorie().getOrDefault(category, List.of());
    if (items.isEmpty()) {
      return;
    }
    sb.append("<h3>").append(esc(snapshot.pharmacieLabel())).append(esc(titleSuffix))
        .append(" (").append(items.size()).append(")</h3>");
    sb.append("<p style='font-size:12px;color:#475569;'>Aucune entrée ni sortie en ")
        .append(esc(moisCourant)).append(".</p>");
    sb.append("<table border='1' cellpadding='5' cellspacing='0' style='border-collapse:collapse;font-size:12px;'>");
    sb.append("<tr><th>Produit</th><th>Stock</th><th>Seuil crit.</th><th>Rupture</th><th>Dernier mvmt</th></tr>");
    items.stream().limit(limit).forEach(p -> {
      sb.append("<tr>");
      sb.append("<td>").append(esc(p.nomCommercial())).append("</td>");
      sb.append("<td>").append(p.stockActuel()).append("</td>");
      sb.append("<td>").append(p.seuilCritique() != null ? p.seuilCritique() : "—").append("</td>");
      sb.append("<td>").append(p.enRupture() ? "Oui" : "Non").append("</td>");
      sb.append("<td>").append(p.dateDernierMouvement() != null ? p.dateDernierMouvement() : "—").append("</td>");
      sb.append("</tr>");
    });
    sb.append("</table>");
    if (items.size() > limit) {
      sb.append("<p><em>").append(items.size() - limit).append(" lignes supplémentaires dans l'Excel.</em></p>");
    }
  }

  private void appendProductMovementRow(StringBuilder sb, StockProductInsightDTO p) {
    sb.append("<tr>");
    sb.append("<td>").append(esc(p.nomCommercial())).append("</td>");
    sb.append("<td>").append(p.stockActuel()).append("</td>");
    sb.append("<td>").append(p.entreesMoisEnCours()).append("</td>");
    sb.append("<td>").append(p.sortiesMoisEnCours()).append("</td>");
    sb.append("<td>").append(p.entreesMoisPrecedent()).append("</td>");
    sb.append("<td>").append(p.sortiesMoisPrecedent()).append("</td>");
    sb.append("<td>").append(formatTrend(p.tendanceSorties())).append("</td>");
    sb.append("<td>").append(p.dateDernierMouvement() != null ? p.dateDernierMouvement() : "—").append("</td>");
    sb.append("<td>").append(p.joursCouvertureEstimes() != null ? p.joursCouvertureEstimes() : "—").append("</td>");
    sb.append("</tr>");
  }

  private String formatTrend(ConsumptionTrend trend) {
    return switch (trend) {
      case HAUSSE -> "Hausse";
      case BAISSE -> "Baisse";
      case STABLE -> "Stable";
      case SANS_SORTIE -> "Sans sortie";
      case INCONNU -> "—";
    };
  }

  private static String esc(String s) {
    if (s == null) {
      return "";
    }
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }
}
