package cd.shad.erp.cmk.cmkerp.platform.security.application.service;

import cd.shad.erp.cmk.cmkerp.platform.dto.response.UtilisateurReportDTO;
import cd.shad.erp.cmk.cmkerp.platform.dto.response.UtilisateurResponse;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service pour la génération de rapports utilisateurs avec JasperReports.
 * Génère des PDF en mémoire (byte[]) pour streaming via REST.
 */
@Service
@Slf4j
public class UserReportService {

    private static final String REPORTS_DIR = "reports/";
    private static final String USERS_REPORT = "utilisateurs.jrxml";

    /**
     * Génère un rapport PDF des utilisateurs.
     *
     * @param utilisateurs Liste des utilisateurs à inclure dans le rapport
     * @return PDF en tant que byte[]
     * @throws JRException Si une erreur survient lors de la génération du rapport
     */
    public byte[] generateUsersReport(List<UtilisateurResponse> utilisateurs) throws JRException {

        log.debug("Génération du rapport utilisateurs: {} utilisateurs", utilisateurs.size());

        // Charger le template .jrxml
        ClassPathResource resource = new ClassPathResource(REPORTS_DIR + USERS_REPORT);
        if (!resource.exists()) {
            String errorMsg = String.format("Template de rapport introuvable: %s%s (chemin complet: %s)",
                    REPORTS_DIR, USERS_REPORT, REPORTS_DIR + USERS_REPORT);
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }
        log.debug("Template de rapport trouvé: {}", resource.getPath());

        // Convertir les UtilisateurResponse en UtilisateurReportDTO
        List<UtilisateurReportDTO> reportData = utilisateurs.stream()
                .map(this::toReportDTO)
                .collect(Collectors.toList());

        // Préparer les paramètres du rapport
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("TITRE_RAPPORT", "Liste des Utilisateurs");
        parameters.put("DATE_GENERATION", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        parameters.put("TOTAL_UTILISATEURS", utilisateurs.size());

        // Charger et compiler le template
        try (InputStream templateStream = resource.getInputStream()) {
            log.debug("Compilation du template JasperReports...");
            JasperReport jasperReport = JasperCompileManager.compileReport(templateStream);
            log.debug("Template compilé avec succès");

            // Créer la source de données
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);
            log.debug("Source de données créée: {} éléments", reportData.size());

            // Remplir le rapport
            log.debug("Remplissage du rapport avec les données...");
            JasperPrint jasperPrint = JasperFillManager.fillReport(
                    jasperReport,
                    parameters,
                    dataSource
            );
            log.debug("Rapport rempli avec succès");

            // Exporter en PDF
            log.debug("Export du rapport en PDF...");
            byte[] pdfBytes = JasperExportManager.exportReportToPdf(jasperPrint);
            log.debug("PDF exporté avec succès: {} bytes", pdfBytes.length);
            return pdfBytes;
        } catch (IOException e) {
            log.error("Erreur lors de la lecture du template de rapport: {}", e.getMessage(), e);
            throw new RuntimeException("Impossible de charger le template de rapport: " + e.getMessage(), e);
        } catch (JRException e) {
            log.error("Erreur JasperReports lors de la génération du rapport: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la génération du PDF: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la génération du rapport: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la génération du PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Convertit un UtilisateurResponse en UtilisateurReportDTO.
     */
    private UtilisateurReportDTO toReportDTO(UtilisateurResponse utilisateur) {
        return UtilisateurReportDTO.builder()
                .id(utilisateur.getId())
                .username(utilisateur.getUsername())
                .nom(utilisateur.getNom())
                .postnom(utilisateur.getPostnom())
                .prenom(utilisateur.getPrenom())
                .sexe(utilisateur.getSexe())
                .specialite(utilisateur.getSpecialite())
                .carted(utilisateur.getCarted())
                .roleName(utilisateur.getRoleName())
                .locked(utilisateur.getLocked())
                .initPassword(utilisateur.getInitPassword())
                .isLoginCard(utilisateur.getIsLoginCard())
                .dateCreate(utilisateur.getDateCreate() != null
                        ? utilisateur.getDateCreate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        : null)
                .dateUpdate(utilisateur.getDateUpdate() != null
                        ? utilisateur.getDateUpdate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        : null)
                .build();
    }
}

