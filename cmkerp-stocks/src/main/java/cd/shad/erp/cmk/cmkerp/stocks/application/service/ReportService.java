package cd.shad.erp.cmk.cmkerp.stocks.application.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.response.FournisseurResponse;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.response.ProduitReportDTO;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.response.ProduitWithStockResponse;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.dto.response.ApprovisionnementLigneFlatReportDTO;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.dto.response.ApprovisionnementResponse;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.dto.response.LigneApprovReportDTO;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.dto.response.LigneApprovResponse;
import cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.dto.response.InventaireLigneFlatReportDTO;
import cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.dto.response.InventaireResponse;
import cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.dto.response.LigneInventaireResponse;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response.LigneRequisitionReportDTO;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response.LigneTransfertReportDTO;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response.LigneTransfertStockResponse;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response.RequisitionLigneFlatReportDTO;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response.RequisitionResponse;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response.TransfertLigneFlatReportDTO;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response.TransfertStockResponse;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.application.dto.response.LigneVenteResponse;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.application.dto.response.VenteLigneFlatReportDTO;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.application.dto.response.VenteResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

/**
 * Service pour la génération de rapports avec JasperReports. Génère des PDF en mémoire (byte[])
 * pour streaming via REST.
 */
@Service
@Slf4j
public class ReportService {

  private static final String REPORTS_DIR = "reports/";
  private static final String PRODUITS_REPORT = "produits.jrxml";
  private static final String DASHBOARD_STAT_REPORT = "dashboard-stat.jrxml";
  private static final String DASHBOARD_STAT_ACHAT_RISQUE_REPORT =
      "dashboard-stat-achat-risque.jrxml";
  private static final String APPROVISIONNEMENT_REPORT = "approvisionnement.jrxml";
  private static final String IMAGES_DIR = "images-cmkerp/";
  private static final String BACKGROUND_LANDSCAPE = "background_paysage.jpg";
  private static final String BACKGROUND_PORTRAIT = "background.jpg";
  private static final String LOGO_CMK = "logocmk.png";
  private static final String APPROVISIONNEMENTS_LIST_REPORT = "approvisionnements-list.jrxml";
  private static final String FOURNISSEURS_REPORT = "fournisseurs.jrxml";
  private static final String REQUISITION_REPORT = "requisition.jrxml";
  private static final String REQUISITIONS_LIST_REPORT = "requisitions-list.jrxml";
  private static final String TRANSFERT_REPORT = "transfert.jrxml";
  private static final String TRANSFERTS_LIST_REPORT = "transferts-list.jrxml";
  private static final String VENTE_REPORT = "vente.jrxml";
  private static final String VENTES_LIST_REPORT = "ventes-list.jrxml";
  private static final String INVENTAIRE_REPORT = "inventaire.jrxml";
  private static final String INVENTAIRES_LIST_REPORT = "inventaires-list.jrxml";

  // Cache pour les templates compilés (thread-safe)
  private final ConcurrentHashMap<String, JasperReport> compiledTemplatesCache =
      new ConcurrentHashMap<>();

  // Cache pour les images (thread-safe)
  private final ConcurrentHashMap<String, byte[]> imagesCache = new ConcurrentHashMap<>();

  /**
   * Initialise le cache des images au démarrage. Ne bloque pas le démarrage si les images ne sont
   * pas trouvées.
   */
  @PostConstruct
  public void initImageCache() {
    try {
      log.info("[ReportService] Initialisation du cache des images...");
      loadImageToCache(LOGO_CMK);
      loadImageToCache(BACKGROUND_LANDSCAPE);
      loadImageToCache(BACKGROUND_PORTRAIT);
      log.info("[ReportService] Cache des images initialisé: {} images", imagesCache.size());
    } catch (Exception e) {
      // Ne pas bloquer le démarrage si le cache échoue
      log.warn(
          "[ReportService] Erreur lors de l'initialisation du cache des images (non bloquant): {}",
          e.getMessage());
    }
  }

  /**
   * Charge une image dans le cache.
   */
  private void loadImageToCache(String imageName) {
    try {
      String[] possiblePaths =
          {IMAGES_DIR + imageName, "images-cmkerp/" + imageName, "/images-cmkerp/" + imageName};

      for (String path : possiblePaths) {
        ClassPathResource imageResource = new ClassPathResource(path);
        if (imageResource.exists()) {
          byte[] imageBytes = imageResource.getInputStream().readAllBytes();
          imagesCache.put(imageName, imageBytes);
          log.debug("[ReportService] Image mise en cache: {} ({} bytes)", imageName,
              imageBytes.length);
          return;
        }
      }
      log.warn("[ReportService] Image non trouvée pour le cache: {}", imageName);
    } catch (Exception e) {
      log.warn("[ReportService] Erreur lors du chargement de l'image en cache: {} - {}", imageName,
          e.getMessage());
    }
  }

  /**
   * Récupère une image depuis le cache ou la charge si nécessaire.
   */
  private byte[] getImageFromCache(String imageName) {
    return imagesCache.get(imageName);
  }

  /**
   * Compile un template et le met en cache, ou retourne la version en cache.
   */
  private JasperReport getCompiledTemplate(String templateName) throws JRException {
    try {
      // Vérifier le cache d'abord
      JasperReport cached = compiledTemplatesCache.get(templateName);
      if (cached != null) {
        log.debug("[ReportService] Template récupéré du cache: {}", templateName);
        return cached;
      }

      // Compiler et mettre en cache
      String resourcePath = REPORTS_DIR + templateName;
      log.info("[ReportService] Recherche du template: {}", resourcePath);
      ClassPathResource resource = new ClassPathResource(resourcePath);

      if (!resource.exists()) {
        String errorMsg = String.format(
            "Template de rapport introuvable: %s (chemin complet: %s). Vérifiez que le fichier existe dans src/main/resources/reports/",
            resourcePath, resource.getPath());
        log.error("[ReportService] {}", errorMsg);
        throw new RuntimeException(errorMsg);
      }

      log.info("[ReportService] Template trouvé, compilation en cours: {} (sera mis en cache)",
          templateName);
      try (InputStream templateStream = resource.getInputStream()) {
        JasperReport compiled = JasperCompileManager.compileReport(templateStream);
        compiledTemplatesCache.put(templateName, compiled);
        log.info("[ReportService] Template compilé et mis en cache avec succès: {}", templateName);
        return compiled;
      } catch (IOException e) {
        log.error("[ReportService] Erreur lors de la lecture du template: {}", templateName, e);
        throw new RuntimeException("Impossible de charger le template de rapport: " + templateName
            + " - " + e.getMessage(), e);
      }
    } catch (JRException e) {
      log.error("[ReportService] Erreur JasperReports lors de la compilation du template: {}",
          templateName, e);
      log.error("[ReportService] Message: {}, Cause: {}", e.getMessage(),
          e.getCause() != null ? e.getCause().getMessage() : "N/A");
      if (e.getCause() != null) {
        log.error("[ReportService] Stack trace de la cause:", e.getCause());
      }
      throw new RuntimeException(
          "Erreur lors de la compilation du template " + templateName + ": " + e.getMessage()
              + (e.getCause() != null ? " (Cause: " + e.getCause().getMessage() + ")" : ""),
          e);
    } catch (RuntimeException e) {
      log.error("[ReportService] Erreur lors de la récupération du template: {}", templateName, e);
      throw e;
    } catch (Exception e) {
      log.error("[ReportService] Erreur inattendue lors de la récupération du template: {}",
          templateName, e);
      throw new RuntimeException("Erreur inattendue lors de la récupération du template "
          + templateName + ": " + e.getMessage(), e);
    }
  }

  /**
   * Génère un rapport PDF des produits.
   *
   * @param produits Liste des produits à inclure dans le rapport
   * @param pharmacieNom Nom de la pharmacie/entrepôt
   * @return PDF en tant que byte[]
   * @throws JRException Si une erreur survient lors de la génération du rapport
   */
  public byte[] generateProduitsReport(List<ProduitWithStockResponse> produits, String pharmacieNom,
      String utilisateurNom) throws JRException {

    log.debug("Génération du rapport produits: {} produits, pharmacie: {}, utilisateur: {}",
        produits.size(), pharmacieNom, utilisateurNom);

    // Récupérer le template compilé depuis le cache (ou le compiler si nécessaire)
    JasperReport jasperReport;
    try {
      jasperReport = getCompiledTemplate(PRODUITS_REPORT);
      log.debug("[ReportService] Template récupéré/compilé avec succès");
    } catch (Exception e) {
      log.error(
          "[ReportService] Erreur critique lors de la récupération du template produits.jrxml", e);
      throw new RuntimeException(
          "Impossible de compiler le template de rapport produits: " + e.getMessage(), e);
    }

    // Convertir les ProduitWithStockResponse en ProduitReportDTO
    List<ProduitReportDTO> reportData =
        produits.stream().map(this::toReportDTO).collect(Collectors.toList());

    // Préparer les paramètres du rapport
    Map<String, Object> parameters = new HashMap<>();
    // Le paramètre pharmacieNom peut contenir le titre du rapport (pour les stats du dashboard)
    // ou le nom de la pharmacie (pour les rapports normaux)
    // Si pharmacieNom contient "Rupture de stock", "Périmé dans", etc., c'est un titre de stat
    String titreRapport = "Liste des Produits";
    if (pharmacieNom != null && !pharmacieNom.isEmpty()) {
      // Si le nom contient des mots-clés de stats, utiliser comme titre
      if (pharmacieNom.contains("Rupture") || pharmacieNom.contains("Périmé")
          || pharmacieNom.contains("Achat") || pharmacieNom.contains("Dormant")
          || pharmacieNom.contains("mouvementé")) {
        titreRapport = pharmacieNom;
        parameters.put("PHARMACIE_NOM", "");
      } else {
        titreRapport = "Liste des Produits";
        parameters.put("PHARMACIE_NOM", pharmacieNom);
      }
    } else {
      parameters.put("PHARMACIE_NOM", "");
    }
    parameters.put("TITRE_RAPPORT", titreRapport);
    // Formater la date comme String pour éviter les problèmes avec JasperReports
    String dateGeneration = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    parameters.put("DATE_GENERATION", dateGeneration);
    parameters.put("TOTAL_PRODUITS", produits.size());
    parameters.put("UTILISATEUR_NOM", utilisateurNom != null ? utilisateurNom : "");

    // Charger l'image de fond depuis le cache
    byte[] backgroundImageBytes = getImageFromCache(BACKGROUND_LANDSCAPE);
    if (backgroundImageBytes != null) {
      parameters.put("BACKGROUND_IMAGE", new ByteArrayInputStream(backgroundImageBytes));
      log.debug("[ReportService] Image de fond chargée depuis le cache, taille: {} bytes",
          backgroundImageBytes.length);
    } else {
      log.debug("[ReportService] Image de fond non disponible dans le cache");
      parameters.put("BACKGROUND_IMAGE", null);
    }

    byte[] logoImageBytes = getImageFromCache(LOGO_CMK);
    if (logoImageBytes != null) {
      parameters.put("LOGO_IMAGE", new ByteArrayInputStream(logoImageBytes));
      log.debug("[ReportService] Logo chargé depuis le cache, taille: {} bytes",
          logoImageBytes.length);
    } else {
      log.debug("[ReportService] Logo non disponible dans le cache");
      parameters.put("LOGO_IMAGE", null);
    }

    // Utiliser le template compilé depuis le cache
    try {
      // Créer la source de données
      log.debug("[ReportService] Création de la source de données avec {} produits",
          reportData.size());
      if (reportData == null || reportData.isEmpty()) {
        log.warn("[ReportService] Aucune donnée à inclure dans le rapport");
      }
      JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);

      // Remplir le rapport
      log.debug("[ReportService] Remplissage du rapport avec les paramètres: {}",
          parameters.keySet());
      JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
      log.debug("[ReportService] Rapport rempli avec succès, {} pages",
          jasperPrint.getPages().size());

      // Exporter en PDF avec compression optimisée (4x plus rapide)
      log.debug("[ReportService] Export du rapport en PDF avec compression...");
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      net.sf.jasperreports.engine.export.JRPdfExporter exporter =
          new net.sf.jasperreports.engine.export.JRPdfExporter();
      exporter.setExporterInput(new net.sf.jasperreports.export.SimpleExporterInput(jasperPrint));
      exporter.setExporterOutput(
          new net.sf.jasperreports.export.SimpleOutputStreamExporterOutput(baos));
      // Compression optimisée pour performance maximale
      net.sf.jasperreports.export.SimplePdfExporterConfiguration configuration =
          new net.sf.jasperreports.export.SimplePdfExporterConfiguration();
      configuration.setCompressed(true);
      // Optimisations supplémentaires pour vitesse maximale
      configuration.setMetadataAuthor("");
      configuration.setMetadataCreator("");
      configuration.setMetadataKeywords("");
      configuration.setMetadataSubject("");
      configuration.setMetadataTitle("");
      // Désactiver les métadonnées pour réduire la taille
      exporter.setConfiguration(configuration);
      exporter.exportReport();
      byte[] pdfBytes = baos.toByteArray();
      log.debug("[ReportService] PDF exporté avec succès (compressé), {} bytes", pdfBytes.length);
      return pdfBytes;
    } catch (JRException e) {
      log.error("[ReportService] Erreur JasperReports lors de la génération du rapport produits",
          e);
      log.error("[ReportService] Message d'erreur: {}", e.getMessage());
      log.error("[ReportService] Cause: {}",
          e.getCause() != null ? e.getCause().getMessage() : "N/A");
      if (e.getCause() != null) {
        log.error("[ReportService] Stack trace de la cause:", e.getCause());
      }
      throw new RuntimeException(
          "Erreur JasperReports lors de la génération du rapport produits: " + e.getMessage(), e);
    } catch (Exception e) {
      log.error("[ReportService] Erreur inattendue lors de la génération du rapport produits", e);
      log.error("[ReportService] Type d'erreur: {}, Message: {}", e.getClass().getName(),
          e.getMessage());
      throw new RuntimeException(
          "Erreur inattendue lors de la génération du rapport produits: " + e.getMessage(), e);
    }
  }

  /**
   * Génère un rapport PDF pour une statistique du dashboard. Utilise un template Jasper dédié pour
   * les stats du dashboard.
   *
   * @param produits Liste des produits à inclure dans le rapport
   * @param statTitle Titre de la statistique (ex: "Rupture de stock", "Périmé dans 3 mois")
   * @param pharmacieNom Nom de la pharmacie (peut être null)
   * @return PDF en tant que byte[]
   * @throws JRException Si une erreur survient lors de la génération du rapport
   */
  public byte[] generateDashboardStatReport(List<ProduitWithStockResponse> produits,
      String statTitle, String pharmacieNom, String utilisateurNom) throws JRException {

    log.debug(
        "Génération du rapport stat dashboard: {} produits, stat: {}, pharmacie: {}, utilisateur: {}",
        produits.size(), statTitle, pharmacieNom, utilisateurNom);

    // Sélectionner le template approprié selon le type de stat
    // Les stats d'achat (conforme, acceptable, risque élevé, non conforme) utilisent un template
    // spécifique avec la colonne "Date Approv"
    boolean isAchatStat = statTitle != null && (statTitle.contains("Achat Conforme")
        || statTitle.contains("Achat Acceptable") || statTitle.contains("Achat avec risque élevé")
        || statTitle.contains("Achat non conforme") || statTitle.contains("Achat Risqué"));
    String templateName = isAchatStat ? DASHBOARD_STAT_ACHAT_RISQUE_REPORT : DASHBOARD_STAT_REPORT;

    log.info(
        "[ReportService] Sélection du template: statTitle='{}', isAchatStat={}, templateName='{}'",
        statTitle, isAchatStat, templateName);

    // Récupérer le template compilé depuis le cache (ou le compiler si nécessaire)
    JasperReport jasperReport;
    try {
      jasperReport = getCompiledTemplate(templateName);
      log.info("[ReportService] Template récupéré/compilé avec succès: {}", templateName);
    } catch (Exception e) {
      log.error("[ReportService] Erreur critique lors de la récupération du template {}",
          templateName, e);
      log.error("[ReportService] Stack trace complète:", e);
      throw new RuntimeException(
          "Impossible de compiler le template de rapport " + templateName + ": " + e.getMessage(),
          e);
    }

    // Convertir les ProduitWithStockResponse en ProduitReportDTO
    List<ProduitReportDTO> reportData =
        produits.stream().map(this::toReportDTO).collect(Collectors.toList());

    // Préparer les paramètres du rapport
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("TITRE_RAPPORT", statTitle != null ? statTitle : "Rapport de Statistique");
    parameters.put("PHARMACIE_NOM", pharmacieNom != null ? pharmacieNom : "Tous les entrepôts");
    // Formater la date comme String pour éviter les problèmes avec JasperReports
    String dateGeneration = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    parameters.put("DATE_GENERATION", dateGeneration);
    parameters.put("TOTAL_PRODUITS", produits.size());
    parameters.put("UTILISATEUR_NOM", utilisateurNom != null ? utilisateurNom : "");

    // Charger l'image de fond depuis le cache
    byte[] backgroundImageBytes = getImageFromCache(BACKGROUND_LANDSCAPE);
    if (backgroundImageBytes != null) {
      parameters.put("BACKGROUND_IMAGE", new ByteArrayInputStream(backgroundImageBytes));
      log.debug("[ReportService] Image de fond chargée depuis le cache, taille: {} bytes",
          backgroundImageBytes.length);
    } else {
      log.debug("[ReportService] Image de fond non disponible dans le cache");
      parameters.put("BACKGROUND_IMAGE", null);
    }

    byte[] logoImageBytes = getImageFromCache(LOGO_CMK);
    if (logoImageBytes != null) {
      parameters.put("LOGO_IMAGE", new ByteArrayInputStream(logoImageBytes));
      log.debug("[ReportService] Logo chargé depuis le cache, taille: {} bytes",
          logoImageBytes.length);
    } else {
      log.debug("[ReportService] Logo non disponible dans le cache");
      parameters.put("LOGO_IMAGE", null);
    }

    // Utiliser le template compilé depuis le cache
    try {
      // Créer la source de données
      log.debug("[ReportService] Création de la source de données avec {} produits",
          reportData.size());
      if (reportData == null || reportData.isEmpty()) {
        log.warn("[ReportService] Aucune donnée à inclure dans le rapport");
      }
      JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);

      // Remplir le rapport
      log.debug("[ReportService] Remplissage du rapport avec les paramètres: {}",
          parameters.keySet());
      JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
      log.debug("[ReportService] Rapport rempli avec succès, {} pages",
          jasperPrint.getPages().size());

      // Exporter en PDF avec compression optimisée (4x plus rapide)
      log.debug("[ReportService] Export du rapport en PDF avec compression...");
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      net.sf.jasperreports.engine.export.JRPdfExporter exporter =
          new net.sf.jasperreports.engine.export.JRPdfExporter();
      exporter.setExporterInput(new net.sf.jasperreports.export.SimpleExporterInput(jasperPrint));
      exporter.setExporterOutput(
          new net.sf.jasperreports.export.SimpleOutputStreamExporterOutput(baos));
      // Compression optimisée pour performance maximale
      net.sf.jasperreports.export.SimplePdfExporterConfiguration configuration =
          new net.sf.jasperreports.export.SimplePdfExporterConfiguration();
      configuration.setCompressed(true);
      // Optimisations supplémentaires pour vitesse maximale
      configuration.setMetadataAuthor("");
      configuration.setMetadataCreator("");
      configuration.setMetadataKeywords("");
      configuration.setMetadataSubject("");
      configuration.setMetadataTitle("");
      // Désactiver les métadonnées pour réduire la taille
      exporter.setConfiguration(configuration);
      exporter.exportReport();
      byte[] pdfBytes = baos.toByteArray();
      log.debug("[ReportService] PDF exporté avec succès (compressé), {} bytes", pdfBytes.length);
      return pdfBytes;
    } catch (JRException e) {
      log.error(
          "[ReportService] Erreur JasperReports lors de la génération du rapport stat dashboard",
          e);
      log.error("[ReportService] Message d'erreur: {}", e.getMessage());
      log.error("[ReportService] Cause: {}",
          e.getCause() != null ? e.getCause().getMessage() : "N/A");
      if (e.getCause() != null) {
        log.error("[ReportService] Stack trace de la cause:", e.getCause());
      }
      throw new RuntimeException(
          "Erreur JasperReports lors de la génération du rapport stat dashboard: " + e.getMessage(),
          e);
    } catch (Exception e) {
      log.error("[ReportService] Erreur inattendue lors de la génération du rapport stat dashboard",
          e);
      log.error("[ReportService] Type d'erreur: {}, Message: {}", e.getClass().getName(),
          e.getMessage());
      throw new RuntimeException(
          "Erreur inattendue lors de la génération du rapport stat dashboard: " + e.getMessage(),
          e);
    }
  }

  /**
   * Convertit un ProduitWithStockResponse en ProduitReportDTO.
   */
  private ProduitReportDTO toReportDTO(ProduitWithStockResponse produit) {
    return ProduitReportDTO.builder().id(produit.getId()).codebarre(produit.getCodebarre())
        .nomcommercial(produit.getNomcommercial()).nomscientifique(produit.getNomscientifique())
        .forme(produit.getForme()).dosage(produit.getDosage())
        .conditionnement(produit.getConditionnement()).categorie(produit.getCategorie())
        .stockencours(produit.getStockencours()).isactif(produit.getIsactif())
        .peremption(produit.getPeremption()).prixachat(produit.getPrixachat())
        .qtealert(produit.getQtealert()).qtcritique(produit.getQtcritique())
        .perimable(produit.getPerimable()).dateApprov(produit.getDateApprov()).build();
  }

  /**
   * Génère un rapport Excel de la liste des produits.
   *
   * @param produits Liste des produits à inclure dans le rapport
   * @param pharmacieNom Nom de la pharmacie/entrepôt
   * @return Excel en tant que byte[]
   */
  public byte[] generateProduitsReportExcel(List<ProduitWithStockResponse> produits,
      String pharmacieNom) {

    log.debug("Génération du rapport Excel produits: {} produits, pharmacie: {}", produits.size(),
        pharmacieNom);

    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {

      Sheet sheet = workbook.createSheet("Liste des Produits");

      // Style pour l'en-tête
      CellStyle headerStyle = workbook.createCellStyle();
      Font headerFont = workbook.createFont();
      headerFont.setBold(true);
      headerFont.setFontHeightInPoints((short) 12);
      headerStyle.setFont(headerFont);
      headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
      headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      headerStyle.setBorderBottom(BorderStyle.THIN);
      headerStyle.setBorderTop(BorderStyle.THIN);
      headerStyle.setBorderLeft(BorderStyle.THIN);
      headerStyle.setBorderRight(BorderStyle.THIN);

      // Style pour les cellules
      CellStyle cellStyle = workbook.createCellStyle();
      cellStyle.setBorderBottom(BorderStyle.THIN);
      cellStyle.setBorderTop(BorderStyle.THIN);
      cellStyle.setBorderLeft(BorderStyle.THIN);
      cellStyle.setBorderRight(BorderStyle.THIN);

      // Style pour les nombres
      CellStyle numberStyle = workbook.createCellStyle();
      numberStyle.cloneStyleFrom(cellStyle);
      DataFormat format = workbook.createDataFormat();
      numberStyle.setDataFormat(format.getFormat("#,##0.00"));

      int rowNum = 0;

      // En-tête du rapport
      Row headerRow = sheet.createRow(rowNum++);
      Cell headerCell = headerRow.createCell(0);
      headerCell.setCellValue("Rapport des Produits");
      headerCell.setCellStyle(headerStyle);
      sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 10));

      rowNum++; // Ligne vide

      // Informations du rapport
      Row infoRow1 = sheet.createRow(rowNum++);
      infoRow1.createCell(0).setCellValue("Entrepôt:");
      infoRow1.createCell(1)
          .setCellValue(pharmacieNom != null ? pharmacieNom : "Tous les entrepôts");

      Row infoRow2 = sheet.createRow(rowNum++);
      infoRow2.createCell(0).setCellValue("Date:");
      infoRow2.createCell(1)
          .setCellValue(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

      Row infoRow3 = sheet.createRow(rowNum++);
      infoRow3.createCell(0).setCellValue("Total:");
      infoRow3.createCell(1).setCellValue(produits.size() + " produit(s)");

      rowNum++; // Ligne vide

      // En-têtes des colonnes
      Row columnHeaderRow = sheet.createRow(rowNum++);
      String[] headers = {"ID", "Nom Commercial", "Nom Scientifique", "Stock", "Prix Achat",
          "Qté Alerte", "Qté Critique", "Péremption", "Périssable", "Forme", "Dosage"};
      for (int i = 0; i < headers.length; i++) {
        Cell cell = columnHeaderRow.createCell(i);
        cell.setCellValue(headers[i]);
        cell.setCellStyle(headerStyle);
      }

      // Données des produits
      for (ProduitWithStockResponse produit : produits) {
        Row row = sheet.createRow(rowNum++);
        int colNum = 0;

        row.createCell(colNum++).setCellValue(produit.getId() != null ? produit.getId() : 0);
        row.createCell(colNum++)
            .setCellValue(produit.getNomcommercial() != null ? produit.getNomcommercial() : "");
        row.createCell(colNum++)
            .setCellValue(produit.getNomscientifique() != null ? produit.getNomscientifique() : "");

        Cell stockCell = row.createCell(colNum++);
        stockCell.setCellValue(
            produit.getStockencours() != null ? produit.getStockencours().doubleValue() : 0.0);
        stockCell.setCellStyle(numberStyle);

        Cell prixCell = row.createCell(colNum++);
        prixCell.setCellValue(
            produit.getPrixachat() != null ? produit.getPrixachat().doubleValue() : 0.0);
        prixCell.setCellStyle(numberStyle);

        Cell qteAlertCell = row.createCell(colNum++);
        qteAlertCell.setCellValue(
            produit.getQtealert() != null ? produit.getQtealert().doubleValue() : 0.0);
        qteAlertCell.setCellStyle(numberStyle);

        Cell qteCritiqueCell = row.createCell(colNum++);
        qteCritiqueCell.setCellValue(
            produit.getQtcritique() != null ? produit.getQtcritique().doubleValue() : 0.0);
        qteCritiqueCell.setCellStyle(numberStyle);

        row.createCell(colNum++).setCellValue(
            produit.getPeremption() != null ? produit.getPeremption().toString() : "");
        row.createCell(colNum++)
            .setCellValue(produit.getPerimable() != null && produit.getPerimable() ? "Oui" : "Non");
        row.createCell(colNum++).setCellValue(produit.getForme() != null ? produit.getForme() : "");
        row.createCell(colNum++)
            .setCellValue(produit.getDosage() != null ? produit.getDosage() : "");

        // Appliquer le style aux cellules
        for (int i = 0; i < headers.length; i++) {
          Cell cell = row.getCell(i);
          if (cell != null && cell.getCellStyle() == null) {
            cell.setCellStyle(cellStyle);
          }
        }
      }

      // Ajuster la largeur des colonnes
      for (int i = 0; i < headers.length; i++) {
        sheet.autoSizeColumn(i);
        // Limiter la largeur maximale à 50
        if (sheet.getColumnWidth(i) > 50 * 256) {
          sheet.setColumnWidth(i, 50 * 256);
        }
      }

      workbook.write(out);
      return out.toByteArray();

    } catch (IOException e) {
      log.error("Erreur lors de la génération du rapport Excel", e);
      throw new RuntimeException("Impossible de générer le rapport Excel", e);
    }
  }

  /**
   * Génère un rapport PDF d'un bon d'approvisionnement.
   *
   * @param approvisionnement L'approvisionnement à inclure dans le rapport
   * @param lignes Les lignes d'approvisionnement
   * @return PDF en tant que byte[]
   * @throws JRException Si une erreur survient lors de la génération du rapport
   */
  public byte[] generateApprovisionnementReport(ApprovisionnementResponse approvisionnement,
      List<LigneApprovResponse> lignes) throws JRException {

    // Vérifier qu'on a des lignes (même si vide, on génère quand même le rapport avec les en-têtes)
    final List<LigneApprovResponse> lignesFinal = (lignes != null) ? lignes : new ArrayList<>();

    log.info("🚀 [ReportService] Génération du rapport approvisionnement: approvId={}, {} lignes",
        approvisionnement.getId(), lignesFinal.size());
    log.info("📊 [ReportService] Nombre de lignes à traiter: {}", lignesFinal.size());

    // Charger le template .jrxml
    ClassPathResource resource = new ClassPathResource(REPORTS_DIR + APPROVISIONNEMENT_REPORT);
    if (!resource.exists()) {
      String errorMsg = String.format("Template de rapport introuvable: %s%s (chemin complet: %s)",
          REPORTS_DIR, APPROVISIONNEMENT_REPORT, REPORTS_DIR + APPROVISIONNEMENT_REPORT);
      log.error(errorMsg);
      throw new RuntimeException(errorMsg);
    }
    log.info("✅ [ReportService] Template de rapport trouvé: {}", resource.getPath());

    // Calculer les totaux
    BigDecimal totalUsd = lignesFinal.stream()
        .map(ligne -> ligne.getPrixachattotal() != null
            ? BigDecimal.valueOf(ligne.getPrixachattotal().doubleValue())
            : BigDecimal.ZERO)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    final BigDecimal totalConversion;
    final Short tauxValue = approvisionnement.getTaux();
    if (tauxValue != null && tauxValue > 0) {
      totalConversion = totalUsd.multiply(BigDecimal.valueOf(tauxValue));
    } else {
      totalConversion = null;
    }

    // Convertir les lignes en DTOs pour le rapport
    List<LigneApprovReportDTO> reportData =
        IntStream.range(0, lignesFinal.size()).mapToObj(index -> {
          LigneApprovResponse ligne = lignesFinal.get(index);
          BigDecimal prixUnitaire = BigDecimal.ZERO;
          BigDecimal totalUsdLigne = BigDecimal.ZERO;
          BigDecimal totalConvLigne = null;

          if (ligne.getQt() != null && ligne.getQt() > 0 && ligne.getPrixachattotal() != null) {
            totalUsdLigne = BigDecimal.valueOf(ligne.getPrixachattotal().doubleValue());
            prixUnitaire = totalUsdLigne.divide(BigDecimal.valueOf(ligne.getQt()), 2,
                java.math.RoundingMode.HALF_UP);

            if (totalConversion != null && tauxValue != null) {
              totalConvLigne = totalUsdLigne.multiply(BigDecimal.valueOf(tauxValue));
            }
          } else if (ligne.getPrixachat() != null) {
            prixUnitaire = BigDecimal.valueOf(ligne.getPrixachat().doubleValue());
            if (ligne.getQt() != null && ligne.getQt() > 0) {
              totalUsdLigne = prixUnitaire.multiply(BigDecimal.valueOf(ligne.getQt()));
              if (totalConversion != null && tauxValue != null) {
                totalConvLigne = totalUsdLigne.multiply(BigDecimal.valueOf(tauxValue));
              }
            }
          }

          log.debug("📊 [ReportService] Ligne {}: produit={}, qt={}, prixUnitaire={}, totalUsd={}",
              index + 1, ligne.getProduitNom(), ligne.getQt(), prixUnitaire, totalUsdLigne);

          return LigneApprovReportDTO.builder().numero(index + 1).produitNom(ligne.getProduitNom())
              .quantite(ligne.getQt()).prixUnitaire(prixUnitaire).totalUsd(totalUsdLigne)
              .totalConversion(totalConvLigne).build();
        }).toList();

    log.info("✅ [ReportService] {} lignes converties en DTOs pour le rapport", reportData.size());

    // Préparer les paramètres du rapport
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("NUM_BON",
        approvisionnement.getNumbonliv() != null ? approvisionnement.getNumbonliv()
            : "N° " + approvisionnement.getId());
    parameters.put("FOURNISSEUR_NOM",
        approvisionnement.getFournisseurNom() != null ? approvisionnement.getFournisseurNom()
            : "-");
    parameters.put("PHARMACIE_NOM",
        approvisionnement.getPharmacieNom() != null ? approvisionnement.getPharmacieNom() : "-");
    parameters.put("DATE_BL",
        approvisionnement.getDatebonliv() != null
            ? approvisionnement.getDatebonliv().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            : "-");
    parameters.put("DEVISE",
        approvisionnement.getEchangeDeviseMonnaie() != null
            ? approvisionnement.getEchangeDeviseMonnaie()
            : "");
    parameters.put("TAUX",
        approvisionnement.getTaux() != null ? approvisionnement.getTaux().toString() : "");

    String statutLabel = "En attente";
    if ("VALIDEE".equals(approvisionnement.getStatut())) {
      statutLabel = "Validée";
    } else if ("ANNULEE".equals(approvisionnement.getStatut())) {
      statutLabel = "Annulée";
    }
    parameters.put("STATUT", statutLabel);

    parameters.put("DATE_CREATION",
        approvisionnement.getDateCreate() != null
            ? approvisionnement.getDateCreate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            : "-");
    parameters.put("TOTAL_USD", totalUsd);
    parameters.put("TOTAL_CONVERSION", totalConversion);
    parameters.put("DATE_GENERATION",
        LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

    // Charger et compiler le template
    try (InputStream templateStream = resource.getInputStream()) {
      log.info("📄 [ReportService] Compilation du template JasperReports...");
      JasperReport jasperReport = JasperCompileManager.compileReport(templateStream);
      log.info("✅ [ReportService] Template compilé avec succès");

      // Créer la source de données - même si vide, cela créera un PDF avec les en-têtes
      JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);
      log.info("✅ [ReportService] Source de données créée: {} éléments", reportData.size());

      // Log des paramètres pour debug
      log.info(
          "📋 [ReportService] Paramètres du rapport: NUM_BON={}, FOURNISSEUR_NOM={}, PHARMACIE_NOM={}, TOTAL_USD={}, lignes={}",
          parameters.get("NUM_BON"), parameters.get("FOURNISSEUR_NOM"),
          parameters.get("PHARMACIE_NOM"), parameters.get("TOTAL_USD"), reportData.size());

      // Remplir le rapport
      log.info("🖨️ [ReportService] Remplissage du rapport avec les données...");
      JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
      log.info("✅ [ReportService] Rapport rempli avec succès. Nombre de pages: {}",
          jasperPrint.getPages().size());

      // Exporter en PDF
      log.info("📤 [ReportService] Export du rapport en PDF...");
      byte[] pdfBytes = JasperExportManager.exportReportToPdf(jasperPrint);
      log.info("✅ [ReportService] PDF exporté avec succès: {} bytes", pdfBytes.length);
      return pdfBytes;
    } catch (IOException e) {
      log.error("Erreur lors de la lecture du template de rapport", e);
      throw new RuntimeException("Impossible de charger le template de rapport", e);
    }
  }

  /**
   * Génère un rapport Excel du bon d'approvisionnement.
   *
   * @param approvisionnement L'approvisionnement à inclure dans le rapport
   * @param lignes Les lignes d'approvisionnement
   * @return Excel en tant que byte[]
   */
  public byte[] generateApprovisionnementReportExcel(ApprovisionnementResponse approvisionnement,
      List<LigneApprovResponse> lignes) {

    log.debug("Génération du rapport Excel approvisionnement: approvId={}, {} lignes",
        approvisionnement.getId(), lignes.size());

    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {

      Sheet sheet = workbook.createSheet("Bon d'approvisionnement");

      // Style pour l'en-tête
      CellStyle headerStyle = workbook.createCellStyle();
      Font headerFont = workbook.createFont();
      headerFont.setBold(true);
      headerFont.setFontHeightInPoints((short) 14);
      headerStyle.setFont(headerFont);
      headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
      headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      headerStyle.setBorderBottom(BorderStyle.THIN);
      headerStyle.setBorderTop(BorderStyle.THIN);
      headerStyle.setBorderLeft(BorderStyle.THIN);
      headerStyle.setBorderRight(BorderStyle.THIN);

      // Style pour les cellules
      CellStyle cellStyle = workbook.createCellStyle();
      cellStyle.setBorderBottom(BorderStyle.THIN);
      cellStyle.setBorderTop(BorderStyle.THIN);
      cellStyle.setBorderLeft(BorderStyle.THIN);
      cellStyle.setBorderRight(BorderStyle.THIN);

      // Style pour les nombres
      CellStyle numberStyle = workbook.createCellStyle();
      numberStyle.cloneStyleFrom(cellStyle);
      DataFormat format = workbook.createDataFormat();
      numberStyle.setDataFormat(format.getFormat("#,##0.00"));

      // Style pour l'en-tête du titre
      CellStyle titleStyle = workbook.createCellStyle();
      Font titleFont = workbook.createFont();
      titleFont.setBold(true);
      titleFont.setFontHeightInPoints((short) 16);
      titleStyle.setFont(titleFont);
      titleStyle.setAlignment(HorizontalAlignment.CENTER);

      int rowNum = 0;

      // En-tête du rapport
      Row headerRow = sheet.createRow(rowNum++);
      Cell headerCell = headerRow.createCell(0);
      headerCell.setCellValue("BON D'APPROVISIONNEMENT");
      headerCell.setCellStyle(titleStyle);
      sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

      Row numBonRow = sheet.createRow(rowNum++);
      Cell numBonCell = numBonRow.createCell(0);
      numBonCell
          .setCellValue(approvisionnement.getNumbonliv() != null ? approvisionnement.getNumbonliv()
              : "N° " + approvisionnement.getId());
      numBonCell.setCellStyle(titleStyle);
      sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 6));

      rowNum++; // Ligne vide

      // Informations fournisseur et pharmacie
      Row infoRow1 = sheet.createRow(rowNum++);
      infoRow1.createCell(0).setCellValue("Fournisseur:");
      infoRow1.createCell(1).setCellValue(
          approvisionnement.getFournisseurNom() != null ? approvisionnement.getFournisseurNom()
              : "-");
      infoRow1.createCell(3).setCellValue("Pharmacie:");
      infoRow1.createCell(4).setCellValue(
          approvisionnement.getPharmacieNom() != null ? approvisionnement.getPharmacieNom() : "-");

      Row infoRow2 = sheet.createRow(rowNum++);
      infoRow2.createCell(0).setCellValue("Date BL:");
      infoRow2.createCell(1)
          .setCellValue(approvisionnement.getDatebonliv() != null
              ? approvisionnement.getDatebonliv().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
              : "-");
      infoRow2.createCell(3).setCellValue("Statut:");
      String statutLabel = "En attente";
      if ("VALIDEE".equals(approvisionnement.getStatut())) {
        statutLabel = "Validée";
      } else if ("ANNULEE".equals(approvisionnement.getStatut())) {
        statutLabel = "Annulée";
      }
      infoRow2.createCell(4).setCellValue(statutLabel);

      Row infoRow3 = sheet.createRow(rowNum++);
      if (approvisionnement.getEchangeDeviseMonnaie() != null) {
        infoRow3.createCell(0).setCellValue("Devise:");
        infoRow3.createCell(1).setCellValue(approvisionnement.getEchangeDeviseMonnaie());
      }
      if (approvisionnement.getTaux() != null) {
        infoRow3.createCell(3).setCellValue("Taux:");
        infoRow3.createCell(4).setCellValue(approvisionnement.getTaux().toString());
      }
      infoRow3.createCell(5).setCellValue("Date création:");
      infoRow3.createCell(6)
          .setCellValue(approvisionnement.getDateCreate() != null
              ? approvisionnement.getDateCreate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
              : "-");

      rowNum++; // Ligne vide

      // En-têtes des colonnes
      Row columnHeaderRow = sheet.createRow(rowNum++);
      String[] headers;
      if (approvisionnement.getTaux() != null && approvisionnement.getTaux() > 0) {
        headers = new String[] {"N°", "Produit", "Quantité", "Prix unitaire", "Total USD",
            "Total Conversion"};
      } else {
        headers = new String[] {"N°", "Produit", "Quantité", "Prix unitaire", "Total USD"};
      }

      for (int i = 0; i < headers.length; i++) {
        Cell cell = columnHeaderRow.createCell(i);
        cell.setCellValue(headers[i]);
        cell.setCellStyle(headerStyle);
      }

      // Données des lignes
      final Short tauxValue = approvisionnement.getTaux();
      for (LigneApprovResponse ligne : lignes) {
        Row row = sheet.createRow(rowNum++);
        int colNum = 0;

        row.createCell(colNum++).setCellValue(rowNum - columnHeaderRow.getRowNum() - 1);
        row.createCell(colNum++)
            .setCellValue(ligne.getProduitNom() != null ? ligne.getProduitNom() : "-");

        // Quantité
        Cell qtCell = row.createCell(colNum++);
        qtCell.setCellValue(ligne.getQt() != null ? ligne.getQt().doubleValue() : 0.0);
        qtCell.setCellStyle(numberStyle);

        // Prix unitaire et Total USD
        BigDecimal prixUnitaire = BigDecimal.ZERO;
        BigDecimal totalUsdLigne = BigDecimal.ZERO;

        if (ligne.getQt() != null && ligne.getQt() > 0 && ligne.getPrixachattotal() != null) {
          totalUsdLigne = BigDecimal.valueOf(ligne.getPrixachattotal().doubleValue());
          prixUnitaire = totalUsdLigne.divide(BigDecimal.valueOf(ligne.getQt()), 2,
              java.math.RoundingMode.HALF_UP);
        } else if (ligne.getPrixachat() != null) {
          prixUnitaire = BigDecimal.valueOf(ligne.getPrixachat().doubleValue());
          if (ligne.getQt() != null && ligne.getQt() > 0) {
            totalUsdLigne = prixUnitaire.multiply(BigDecimal.valueOf(ligne.getQt()));
          }
        }

        Cell prixUnitaireCell = row.createCell(colNum++);
        prixUnitaireCell.setCellValue(prixUnitaire.doubleValue());
        prixUnitaireCell.setCellStyle(numberStyle);

        Cell totalUsdCell = row.createCell(colNum++);
        totalUsdCell.setCellValue(totalUsdLigne.doubleValue());
        totalUsdCell.setCellStyle(numberStyle);

        // Total Conversion (si applicable)
        if (tauxValue != null && tauxValue > 0) {
          BigDecimal totalConvLigne = totalUsdLigne.multiply(BigDecimal.valueOf(tauxValue));
          Cell totalConvCell = row.createCell(colNum++);
          totalConvCell.setCellValue(totalConvLigne.doubleValue());
          totalConvCell.setCellStyle(numberStyle);
        }

        // Appliquer le style aux cellules sans style
        for (int i = 0; i < headers.length; i++) {
          Cell cell = row.getCell(i);
          if (cell != null && cell.getCellStyle() == null) {
            cell.setCellStyle(cellStyle);
          }
        }
      }

      rowNum++; // Ligne vide

      // Totaux
      BigDecimal totalUsd = lignes.stream()
          .map(ligne -> ligne.getPrixachattotal() != null
              ? BigDecimal.valueOf(ligne.getPrixachattotal().doubleValue())
              : BigDecimal.ZERO)
          .reduce(BigDecimal.ZERO, BigDecimal::add);

      Row totalRow = sheet.createRow(rowNum++);
      totalRow.createCell(3).setCellValue("Total USD:");
      Cell totalUsdCell = totalRow.createCell(4);
      totalUsdCell.setCellValue(totalUsd.doubleValue());
      totalUsdCell.setCellStyle(numberStyle);
      Font totalFont = workbook.createFont();
      totalFont.setBold(true);
      totalUsdCell.getCellStyle().setFont(totalFont);

      if (tauxValue != null && tauxValue > 0) {
        BigDecimal totalConversion = totalUsd.multiply(BigDecimal.valueOf(tauxValue));
        Row totalConvRow = sheet.createRow(rowNum++);
        totalConvRow.createCell(3).setCellValue("Total Conversion:");
        Cell totalConvCell = totalConvRow.createCell(5);
        totalConvCell.setCellValue(totalConversion.doubleValue());
        totalConvCell.setCellStyle(numberStyle);
        totalConvCell.getCellStyle().setFont(totalFont);
      }

      Row nbLignesRow = sheet.createRow(rowNum++);
      nbLignesRow.createCell(0).setCellValue("Nombre de lignes:");
      nbLignesRow.createCell(1).setCellValue(lignes.size());

      // Ajuster la largeur des colonnes
      for (int i = 0; i < headers.length; i++) {
        sheet.autoSizeColumn(i);
        // Limiter la largeur maximale à 50
        if (sheet.getColumnWidth(i) > 50 * 256) {
          sheet.setColumnWidth(i, 50 * 256);
        }
      }

      workbook.write(out);
      return out.toByteArray();

    } catch (IOException e) {
      log.error("Erreur lors de la génération du rapport Excel approvisionnement", e);
      throw new RuntimeException("Impossible de générer le rapport Excel", e);
    }
  }

  /**
   * Génère un rapport PDF contenant une liste d'approvisionnements avec leurs lignes. Chaque
   * approvisionnement est affiché avec toutes ses lignes dans le même PDF.
   *
   * @param approvisionnements Liste des approvisionnements à inclure dans le rapport
   * @param approvIdToLignes Map des IDs d'approvisionnement vers leurs lignes
   * @param pharmacieNom Nom de la pharmacie/service (optionnel)
   * @return PDF en tant que byte[]
   * @throws JRException Si une erreur survient lors de la génération du rapport
   */
  public byte[] generateApprovisionnementsListReport(
      List<ApprovisionnementResponse> approvisionnements,
      Map<Long, List<LigneApprovResponse>> approvIdToLignes, String pharmacieNom)
      throws JRException {

    log.info(
        "🚀 [ReportService] Génération du rapport liste approvisionnements: {} approvisionnements",
        approvisionnements.size());

    // Charger le template .jrxml
    ClassPathResource resource =
        new ClassPathResource(REPORTS_DIR + APPROVISIONNEMENTS_LIST_REPORT);
    if (!resource.exists()) {
      String errorMsg =
          String.format("Template de rapport introuvable: %s%s (chemin complet: %s)", REPORTS_DIR,
              APPROVISIONNEMENTS_LIST_REPORT, REPORTS_DIR + APPROVISIONNEMENTS_LIST_REPORT);
      log.error(errorMsg);
      throw new RuntimeException(errorMsg);
    }
    log.info("✅ [ReportService] Template de rapport trouvé: {}", resource.getPath());

    // Créer une structure plate : une ligne de rapport par ligne d'approvisionnement
    // Chaque ligne contient les infos de l'approvisionnement + les infos de la ligne
    List<ApprovisionnementLigneFlatReportDTO> reportData = new ArrayList<>();

    for (ApprovisionnementResponse approv : approvisionnements) {
      List<LigneApprovResponse> lignes =
          approvIdToLignes.getOrDefault(approv.getId(), new ArrayList<>());

      // Calculer les totaux pour cet approvisionnement
      BigDecimal approvTotalUsd = lignes.stream()
          .map(ligne -> ligne.getPrixachattotal() != null
              ? BigDecimal.valueOf(ligne.getPrixachattotal().doubleValue())
              : BigDecimal.ZERO)
          .reduce(BigDecimal.ZERO, BigDecimal::add);

      BigDecimal approvTotalConversion = null;
      if (approv.getTaux() != null && approv.getTaux() > 0) {
        approvTotalConversion = approvTotalUsd.multiply(BigDecimal.valueOf(approv.getTaux()));
      }

      String statutLabel = "En attente";
      if ("VALIDEE".equals(approv.getStatut())) {
        statutLabel = "Validée";
      } else if ("ANNULEE".equals(approv.getStatut())) {
        statutLabel = "Annulée";
      }

      String numBon =
          approv.getNumbonliv() != null ? approv.getNumbonliv() : "N° " + approv.getId();
      String fournisseurNom = approv.getFournisseurNom() != null ? approv.getFournisseurNom() : "-";
      String approvPharmacieNomLocal =
          approv.getPharmacieNom() != null ? approv.getPharmacieNom() : "-";
      String dateBl = approv.getDatebonliv() != null
          ? approv.getDatebonliv().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
          : "-";
      String dateCreation = approv.getDateCreate() != null
          ? approv.getDateCreate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
          : "-";
      String devise =
          approv.getEchangeDeviseMonnaie() != null ? approv.getEchangeDeviseMonnaie() : "";
      String taux = approv.getTaux() != null ? approv.getTaux().toString() : "";

      // Créer une ligne plate pour chaque ligne d'approvisionnement
      if (lignes.isEmpty()) {
        // Si pas de lignes, créer quand même une ligne avec les infos de l'approvisionnement
        reportData.add(ApprovisionnementLigneFlatReportDTO.builder().numBon(numBon)
            .fournisseurNom(fournisseurNom).pharmacieNom(approvPharmacieNomLocal).dateBl(dateBl)
            .dateCreation(dateCreation).statut(statutLabel).devise(devise).taux(taux)
            .approvTotalUsd(approvTotalUsd).approvTotalConversion(approvTotalConversion)
            .numeroLigne(0).produitNom(null).quantite(null).prixUnitaire(null).totalUsd(null)
            .totalConversion(null).build());
      } else {
        for (int index = 0; index < lignes.size(); index++) {
          LigneApprovResponse ligne = lignes.get(index);
          BigDecimal prixUnitaire = BigDecimal.ZERO;
          BigDecimal totalUsdLigne = BigDecimal.ZERO;
          BigDecimal totalConvLigne = null;

          if (ligne.getQt() != null && ligne.getQt() > 0 && ligne.getPrixachattotal() != null) {
            totalUsdLigne = BigDecimal.valueOf(ligne.getPrixachattotal().doubleValue());
            prixUnitaire = totalUsdLigne.divide(BigDecimal.valueOf(ligne.getQt()), 2,
                java.math.RoundingMode.HALF_UP);

            if (approvTotalConversion != null && approv.getTaux() != null) {
              totalConvLigne = totalUsdLigne.multiply(BigDecimal.valueOf(approv.getTaux()));
            }
          } else if (ligne.getPrixachat() != null) {
            prixUnitaire = BigDecimal.valueOf(ligne.getPrixachat().doubleValue());
            if (ligne.getQt() != null && ligne.getQt() > 0) {
              totalUsdLigne = prixUnitaire.multiply(BigDecimal.valueOf(ligne.getQt()));
              if (approvTotalConversion != null && approv.getTaux() != null) {
                totalConvLigne = totalUsdLigne.multiply(BigDecimal.valueOf(approv.getTaux()));
              }
            }
          }

          reportData.add(ApprovisionnementLigneFlatReportDTO.builder().numBon(numBon)
              .fournisseurNom(fournisseurNom).pharmacieNom(approvPharmacieNomLocal).dateBl(dateBl)
              .dateCreation(dateCreation).statut(statutLabel).devise(devise).taux(taux)
              .approvTotalUsd(approvTotalUsd).approvTotalConversion(approvTotalConversion)
              .numeroLigne(index + 1).produitNom(ligne.getProduitNom()).quantite(ligne.getQt())
              .prixUnitaire(prixUnitaire).totalUsd(totalUsdLigne).totalConversion(totalConvLigne)
              .build());
        }
      }
    }

    log.info("✅ [ReportService] {} lignes (plates) créées pour le rapport", reportData.size());

    // Préparer les paramètres du rapport
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("TITRE_RAPPORT", "Liste des Approvisionnements");
    parameters.put("PHARMACIE_NOM", pharmacieNom != null ? pharmacieNom : "Tous les services");
    parameters.put("DATE_GENERATION",
        LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    parameters.put("TOTAL_APPROVISIONNEMENTS", approvisionnements.size());

    // Charger et compiler le template
    try (InputStream templateStream = resource.getInputStream()) {
      log.info("📄 [ReportService] Compilation du template JasperReports...");
      JasperReport jasperReport = JasperCompileManager.compileReport(templateStream);
      log.info("✅ [ReportService] Template compilé avec succès");

      // Créer la source de données (structure plate)
      JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);
      log.info("✅ [ReportService] Source de données créée: {} lignes plates", reportData.size());

      // Remplir le rapport
      log.info("🖨️ [ReportService] Remplissage du rapport avec les données...");
      JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
      log.info("✅ [ReportService] Rapport rempli avec succès. Nombre de pages: {}",
          jasperPrint.getPages().size());

      // Exporter en PDF
      log.info("📤 [ReportService] Export du rapport en PDF...");
      byte[] pdfBytes = JasperExportManager.exportReportToPdf(jasperPrint);
      log.info("✅ [ReportService] PDF exporté avec succès: {} bytes", pdfBytes.length);
      return pdfBytes;
    } catch (IOException e) {
      log.error("Erreur lors de la lecture du template de rapport", e);
      throw new RuntimeException("Impossible de charger le template de rapport", e);
    }
  }

  /**
   * Génère un rapport Excel de la liste des approvisionnements avec leurs lignes.
   *
   * @param approvisionnements Liste des approvisionnements à inclure dans le rapport
   * @param approvIdToLignes Map des lignes par ID d'approvisionnement
   * @param pharmacieNom Nom de la pharmacie/service
   * @return Excel en tant que byte[]
   */
  public byte[] generateApprovisionnementsListReportExcel(
      List<ApprovisionnementResponse> approvisionnements,
      Map<Long, List<LigneApprovResponse>> approvIdToLignes, String pharmacieNom) {

    log.info(
        "🚀 [ReportService] Génération du rapport Excel liste approvisionnements: {} approvisionnements",
        approvisionnements.size());

    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {

      Sheet sheet = workbook.createSheet("Liste des Approvisionnements");

      // Style pour l'en-tête
      CellStyle headerStyle = workbook.createCellStyle();
      Font headerFont = workbook.createFont();
      headerFont.setBold(true);
      headerFont.setFontHeightInPoints((short) 12);
      headerStyle.setFont(headerFont);
      headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
      headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      headerStyle.setBorderBottom(BorderStyle.THIN);
      headerStyle.setBorderTop(BorderStyle.THIN);
      headerStyle.setBorderLeft(BorderStyle.THIN);
      headerStyle.setBorderRight(BorderStyle.THIN);

      // Style pour les cellules
      CellStyle cellStyle = workbook.createCellStyle();
      cellStyle.setBorderBottom(BorderStyle.THIN);
      cellStyle.setBorderTop(BorderStyle.THIN);
      cellStyle.setBorderLeft(BorderStyle.THIN);
      cellStyle.setBorderRight(BorderStyle.THIN);

      // Style pour les nombres
      CellStyle numberStyle = workbook.createCellStyle();
      numberStyle.cloneStyleFrom(cellStyle);
      DataFormat format = workbook.createDataFormat();
      numberStyle.setDataFormat(format.getFormat("#,##0.00"));

      // Style pour les en-têtes de groupe (approvisionnement)
      CellStyle groupHeaderStyle = workbook.createCellStyle();
      Font groupHeaderFont = workbook.createFont();
      groupHeaderFont.setBold(true);
      groupHeaderFont.setFontHeightInPoints((short) 11);
      groupHeaderFont.setColor(IndexedColors.DARK_BLUE.getIndex());
      groupHeaderStyle.setFont(groupHeaderFont);
      groupHeaderStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
      groupHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      groupHeaderStyle.setBorderBottom(BorderStyle.MEDIUM);
      groupHeaderStyle.setBorderTop(BorderStyle.MEDIUM);
      groupHeaderStyle.setBorderLeft(BorderStyle.MEDIUM);
      groupHeaderStyle.setBorderRight(BorderStyle.MEDIUM);

      int rowNum = 0;

      // En-tête du rapport
      Row headerRow = sheet.createRow(rowNum++);
      Cell headerCell = headerRow.createCell(0);
      headerCell.setCellValue("Rapport des Approvisionnements");
      headerCell.setCellStyle(headerStyle);
      sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 11));

      rowNum++; // Ligne vide

      // Informations du rapport
      Row infoRow1 = sheet.createRow(rowNum++);
      infoRow1.createCell(0).setCellValue("Service:");
      infoRow1.createCell(1)
          .setCellValue(pharmacieNom != null ? pharmacieNom : "Tous les services");

      Row infoRow2 = sheet.createRow(rowNum++);
      infoRow2.createCell(0).setCellValue("Date:");
      infoRow2.createCell(1)
          .setCellValue(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

      Row infoRow3 = sheet.createRow(rowNum++);
      infoRow3.createCell(0).setCellValue("Total:");
      infoRow3.createCell(1).setCellValue(approvisionnements.size() + " approvisionnement(s)");

      rowNum++; // Ligne vide

      // Parcourir chaque approvisionnement
      for (ApprovisionnementResponse approv : approvisionnements) {
        List<LigneApprovResponse> lignes =
            approvIdToLignes.getOrDefault(approv.getId(), new ArrayList<>());

        // Calculer les totaux
        BigDecimal approvTotalUsd = lignes.stream()
            .map(ligne -> ligne.getPrixachattotal() != null
                ? BigDecimal.valueOf(ligne.getPrixachattotal().doubleValue())
                : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal approvTotalConversion = null;
        if (approv.getTaux() != null && approv.getTaux() > 0) {
          approvTotalConversion = approvTotalUsd.multiply(BigDecimal.valueOf(approv.getTaux()));
        }

        String statutLabel = "En attente";
        if ("VALIDEE".equals(approv.getStatut())) {
          statutLabel = "Validée";
        } else if ("ANNULEE".equals(approv.getStatut())) {
          statutLabel = "Annulée";
        }

        String numBon =
            approv.getNumbonliv() != null ? approv.getNumbonliv() : "N° " + approv.getId();

        // En-tête de l'approvisionnement
        Row approvHeaderRow = sheet.createRow(rowNum++);
        Cell approvHeaderCell = approvHeaderRow.createCell(0);
        approvHeaderCell.setCellValue("BON D'APPROVISIONNEMENT: " + numBon);
        approvHeaderCell.setCellStyle(groupHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 11));

        // Informations de l'approvisionnement
        Row approvInfoRow1 = sheet.createRow(rowNum++);
        approvInfoRow1.createCell(0).setCellValue("Fournisseur:");
        approvInfoRow1.createCell(1)
            .setCellValue(approv.getFournisseurNom() != null ? approv.getFournisseurNom() : "-");
        approvInfoRow1.createCell(6).setCellValue("Service:");
        approvInfoRow1.createCell(7)
            .setCellValue(approv.getPharmacieNom() != null ? approv.getPharmacieNom() : "-");

        Row approvInfoRow2 = sheet.createRow(rowNum++);
        approvInfoRow2.createCell(0).setCellValue("Date BL:");
        approvInfoRow2.createCell(1)
            .setCellValue(approv.getDatebonliv() != null
                ? approv.getDatebonliv().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : "-");
        approvInfoRow2.createCell(6).setCellValue("Date création:");
        approvInfoRow2.createCell(7)
            .setCellValue(approv.getDateCreate() != null
                ? approv.getDateCreate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : "-");

        Row approvInfoRow3 = sheet.createRow(rowNum++);
        if (approv.getEchangeDeviseMonnaie() != null
            && !approv.getEchangeDeviseMonnaie().isEmpty()) {
          approvInfoRow3.createCell(0).setCellValue("Devise:");
          approvInfoRow3.createCell(1).setCellValue(approv.getEchangeDeviseMonnaie());
        }
        approvInfoRow3.createCell(6).setCellValue("Statut:");
        approvInfoRow3.createCell(7).setCellValue(statutLabel);

        if (approv.getTaux() != null && approv.getTaux() > 0) {
          Row approvInfoRow4 = sheet.createRow(rowNum++);
          approvInfoRow4.createCell(0).setCellValue("Taux:");
          approvInfoRow4.createCell(1).setCellValue(approv.getTaux().toString());
        }

        rowNum++; // Ligne vide

        // En-têtes des colonnes pour les lignes de produit
        Row columnHeaderRow = sheet.createRow(rowNum++);
        String[] headers =
            {"N°", "Produit", "Quantité", "Prix unitaire", "Total USD", "Total Conversion"};
        for (int i = 0; i < headers.length; i++) {
          Cell cell = columnHeaderRow.createCell(i);
          cell.setCellValue(headers[i]);
          cell.setCellStyle(headerStyle);
        }

        // Données des lignes de produit
        if (lignes.isEmpty()) {
          Row emptyRow = sheet.createRow(rowNum++);
          Cell emptyCell = emptyRow.createCell(1);
          emptyCell.setCellValue("Aucune ligne pour cet approvisionnement");
          emptyCell.setCellStyle(cellStyle);
          sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 1, 5));
        } else {
          for (int index = 0; index < lignes.size(); index++) {
            LigneApprovResponse ligne = lignes.get(index);
            Row row = sheet.createRow(rowNum++);
            int colNum = 0;

            row.createCell(colNum++).setCellValue(index + 1);
            row.createCell(colNum++)
                .setCellValue(ligne.getProduitNom() != null ? ligne.getProduitNom() : "");

            if (ligne.getQt() != null) {
              Cell qtyCell = row.createCell(colNum++);
              qtyCell.setCellValue(ligne.getQt().doubleValue());
              qtyCell.setCellStyle(numberStyle);
            } else {
              colNum++;
            }

            BigDecimal prixUnitaire = BigDecimal.ZERO;
            BigDecimal totalUsdLigne = BigDecimal.ZERO;
            BigDecimal totalConvLigne = null;

            if (ligne.getQt() != null && ligne.getQt() > 0 && ligne.getPrixachattotal() != null) {
              totalUsdLigne = BigDecimal.valueOf(ligne.getPrixachattotal().doubleValue());
              prixUnitaire = totalUsdLigne.divide(BigDecimal.valueOf(ligne.getQt()), 2,
                  java.math.RoundingMode.HALF_UP);
              if (approvTotalConversion != null && approv.getTaux() != null) {
                totalConvLigne = totalUsdLigne.multiply(BigDecimal.valueOf(approv.getTaux()));
              }
            } else if (ligne.getPrixachat() != null) {
              prixUnitaire = BigDecimal.valueOf(ligne.getPrixachat().doubleValue());
              if (ligne.getQt() != null && ligne.getQt() > 0) {
                totalUsdLigne = prixUnitaire.multiply(BigDecimal.valueOf(ligne.getQt()));
                if (approvTotalConversion != null && approv.getTaux() != null) {
                  totalConvLigne = totalUsdLigne.multiply(BigDecimal.valueOf(approv.getTaux()));
                }
              }
            }

            Cell prixCell = row.createCell(colNum++);
            prixCell.setCellValue(prixUnitaire.doubleValue());
            prixCell.setCellStyle(numberStyle);

            Cell totalUsdCell = row.createCell(colNum++);
            totalUsdCell.setCellValue(totalUsdLigne.doubleValue());
            totalUsdCell.setCellStyle(numberStyle);

            Cell totalConvCell = row.createCell(colNum++);
            if (totalConvLigne != null) {
              totalConvCell.setCellValue(totalConvLigne.doubleValue());
              totalConvCell.setCellStyle(numberStyle);
            } else {
              totalConvCell.setCellValue("");
              totalConvCell.setCellStyle(cellStyle);
            }

            // Appliquer le style aux cellules
            for (int i = 0; i < headers.length; i++) {
              Cell cell = row.getCell(i);
              if (cell != null && cell.getCellStyle() == null) {
                cell.setCellStyle(cellStyle);
              }
            }
          }
        }

        // Totaux de l'approvisionnement
        rowNum++; // Ligne vide
        Row totalRow = sheet.createRow(rowNum++);
        Cell totalLabelCell = totalRow.createCell(4);
        totalLabelCell.setCellValue("Total USD:");
        totalLabelCell.setCellStyle(headerStyle);
        Cell totalUsdCell = totalRow.createCell(5);
        totalUsdCell.setCellValue(approvTotalUsd.doubleValue());
        totalUsdCell.setCellStyle(headerStyle);

        if (approvTotalConversion != null) {
          Row totalConvRow = sheet.createRow(rowNum++);
          Cell totalConvLabelCell = totalConvRow.createCell(4);
          totalConvLabelCell.setCellValue("Total Conversion:");
          totalConvLabelCell.setCellStyle(headerStyle);
          Cell totalConvValueCell = totalConvRow.createCell(5);
          totalConvValueCell.setCellValue(approvTotalConversion.doubleValue());
          totalConvValueCell.setCellStyle(headerStyle);
        }

        rowNum++; // Ligne vide entre les approvisionnements
      }

      // Ajuster la largeur des colonnes
      for (int i = 0; i < 6; i++) {
        sheet.autoSizeColumn(i);
        // Limiter la largeur maximale à 50
        if (sheet.getColumnWidth(i) > 50 * 256) {
          sheet.setColumnWidth(i, 50 * 256);
        }
      }

      workbook.write(out);
      log.info("✅ [ReportService] Excel exporté avec succès: {} bytes", out.size());
      return out.toByteArray();

    } catch (IOException e) {
      log.error("Erreur lors de la génération du rapport Excel liste approvisionnements", e);
      throw new RuntimeException("Impossible de générer le rapport Excel", e);
    }
  }

  /**
   * Génère un rapport PDF de la liste des fournisseurs.
   *
   * @param fournisseurs Liste des fournisseurs à inclure dans le rapport
   * @return PDF en tant que byte[]
   * @throws JRException Si une erreur survient lors de la génération du rapport
   */
  public byte[] generateFournisseursReport(List<FournisseurResponse> fournisseurs)
      throws JRException {

    log.info("🚀 [ReportService] Génération du rapport fournisseurs: {} fournisseurs",
        fournisseurs.size());

    // Charger le template .jrxml
    ClassPathResource resource = new ClassPathResource(REPORTS_DIR + FOURNISSEURS_REPORT);
    if (!resource.exists()) {
      String errorMsg =
          String.format("Template de rapport introuvable: %s%s", REPORTS_DIR, FOURNISSEURS_REPORT);
      log.error(errorMsg);
      throw new RuntimeException(errorMsg);
    }
    log.info("✅ [ReportService] Template de rapport trouvé: {}", resource.getPath());

    // Préparer les paramètres du rapport
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("TITRE_RAPPORT", "Liste des Fournisseurs");
    parameters.put("DATE_GENERATION",
        LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    parameters.put("TOTAL_FOURNISSEURS", fournisseurs.size());

    // Charger et compiler le template
    try (InputStream templateStream = resource.getInputStream()) {
      log.info("📄 [ReportService] Compilation du template JasperReports...");
      JasperReport jasperReport = JasperCompileManager.compileReport(templateStream);
      log.info("✅ [ReportService] Template compilé avec succès");

      // Créer la source de données
      JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(fournisseurs);
      log.info("✅ [ReportService] Source de données créée: {} fournisseurs", fournisseurs.size());

      // Remplir le rapport
      log.info("🖨️ [ReportService] Remplissage du rapport avec les données...");
      JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
      log.info("✅ [ReportService] Rapport rempli avec succès. Nombre de pages: {}",
          jasperPrint.getPages().size());

      // Exporter en PDF
      log.info("📤 [ReportService] Export du rapport en PDF...");
      byte[] pdfBytes = JasperExportManager.exportReportToPdf(jasperPrint);
      log.info("✅ [ReportService] PDF exporté avec succès: {} bytes", pdfBytes.length);
      return pdfBytes;
    } catch (IOException e) {
      log.error("Erreur lors de la lecture du template de rapport", e);
      throw new RuntimeException("Impossible de charger le template de rapport", e);
    }
  }

  /**
   * Génère un rapport Excel de la liste des fournisseurs.
   *
   * @param fournisseurs Liste des fournisseurs à inclure dans le rapport
   * @return Excel en tant que byte[]
   */
  public byte[] generateFournisseursReportExcel(List<FournisseurResponse> fournisseurs) {

    log.info("🚀 [ReportService] Génération du rapport Excel fournisseurs: {} fournisseurs",
        fournisseurs.size());

    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {

      Sheet sheet = workbook.createSheet("Liste des Fournisseurs");

      // Style pour l'en-tête
      CellStyle headerStyle = workbook.createCellStyle();
      Font headerFont = workbook.createFont();
      headerFont.setBold(true);
      headerFont.setFontHeightInPoints((short) 12);
      headerStyle.setFont(headerFont);
      headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
      headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      headerStyle.setBorderBottom(BorderStyle.THIN);
      headerStyle.setBorderTop(BorderStyle.THIN);
      headerStyle.setBorderLeft(BorderStyle.THIN);
      headerStyle.setBorderRight(BorderStyle.THIN);

      // Style pour les cellules
      CellStyle cellStyle = workbook.createCellStyle();
      cellStyle.setBorderBottom(BorderStyle.THIN);
      cellStyle.setBorderTop(BorderStyle.THIN);
      cellStyle.setBorderLeft(BorderStyle.THIN);
      cellStyle.setBorderRight(BorderStyle.THIN);

      int rowNum = 0;

      // En-tête du rapport
      Row headerRow = sheet.createRow(rowNum++);
      Cell headerCell = headerRow.createCell(0);
      headerCell.setCellValue("Rapport des Fournisseurs");
      headerCell.setCellStyle(headerStyle);
      sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));

      rowNum++; // Ligne vide

      // Informations du rapport
      Row infoRow = sheet.createRow(rowNum++);
      infoRow.createCell(0).setCellValue("Date:");
      infoRow.createCell(1)
          .setCellValue(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

      Row totalRow = sheet.createRow(rowNum++);
      totalRow.createCell(0).setCellValue("Total:");
      totalRow.createCell(1).setCellValue(fournisseurs.size() + " fournisseur(s)");

      rowNum++; // Ligne vide

      // En-têtes des colonnes
      Row columnHeaderRow = sheet.createRow(rowNum++);
      String[] headers = {"ID", "Nom", "Téléphone", "Email", "Adresse"};
      for (int i = 0; i < headers.length; i++) {
        Cell cell = columnHeaderRow.createCell(i);
        cell.setCellValue(headers[i]);
        cell.setCellStyle(headerStyle);
      }

      // Données
      for (FournisseurResponse fournisseur : fournisseurs) {
        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue(fournisseur.getId() != null ? fournisseur.getId() : 0);
        row.createCell(1).setCellValue(fournisseur.getNom() != null ? fournisseur.getNom() : "");
        row.createCell(2)
            .setCellValue(fournisseur.getTelephone() != null ? fournisseur.getTelephone() : "");
        row.createCell(3)
            .setCellValue(fournisseur.getEmail() != null ? fournisseur.getEmail() : "");
        row.createCell(4)
            .setCellValue(fournisseur.getAdresse() != null ? fournisseur.getAdresse() : "");

        for (int i = 0; i < headers.length; i++) {
          row.getCell(i).setCellStyle(cellStyle);
        }
      }

      // Auto-size des colonnes
      for (int i = 0; i < headers.length; i++) {
        sheet.autoSizeColumn(i);
        if (sheet.getColumnWidth(i) > 50 * 256) {
          sheet.setColumnWidth(i, 50 * 256);
        }
      }

      workbook.write(out);
      log.info("✅ [ReportService] Excel exporté avec succès: {} bytes", out.size());
      return out.toByteArray();

    } catch (IOException e) {
      log.error("Erreur lors de la génération du rapport Excel fournisseurs", e);
      throw new RuntimeException("Impossible de générer le rapport Excel", e);
    }
  }

  /**
   * Génère un rapport PDF d'une requisition individuelle (sans prix).
   *
   * @param requisition La requisition à inclure dans le rapport
   * @param lignes Les lignes de la requisition
   * @return PDF en tant que byte[]
   * @throws JRException Si une erreur survient lors de la génération du rapport
   */
  public byte[] generateRequisitionReport(RequisitionResponse requisition,
      List<LigneRequisitionReportDTO> lignes) throws JRException {

    log.info("🚀 [ReportService] Génération du rapport requisition: requisitionId={}, {} lignes",
        requisition.getId(), lignes.size());

    // Charger le template .jrxml
    ClassPathResource resource = new ClassPathResource(REPORTS_DIR + REQUISITION_REPORT);
    if (!resource.exists()) {
      String errorMsg =
          String.format("Template de rapport introuvable: %s%s", REPORTS_DIR, REQUISITION_REPORT);
      log.error(errorMsg);
      throw new RuntimeException(errorMsg);
    }
    log.info("✅ [ReportService] Template de rapport trouvé: {}", resource.getPath());

    // Préparer les paramètres du rapport
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("REQUISITION_ID", requisition.getId());
    parameters.put("PHARMACIE_NOM",
        requisition.getPharmacieNom() != null ? requisition.getPharmacieNom() : "-");
    parameters.put("PHARMACIE_STOCK_NOM",
        requisition.getPharmacieStockNom() != null ? requisition.getPharmacieStockNom() : "-");
    parameters.put("STATUT", requisition.getStatut() != null ? requisition.getStatut() : "-");
    parameters.put("DATE_CREATION",
        requisition.getDateCreate() != null
            ? requisition.getDateCreate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            : "-");
    parameters.put("COMMENTAIRE",
        requisition.getCommentaire() != null ? requisition.getCommentaire() : "");
    parameters.put("URGENT",
        requisition.getUrgent() != null && requisition.getUrgent() ? "OUI" : "NON");
    parameters.put("DATE_GENERATION",
        LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    parameters.put("TOTAL_LIGNES", lignes.size());
    parameters.put("WITH_PRICE", false); // Sans prix par défaut

    // Charger et compiler le template
    try (InputStream templateStream = resource.getInputStream()) {
      log.info("📄 [ReportService] Compilation du template JasperReports...");
      JasperReport jasperReport = JasperCompileManager.compileReport(templateStream);
      log.info("✅ [ReportService] Template compilé avec succès");

      // Créer la source de données avec les lignes
      JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(lignes);
      log.info("✅ [ReportService] Source de données créée: {} lignes", lignes.size());

      // Remplir le rapport
      log.info("🖨️ [ReportService] Remplissage du rapport avec les données...");
      JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
      log.info("✅ [ReportService] Rapport rempli avec succès. Nombre de pages: {}",
          jasperPrint.getPages().size());

      // Exporter en PDF
      log.info("📤 [ReportService] Export du rapport en PDF...");
      byte[] pdfBytes = JasperExportManager.exportReportToPdf(jasperPrint);
      log.info("✅ [ReportService] PDF exporté avec succès: {} bytes", pdfBytes.length);
      return pdfBytes;
    } catch (IOException e) {
      log.error("Erreur lors de la lecture du template de rapport", e);
      throw new RuntimeException("Impossible de charger le template de rapport", e);
    }
  }

  /**
   * Génère un rapport PDF d'une requisition individuelle (avec prix). Note: Pour l'instant,
   * identique au rapport sans prix car les prix ne sont pas stockés dans les lignes de requisition.
   *
   * @param requisition La requisition à inclure dans le rapport
   * @param lignes Les lignes de la requisition
   * @return PDF en tant que byte[]
   * @throws JRException Si une erreur survient lors de la génération du rapport
   */
  public byte[] generateRequisitionReportWithPrice(RequisitionResponse requisition,
      List<LigneRequisitionReportDTO> lignes) throws JRException {

    log.info(
        "🚀 [ReportService] Génération du rapport requisition avec prix: requisitionId={}, {} lignes",
        requisition.getId(), lignes.size());

    // Charger le template .jrxml (même template, mais avec WITH_PRICE = true)
    ClassPathResource resource = new ClassPathResource(REPORTS_DIR + REQUISITION_REPORT);
    if (!resource.exists()) {
      String errorMsg =
          String.format("Template de rapport introuvable: %s%s", REPORTS_DIR, REQUISITION_REPORT);
      log.error(errorMsg);
      throw new RuntimeException(errorMsg);
    }
    log.info("✅ [ReportService] Template de rapport trouvé: {}", resource.getPath());

    // Préparer les paramètres du rapport
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("REQUISITION_ID", requisition.getId());
    parameters.put("PHARMACIE_NOM",
        requisition.getPharmacieNom() != null ? requisition.getPharmacieNom() : "-");
    parameters.put("PHARMACIE_STOCK_NOM",
        requisition.getPharmacieStockNom() != null ? requisition.getPharmacieStockNom() : "-");
    parameters.put("STATUT", requisition.getStatut() != null ? requisition.getStatut() : "-");
    parameters.put("DATE_CREATION",
        requisition.getDateCreate() != null
            ? requisition.getDateCreate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            : "-");
    parameters.put("COMMENTAIRE",
        requisition.getCommentaire() != null ? requisition.getCommentaire() : "");
    parameters.put("URGENT",
        requisition.getUrgent() != null && requisition.getUrgent() ? "OUI" : "NON");
    parameters.put("DATE_GENERATION",
        LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    parameters.put("TOTAL_LIGNES", lignes.size());
    parameters.put("WITH_PRICE", true); // Avec prix

    // Charger et compiler le template
    try (InputStream templateStream = resource.getInputStream()) {
      log.info("📄 [ReportService] Compilation du template JasperReports...");
      JasperReport jasperReport = JasperCompileManager.compileReport(templateStream);
      log.info("✅ [ReportService] Template compilé avec succès");

      // Créer la source de données avec les lignes
      JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(lignes);
      log.info("✅ [ReportService] Source de données créée: {} lignes", lignes.size());

      // Remplir le rapport
      log.info("🖨️ [ReportService] Remplissage du rapport avec les données...");
      JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
      log.info("✅ [ReportService] Rapport rempli avec succès. Nombre de pages: {}",
          jasperPrint.getPages().size());

      // Exporter en PDF
      log.info("📤 [ReportService] Export du rapport en PDF...");
      byte[] pdfBytes = JasperExportManager.exportReportToPdf(jasperPrint);
      log.info("✅ [ReportService] PDF exporté avec succès: {} bytes", pdfBytes.length);
      return pdfBytes;
    } catch (IOException e) {
      log.error("Erreur lors de la lecture du template de rapport", e);
      throw new RuntimeException("Impossible de charger le template de rapport", e);
    }
  }

  /**
   * Génère un rapport PDF de la liste des requisitions avec leurs lignes détaillées.
   *
   * @param requisitions Liste des requisitions à inclure dans le rapport
   * @param requisitionIdToLignes Map des IDs de requisition vers leurs lignes
   * @param pharmacieNom Nom de la pharmacie/service
   * @return PDF en tant que byte[]
   * @throws JRException Si une erreur survient lors de la génération du rapport
   */
  public byte[] generateRequisitionsListReport(List<RequisitionResponse> requisitions,
      Map<Long, List<LigneRequisitionReportDTO>> requisitionIdToLignes, String pharmacieNom)
      throws JRException {

    log.info("🚀 [ReportService] Génération du rapport liste requisitions: {} requisitions",
        requisitions.size());

    // Charger le template .jrxml
    ClassPathResource resource = new ClassPathResource(REPORTS_DIR + REQUISITIONS_LIST_REPORT);
    if (!resource.exists()) {
      String errorMsg = String.format("Template de rapport introuvable: %s%s", REPORTS_DIR,
          REQUISITIONS_LIST_REPORT);
      log.error(errorMsg);
      throw new RuntimeException(errorMsg);
    }
    log.info("✅ [ReportService] Template de rapport trouvé: {}", resource.getPath());

    // Créer une structure plate : une ligne de rapport par ligne de requisition
    // Chaque ligne contient les infos de la requisition + les infos de la ligne
    List<RequisitionLigneFlatReportDTO> reportData = new ArrayList<>();

    for (RequisitionResponse req : requisitions) {
      List<LigneRequisitionReportDTO> lignes =
          requisitionIdToLignes.getOrDefault(req.getId(), new ArrayList<>());

      String reqPharmacieNom = req.getPharmacieNom() != null ? req.getPharmacieNom() : "-";
      String reqPharmacieStockNom =
          req.getPharmacieStockNom() != null ? req.getPharmacieStockNom() : "-";
      String reqStatut = req.getStatut() != null ? req.getStatut() : "-";
      String reqDateCreation = req.getDateCreate() != null
          ? req.getDateCreate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
          : "-";
      String reqCommentaire = req.getCommentaire() != null ? req.getCommentaire() : "";
      Boolean reqUrgent = req.getUrgent() != null ? req.getUrgent() : false;

      // Créer une ligne plate pour chaque ligne de requisition
      if (lignes.isEmpty()) {
        // Si pas de lignes, créer quand même une ligne avec les infos de la requisition
        reportData.add(RequisitionLigneFlatReportDTO.builder().requisitionId(req.getId())
            .pharmacieNom(reqPharmacieNom).pharmacieStockNom(reqPharmacieStockNom).statut(reqStatut)
            .dateCreation(reqDateCreation).commentaire(reqCommentaire).urgent(reqUrgent)
            .numeroLigne(0).nomCommercial(null).nomScientifique(null).forme(null).dosage(null)
            .conditionnement(null).quantite(null).quantiteEnStock(null).prixUnitaire(null)
            .total(null).build());
      } else {
        for (int index = 0; index < lignes.size(); index++) {
          LigneRequisitionReportDTO ligne = lignes.get(index);
          reportData.add(RequisitionLigneFlatReportDTO.builder().requisitionId(req.getId())
              .pharmacieNom(reqPharmacieNom).pharmacieStockNom(reqPharmacieStockNom)
              .statut(reqStatut).dateCreation(reqDateCreation).commentaire(reqCommentaire)
              .urgent(reqUrgent).numeroLigne(index + 1).nomCommercial(ligne.getNomCommercial())
              .nomScientifique(ligne.getNomScientifique()).forme(ligne.getForme())
              .dosage(ligne.getDosage()).conditionnement(ligne.getConditionnement())
              .quantite(ligne.getQuantite()).quantiteEnStock(ligne.getQuantiteEnStock())
              .prixUnitaire(ligne.getPrixUnitaire()).total(ligne.getTotal()).build());
        }
      }
    }

    // Préparer les paramètres du rapport
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("TITRE_RAPPORT", "Liste des Requisitions");
    parameters.put("PHARMACIE_NOM", pharmacieNom != null ? pharmacieNom : "Tous les services");
    parameters.put("DATE_GENERATION",
        LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    parameters.put("TOTAL_REQUISITIONS", requisitions.size());

    // Charger et compiler le template
    try (InputStream templateStream = resource.getInputStream()) {
      log.info("📄 [ReportService] Compilation du template JasperReports...");
      JasperReport jasperReport = JasperCompileManager.compileReport(templateStream);
      log.info("✅ [ReportService] Template compilé avec succès");

      // Créer la source de données avec la structure plate
      JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);
      log.info("✅ [ReportService] Source de données créée: {} lignes ({} requisitions)",
          reportData.size(), requisitions.size());

      // Remplir le rapport
      log.info("🖨️ [ReportService] Remplissage du rapport avec les données...");
      JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
      log.info("✅ [ReportService] Rapport rempli avec succès. Nombre de pages: {}",
          jasperPrint.getPages().size());

      // Exporter en PDF
      log.info("📤 [ReportService] Export du rapport en PDF...");
      byte[] pdfBytes = JasperExportManager.exportReportToPdf(jasperPrint);
      log.info("✅ [ReportService] PDF exporté avec succès: {} bytes", pdfBytes.length);
      return pdfBytes;
    } catch (IOException e) {
      log.error("Erreur lors de la lecture du template de rapport", e);
      throw new RuntimeException("Impossible de charger le template de rapport", e);
    }
  }

  /**
   * Génère un rapport Excel de la liste des requisitions.
   *
   * @param requisitions Liste des requisitions à inclure dans le rapport
   * @param pharmacieNom Nom de la pharmacie/service
   * @return Excel en tant que byte[]
   */
  public byte[] generateRequisitionsReportExcel(List<RequisitionResponse> requisitions,
      String pharmacieNom) {

    log.info("🚀 [ReportService] Génération du rapport Excel requisitions: {} requisitions",
        requisitions.size());

    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {

      Sheet sheet = workbook.createSheet("Liste des Requisitions");

      // Style pour l'en-tête
      CellStyle headerStyle = workbook.createCellStyle();
      Font headerFont = workbook.createFont();
      headerFont.setBold(true);
      headerFont.setFontHeightInPoints((short) 12);
      headerStyle.setFont(headerFont);
      headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
      headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      headerStyle.setBorderBottom(BorderStyle.THIN);
      headerStyle.setBorderTop(BorderStyle.THIN);
      headerStyle.setBorderLeft(BorderStyle.THIN);
      headerStyle.setBorderRight(BorderStyle.THIN);

      // Style pour les cellules
      CellStyle cellStyle = workbook.createCellStyle();
      cellStyle.setBorderBottom(BorderStyle.THIN);
      cellStyle.setBorderTop(BorderStyle.THIN);
      cellStyle.setBorderLeft(BorderStyle.THIN);
      cellStyle.setBorderRight(BorderStyle.THIN);

      int rowNum = 0;

      // En-tête du rapport
      Row headerRow = sheet.createRow(rowNum++);
      Cell headerCell = headerRow.createCell(0);
      headerCell.setCellValue("Rapport des Requisitions");
      headerCell.setCellStyle(headerStyle);
      sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

      rowNum++; // Ligne vide

      // Informations du rapport
      Row infoRow = sheet.createRow(rowNum++);
      infoRow.createCell(0).setCellValue("Service:");
      infoRow.createCell(1).setCellValue(pharmacieNom != null ? pharmacieNom : "Tous les services");
      Row dateRow = sheet.createRow(rowNum++);
      dateRow.createCell(0).setCellValue("Date:");
      dateRow.createCell(1)
          .setCellValue(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
      Row totalRow = sheet.createRow(rowNum++);
      totalRow.createCell(0).setCellValue("Total:");
      totalRow.createCell(1).setCellValue(requisitions.size() + " requisition(s)");

      rowNum++; // Ligne vide

      // En-têtes des colonnes
      Row columnHeaderRow = sheet.createRow(rowNum++);
      String[] headers = {"ID", "Pharmacie Demandeur", "Pharmacie Stock", "Statut", "Date Création",
          "Urgent", "Commentaire"};
      for (int i = 0; i < headers.length; i++) {
        Cell cell = columnHeaderRow.createCell(i);
        cell.setCellValue(headers[i]);
        cell.setCellStyle(headerStyle);
      }

      // Données
      for (RequisitionResponse requisition : requisitions) {
        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue(requisition.getId() != null ? requisition.getId() : 0);
        row.createCell(1).setCellValue(
            requisition.getPharmacieNom() != null ? requisition.getPharmacieNom() : "-");
        row.createCell(2).setCellValue(
            requisition.getPharmacieStockNom() != null ? requisition.getPharmacieStockNom() : "-");
        row.createCell(3)
            .setCellValue(requisition.getStatut() != null ? requisition.getStatut() : "-");
        row.createCell(4).setCellValue(requisition.getDateCreate() != null
            ? requisition.getDateCreate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            : "-");
        row.createCell(5).setCellValue(
            requisition.getUrgent() != null && requisition.getUrgent() ? "OUI" : "NON");
        row.createCell(6)
            .setCellValue(requisition.getCommentaire() != null ? requisition.getCommentaire() : "");

        for (int i = 0; i < headers.length; i++) {
          row.getCell(i).setCellStyle(cellStyle);
        }
      }

      // Auto-size des colonnes
      for (int i = 0; i < headers.length; i++) {
        sheet.autoSizeColumn(i);
        if (sheet.getColumnWidth(i) > 50 * 256) {
          sheet.setColumnWidth(i, 50 * 256);
        }
      }

      workbook.write(out);
      log.info("✅ [ReportService] Excel exporté avec succès: {} bytes", out.size());
      return out.toByteArray();

    } catch (IOException e) {
      log.error("Erreur lors de la génération du rapport Excel requisitions", e);
      throw new RuntimeException("Impossible de générer le rapport Excel", e);
    }
  }

  /**
   * Génère un rapport PDF d'un transfert individuel (bon de transfert).
   *
   * @param transfert Le transfert à inclure dans le rapport
   * @param lignes Les lignes du transfert
   * @return PDF en tant que byte[]
   * @throws JRException Si une erreur survient lors de la génération du rapport
   */
  public byte[] generateTransfertReport(TransfertStockResponse transfert,
      List<LigneTransfertReportDTO> lignes) throws JRException {

    log.info("🚀 [ReportService] Génération du rapport transfert: transfertId={}, {} lignes",
        transfert.getId(), lignes.size());

    // Charger le template .jrxml
    ClassPathResource resource = new ClassPathResource(REPORTS_DIR + TRANSFERT_REPORT);
    if (!resource.exists()) {
      String errorMsg =
          String.format("Template de rapport introuvable: %s%s", REPORTS_DIR, TRANSFERT_REPORT);
      log.error(errorMsg);
      throw new RuntimeException(errorMsg);
    }
    log.info("✅ [ReportService] Template de rapport trouvé: {}", resource.getPath());

    // Préparer les paramètres du rapport
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("TRANSFERT_ID", transfert.getId());
    parameters.put("REQUISITION_NUMERO",
        transfert.getRequisitionNumero() != null ? transfert.getRequisitionNumero().toString()
            : "-");
    parameters.put("PHARMACIE_DEMANDEUR_NOM",
        transfert.getPharmacieDemandeurNom() != null ? transfert.getPharmacieDemandeurNom() : "-");
    parameters.put("STATUT", transfert.getStatut() != null ? transfert.getStatut() : "-");
    parameters.put("DATE_CREATION",
        transfert.getDateCreate() != null
            ? transfert.getDateCreate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            : "-");
    parameters.put("DATE_GENERATION",
        LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    parameters.put("TOTAL_LIGNES", lignes.size());

    // Charger et compiler le template
    try (InputStream templateStream = resource.getInputStream()) {
      log.info("📄 [ReportService] Compilation du template JasperReports...");
      JasperReport jasperReport = JasperCompileManager.compileReport(templateStream);
      log.info("✅ [ReportService] Template compilé avec succès");

      // Créer la source de données avec les lignes
      JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(lignes);
      log.info("✅ [ReportService] Source de données créée: {} lignes", lignes.size());

      // Remplir le rapport
      log.info("🖨️ [ReportService] Remplissage du rapport avec les données...");
      JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
      log.info("✅ [ReportService] Rapport rempli avec succès. Nombre de pages: {}",
          jasperPrint.getPages().size());

      // Exporter en PDF
      log.info("📤 [ReportService] Export du rapport en PDF...");
      byte[] pdfBytes = JasperExportManager.exportReportToPdf(jasperPrint);
      log.info("✅ [ReportService] PDF exporté avec succès: {} bytes", pdfBytes.length);
      return pdfBytes;
    } catch (IOException e) {
      log.error("Erreur lors de la lecture du template de rapport", e);
      throw new RuntimeException("Impossible de charger le template de rapport", e);
    }
  }

  /**
   * Génère un rapport PDF de la liste des transferts avec leurs lignes détaillées.
   *
   * @param transferts Liste des transferts à inclure dans le rapport
   * @param transfertIdToLignes Map des IDs de transfert vers leurs lignes
   * @param pharmacieNom Nom de la pharmacie/service
   * @return PDF en tant que byte[]
   * @throws JRException Si une erreur survient lors de la génération du rapport
   */
  public byte[] generateTransfertsListReport(List<TransfertStockResponse> transferts,
      Map<Long, List<LigneTransfertStockResponse>> transfertIdToLignes, String pharmacieNom)
      throws JRException {

    log.info("🚀 [ReportService] Génération du rapport liste transferts: {} transferts",
        transferts.size());

    // Charger le template .jrxml
    ClassPathResource resource = new ClassPathResource(REPORTS_DIR + TRANSFERTS_LIST_REPORT);
    if (!resource.exists()) {
      String errorMsg = String.format("Template de rapport introuvable: %s%s", REPORTS_DIR,
          TRANSFERTS_LIST_REPORT);
      log.error(errorMsg);
      throw new RuntimeException(errorMsg);
    }
    log.info("✅ [ReportService] Template de rapport trouvé: {}", resource.getPath());

    // Créer une structure plate : une ligne de rapport par ligne de transfert
    // Chaque ligne contient les infos du transfert + les infos de la ligne
    List<TransfertLigneFlatReportDTO> reportData = new ArrayList<>();

    for (TransfertStockResponse transf : transferts) {
      List<LigneTransfertStockResponse> lignes =
          transfertIdToLignes.getOrDefault(transf.getId(), new ArrayList<>());

      Long transfRequisitionNumero = transf.getRequisitionNumero();
      String transfPharmacieDemandeurNom =
          transf.getPharmacieDemandeurNom() != null ? transf.getPharmacieDemandeurNom() : "-";
      String transfStatut = transf.getStatut() != null ? transf.getStatut() : "-";
      String transfDateCreation = transf.getDateCreate() != null
          ? transf.getDateCreate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
          : "-";

      // Créer une ligne plate pour chaque ligne de transfert
      if (lignes.isEmpty()) {
        // Si pas de lignes, créer quand même une ligne avec les infos du transfert
        reportData.add(TransfertLigneFlatReportDTO.builder().transfertId(transf.getId())
            .requisitionNumero(transfRequisitionNumero)
            .pharmacieDemandeurNom(transfPharmacieDemandeurNom).statut(transfStatut)
            .dateCreation(transfDateCreation).numeroLigne(0).nomCommercial(null)
            .nomScientifique(null).forme(null).dosage(null).conditionnement(null)
            .quantiteDemandee(null).quantite(null).quantiteEnStock(null).build());
      } else {
        for (int index = 0; index < lignes.size(); index++) {
          LigneTransfertStockResponse ligne = lignes.get(index);
          if (ligne == null) {
            log.warn("[ReportService] ⚠️ Ligne null trouvée pour le transfert {} à l'index {}",
                transf.getId(), index);
            continue;
          }
          reportData.add(TransfertLigneFlatReportDTO.builder().transfertId(transf.getId())
              .requisitionNumero(transfRequisitionNumero)
              .pharmacieDemandeurNom(transfPharmacieDemandeurNom).statut(transfStatut)
              .dateCreation(transfDateCreation).numeroLigne(index + 1)
              .nomCommercial(
                  ligne.getStockNomCommercial() != null ? ligne.getStockNomCommercial() : "-")
              .nomScientifique(
                  ligne.getStockNomScientifique() != null ? ligne.getStockNomScientifique() : "-")
              .forme(ligne.getStockForme() != null ? ligne.getStockForme() : "-")
              .dosage(ligne.getStockDosage() != null ? ligne.getStockDosage() : "-")
              .conditionnement(
                  ligne.getStockConditionnement() != null ? ligne.getStockConditionnement() : "-")
              .quantiteDemandee(
                  ligne.getQuantiteDemandee() != null ? ligne.getQuantiteDemandee() : null)
              .quantite(ligne.getQuantite() != null ? ligne.getQuantite() : null).quantiteEnStock(
                  ligne.getQuantiteEnStock() != null ? ligne.getQuantiteEnStock() : null)
              .build());
        }
      }
    }

    // Préparer les paramètres du rapport
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("TITRE_RAPPORT", "Liste des Transferts");
    parameters.put("PHARMACIE_NOM", pharmacieNom != null ? pharmacieNom : "Tous les services");
    parameters.put("DATE_GENERATION",
        LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    parameters.put("TOTAL_TRANSFERTS", transferts.size());

    // Charger et compiler le template
    try (InputStream templateStream = resource.getInputStream()) {
      log.info("📄 [ReportService] Compilation du template JasperReports...");
      JasperReport jasperReport = JasperCompileManager.compileReport(templateStream);
      log.info("✅ [ReportService] Template compilé avec succès");

      // Créer la source de données avec la structure plate
      JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);
      log.info("✅ [ReportService] Source de données créée: {} lignes ({} transferts)",
          reportData.size(), transferts.size());

      // Remplir le rapport
      log.info("🖨️ [ReportService] Remplissage du rapport avec les données...");
      JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
      log.info("✅ [ReportService] Rapport rempli avec succès. Nombre de pages: {}",
          jasperPrint.getPages().size());

      // Exporter en PDF
      log.info("📤 [ReportService] Export du rapport en PDF...");
      byte[] pdfBytes = JasperExportManager.exportReportToPdf(jasperPrint);
      log.info("✅ [ReportService] PDF exporté avec succès: {} bytes", pdfBytes.length);
      return pdfBytes;
    } catch (IOException e) {
      log.error("Erreur lors de la lecture du template de rapport", e);
      throw new RuntimeException("Impossible de charger le template de rapport", e);
    }
  }

  /**
   * Génère un rapport Excel de la liste des transferts.
   *
   * @param transferts Liste des transferts à inclure dans le rapport
   * @param pharmacieNom Nom de la pharmacie/service
   * @return Excel en tant que byte[]
   */
  public byte[] generateTransfertsReportExcel(List<TransfertStockResponse> transferts,
      String pharmacieNom) {

    log.info("🚀 [ReportService] Génération du rapport Excel transferts: {} transferts",
        transferts.size());

    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {

      Sheet sheet = workbook.createSheet("Liste des Transferts");

      // Style pour l'en-tête
      CellStyle headerStyle = workbook.createCellStyle();
      Font headerFont = workbook.createFont();
      headerFont.setBold(true);
      headerFont.setFontHeightInPoints((short) 12);
      headerStyle.setFont(headerFont);
      headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
      headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      headerStyle.setBorderBottom(BorderStyle.THIN);
      headerStyle.setBorderTop(BorderStyle.THIN);
      headerStyle.setBorderLeft(BorderStyle.THIN);
      headerStyle.setBorderRight(BorderStyle.THIN);

      // Style pour les cellules
      CellStyle cellStyle = workbook.createCellStyle();
      cellStyle.setBorderBottom(BorderStyle.THIN);
      cellStyle.setBorderTop(BorderStyle.THIN);
      cellStyle.setBorderLeft(BorderStyle.THIN);
      cellStyle.setBorderRight(BorderStyle.THIN);

      int rowNum = 0;

      // En-tête du rapport
      Row headerRow = sheet.createRow(rowNum++);
      Cell headerCell = headerRow.createCell(0);
      headerCell.setCellValue("Rapport des Transferts");
      headerCell.setCellStyle(headerStyle);
      sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

      rowNum++; // Ligne vide

      // Informations du rapport
      Row infoRow = sheet.createRow(rowNum++);
      infoRow.createCell(0).setCellValue("Service:");
      infoRow.createCell(1).setCellValue(pharmacieNom != null ? pharmacieNom : "Tous les services");
      Row dateRow = sheet.createRow(rowNum++);
      dateRow.createCell(0).setCellValue("Date:");
      dateRow.createCell(1)
          .setCellValue(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
      Row totalRow = sheet.createRow(rowNum++);
      totalRow.createCell(0).setCellValue("Total:");
      totalRow.createCell(1).setCellValue(transferts.size() + " transfert(s)");

      rowNum++; // Ligne vide

      // En-têtes des colonnes
      Row columnHeaderRow = sheet.createRow(rowNum++);
      String[] headers = {"ID", "ID Requisition", "Pharmacie Demandeur", "Statut", "Date Création"};
      for (int i = 0; i < headers.length; i++) {
        Cell cell = columnHeaderRow.createCell(i);
        cell.setCellValue(headers[i]);
        cell.setCellStyle(headerStyle);
      }

      // Données
      for (TransfertStockResponse transfert : transferts) {
        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue(transfert.getId() != null ? transfert.getId() : 0);
        row.createCell(1).setCellValue(
            transfert.getRequisitionNumero() != null ? transfert.getRequisitionNumero() : 0);
        row.createCell(2).setCellValue(
            transfert.getPharmacieDemandeurNom() != null ? transfert.getPharmacieDemandeurNom()
                : "-");
        row.createCell(3).setCellValue(transfert.getStatut() != null ? transfert.getStatut() : "-");
        row.createCell(4)
            .setCellValue(transfert.getDateCreate() != null
                ? transfert.getDateCreate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "-");

        for (int i = 0; i < headers.length; i++) {
          row.getCell(i).setCellStyle(cellStyle);
        }
      }

      // Auto-size des colonnes
      for (int i = 0; i < headers.length; i++) {
        sheet.autoSizeColumn(i);
        if (sheet.getColumnWidth(i) > 50 * 256) {
          sheet.setColumnWidth(i, 50 * 256);
        }
      }

      workbook.write(out);
      log.info("✅ [ReportService] Excel exporté avec succès: {} bytes", out.size());
      return out.toByteArray();

    } catch (IOException e) {
      log.error("Erreur lors de la génération du rapport Excel transferts", e);
      throw new RuntimeException("Impossible de générer le rapport Excel", e);
    }
  }

  /**
   * Génère un rapport PDF d'une sortie pour usage (vente).
   *
   * @param vente La vente à inclure dans le rapport
   * @param lignes Les lignes de la vente
   * @return PDF en tant que byte[]
   * @throws JRException Si une erreur survient lors de la génération du rapport
   */
  public byte[] generateVenteReport(VenteResponse vente, List<LigneVenteResponse> lignes)
      throws JRException {

    log.info("🚀 [ReportService] Génération du rapport vente: venteId={}, {} lignes", vente.getId(),
        lignes.size());

    // Charger le template .jrxml
    ClassPathResource resource = new ClassPathResource(REPORTS_DIR + VENTE_REPORT);
    if (!resource.exists()) {
      String errorMsg =
          String.format("Template de rapport introuvable: %s%s", REPORTS_DIR, VENTE_REPORT);
      log.error(errorMsg);
      throw new RuntimeException(errorMsg);
    }
    log.info("✅ [ReportService] Template de rapport trouvé: {}", resource.getPath());

    // Calculer les statistiques
    BigDecimal totalVente = lignes.stream().map(ligne -> {
      BigDecimal qt =
          ligne.getQt() != null ? BigDecimal.valueOf(ligne.getQt().doubleValue()) : BigDecimal.ZERO;
      BigDecimal prix = ligne.getPrixventes() != null ? ligne.getPrixventes() : BigDecimal.ZERO;
      return qt.multiply(prix);
    }).reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal quantiteTotale = lignes.stream()
        .map(ligne -> ligne.getQt() != null ? BigDecimal.valueOf(ligne.getQt().doubleValue())
            : BigDecimal.ZERO)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    // Préparer les paramètres du rapport
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("VENTE_ID", vente.getId());
    parameters.put("PHARMACIE_NOM",
        vente.getPharmacieNom() != null ? vente.getPharmacieNom() : "-");
    parameters.put("STATUT", vente.getStatut() != null ? vente.getStatut() : "-");
    parameters.put("DATE_CREATION",
        vente.getDateCreate() != null
            ? vente.getDateCreate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            : "-");
    parameters.put("DATE_VALIDATION",
        vente.getDateUpdate() != null
            ? vente.getDateUpdate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            : "-");
    parameters.put("RAISON_SORTIE", vente.getRaisonsortie() != null ? vente.getRaisonsortie() : "");
    parameters.put("DEMANDEUR", vente.getDemandeur() != null ? vente.getDemandeur() : "");
    parameters.put("DATE_GENERATION",
        LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    parameters.put("TOTAL_LIGNES", lignes.size());
    parameters.put("TOTAL_VENTE", totalVente);
    parameters.put("QUANTITE_TOTALE", quantiteTotale);

    // Charger et compiler le template
    try (InputStream templateStream = resource.getInputStream()) {
      log.info("📄 [ReportService] Compilation du template JasperReports...");
      JasperReport jasperReport = JasperCompileManager.compileReport(templateStream);
      log.info("✅ [ReportService] Template compilé avec succès");

      // Créer la source de données avec les lignes
      JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(lignes);
      log.info("✅ [ReportService] Source de données créée: {} lignes", lignes.size());

      // Remplir le rapport
      log.info("🖨️ [ReportService] Remplissage du rapport avec les données...");
      JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
      log.info("✅ [ReportService] Rapport rempli avec succès. Nombre de pages: {}",
          jasperPrint.getPages().size());

      // Exporter en PDF
      byte[] pdfBytes = JasperExportManager.exportReportToPdf(jasperPrint);
      log.info("✅ [ReportService] PDF exporté avec succès: {} bytes", pdfBytes.length);
      return pdfBytes;
    } catch (IOException | JRException e) {
      log.error("Erreur lors de la génération du rapport de vente", e);
      throw new RuntimeException("Impossible de charger le template de rapport", e);
    }
  }

  /**
   * Génère un rapport PDF de la liste des ventes.
   *
   * @param ventes Liste des ventes
   * @param venteIdToLignes Map des IDs de vente vers leurs lignes
   * @param pharmacieNom Nom de la pharmacie/service (optionnel)
   * @return PDF en tant que byte[]
   * @throws JRException Si une erreur survient lors de la génération du rapport
   */
  public byte[] generateVentesListReport(List<VenteResponse> ventes,
      Map<Long, List<LigneVenteResponse>> venteIdToLignes, String pharmacieNom) throws JRException {

    log.info("🚀 [ReportService] Génération du rapport liste ventes: {} ventes",



        ventes.size());

    // Charger le template .jrxml
    ClassPathResource resource = new ClassPathResource(REPORTS_DIR + VENTES_LIST_REPORT);
    if (!resource.exists()) {
      String errorMsg =
          String.format("Template de rapport introuvable: %s%s", REPORTS_DIR, VENTES_LIST_REPORT);
      log.error(errorMsg);
      throw new RuntimeException(errorMsg);
    }
    log.info("✅ [ReportService] Template de rapport trouvé: {}", resource.getPath());

    // Créer une structure plate : une ligne de rapport par ligne de vente
    // Chaque ligne contient les infos de la vente + les infos de la ligne
    List<VenteLigneFlatReportDTO> reportData = new ArrayList<>();

    for (VenteResponse vente : ventes) {
      List<LigneVenteResponse> lignes =
          venteIdToLignes.getOrDefault(vente.getId(), new ArrayList<>());

      String statutLabel = vente.getStatut() != null ? vente.getStatut() : "-";
      String pharmacieNomLocal = vente.getPharmacieNom() != null ? vente.getPharmacieNom() : "-";
      String dateCreation = vente.getDateCreate() != null
          ? vente.getDateCreate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
          : "-";
      String dateValidation = vente.getDateUpdate() != null
          ? vente.getDateUpdate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
          : "";
      String raisonSortie = vente.getRaisonsortie() != null ? vente.getRaisonsortie() : "";
      String demandeur = vente.getDemandeur() != null ? vente.getDemandeur() : "";

      // Créer une ligne plate pour chaque ligne de vente
      if (lignes.isEmpty()) {
        // Si pas de lignes, créer quand même une ligne avec les infos de la vente
        reportData.add(VenteLigneFlatReportDTO.builder().venteId(vente.getId())
            .pharmacieNom(pharmacieNomLocal).statut(statutLabel).dateCreation(dateCreation)
            .dateValidation(dateValidation).raisonSortie(raisonSortie).demandeur(demandeur)
            .numeroLigne(0).produitNom(null).prixventes(null).totalLigne(null).build());
      } else {
        for (int index = 0; index < lignes.size(); index++) {
          LigneVenteResponse ligne = lignes.get(index);
          BigDecimal qt = ligne.getQt() != null ? BigDecimal.valueOf(ligne.getQt().doubleValue())
              : BigDecimal.ZERO;
          BigDecimal prixUnitaire =
              ligne.getPrixventes() != null ? ligne.getPrixventes() : BigDecimal.ZERO;
          BigDecimal totalLigne = qt.multiply(prixUnitaire);
          reportData.add(VenteLigneFlatReportDTO.builder().venteId(vente.getId())
              .pharmacieNom(pharmacieNomLocal).statut(statutLabel).dateCreation(dateCreation)
              .dateValidation(dateValidation).raisonSortie(raisonSortie).demandeur(demandeur)
              .numeroLigne(index + 1).produitNom(ligne.getProduitNom()).qt(ligne.getQt())
              .prixventes(prixUnitaire).totalLigne(totalLigne).build());
        }
      }
    }

    log.info("✅ [ReportService] Structure plate créée: {} lignes (plates) pour {} ventes",
        reportData.size(), ventes.size());

    // Calculer les statistiques globales
    BigDecimal totalGlobal = reportData.stream().map(VenteLigneFlatReportDTO::getTotalLigne)
        .filter(total -> total != null).reduce(BigDecimal.ZERO, BigDecimal::add);
    int totalLignes = reportData.size();

    // Préparer les paramètres du rapport
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("TITRE_RAPPORT", "Liste des Ventes");
    parameters.put("PHARMACIE_NOM", pharmacieNom != null ? pharmacieNom : "Tous les services");
    parameters.put("DATE_GENERATION",
        LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    parameters.put("TOTAL_VENTES", ventes.size());
    parameters.put("TOTAL_LIGNES", totalLignes);
    parameters.put("TOTAL_GLOBAL", totalGlobal);


    // Charger et compiler le template
    try (InputStream templateStream = resource.getInputStream()) {
      log.info("📄 [ReportService] Compilation du template JasperReports...");
      JasperReport jasperReport = JasperCompileManager.compileReport(templateStream);
      log.info("✅ [ReportService] Template compilé avec succès");

      // Créer la source de données (structure plate)
      JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);
      log.info("✅ [ReportService] Source de données créée: {} lignes plates", reportData.size());

      // Remplir le rapport
      log.info("🖨️ [ReportService] Remplissage du rapport avec les données...");
      JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
      log.info("✅ [ReportService] Rapport rempli avec succès. Nombre de pages: {}",
          jasperPrint.getPages().size());

      // Exporter en PDF
      log.info("📤 [ReportService] Export du rapport en PDF...");
      byte[] pdfBytes = JasperExportManager.exportReportToPdf(jasperPrint);
      log.info("✅ [ReportService] PDF exporté avec succès: {} bytes", pdfBytes.length);
      return pdfBytes;
    } catch (IOException | JRException e) {
      log.error("Erreur lors de la génération du rapport de liste ventes", e);
      throw new RuntimeException("Impossible de charger le template de rapport", e);
    }
  }

  /**
   * Génère un rapport PDF d'un inventaire.
   *
   * @param inventaire L'inventaire à inclure dans le rapport
   * @param lignes Liste des lignes d'inventaire
   * @return PDF en tant que byte[]
   * @throws JRException Si une erreur survient lors de la génération du rapport
   */
  public byte[] generateInventaireReport(InventaireResponse inventaire,
      List<LigneInventaireResponse> lignes) throws JRException {

    log.info("🚀 [ReportService] Génération du rapport inventaire: inventaireId={}, {} lignes",
        inventaire.getId(), lignes.size());

    // Charger le template .jrxml
    ClassPathResource resource = new ClassPathResource(REPORTS_DIR + INVENTAIRE_REPORT);
    if (!resource.exists()) {
      String errorMsg =
          String.format("Template de rapport introuvable: %s%s", REPORTS_DIR, INVENTAIRE_REPORT);
      log.error(errorMsg);
      throw new RuntimeException(errorMsg);
    }
    log.info("✅ [ReportService] Template de rapport trouvé: {}", resource.getPath());

    // Préparer les paramètres du rapport
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("PHARMACIE_NOM",
        inventaire.getPharmacieNom() != null ? inventaire.getPharmacieNom() : "-");
    parameters.put("STATUT", inventaire.getStatut() != null ? inventaire.getStatut() : "-");
    parameters.put("DATE_DEBUT",
        inventaire.getDate_debut() != null
            ? inventaire.getDate_debut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            : "-");
    parameters.put("DATE_FIN",
        inventaire.getDate_fin() != null
            ? inventaire.getDate_fin().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            : "-");
    parameters.put("DATE_GENERATION",
        LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    parameters.put("TOTAL_LIGNES", lignes.size());

    // Charger et compiler le template
    try (InputStream templateStream = resource.getInputStream()) {
      log.info("📄 [ReportService] Compilation du template JasperReports...");
      JasperReport jasperReport = JasperCompileManager.compileReport(templateStream);
      log.info("✅ [ReportService] Template compilé avec succès");

      // Créer la source de données avec les lignes
      JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(lignes);
      log.info("✅ [ReportService] Source de données créée: {} lignes", lignes.size());

      // Remplir le rapport
      log.info("🖨️ [ReportService] Remplissage du rapport avec les données...");
      JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
      log.info("✅ [ReportService] Rapport rempli avec succès. Nombre de pages: {}",
          jasperPrint.getPages().size());

      // Exporter en PDF
      log.info("📤 [ReportService] Export du rapport en PDF...");
      byte[] pdfBytes = JasperExportManager.exportReportToPdf(jasperPrint);
      log.info("✅ [ReportService] PDF exporté avec succès: {} bytes", pdfBytes.length);
      return pdfBytes;
    } catch (IOException | JRException e) {
      log.error("Erreur lors de la génération du rapport inventaire", e);
      throw new RuntimeException("Impossible de charger le template de rapport", e);
    }
  }

  /**
   * Génère un rapport PDF de la liste des inventaires. Chaque inventaire est affiché avec toutes
   * ses lignes dans le même PDF.
   *
   * @param inventaires List des inventaires
   * @param inventaireIdToLignes Map des IDs d'inventaire vers leurs lignes
   * @param pharmacieNom Nom de la pharmacie/service (optionnel)
   * @return PDF en tant que byte[]
   * @throws JRException Si une erreur survient lors de la génération du rapport
   */
  public byte[] generateInventairesListReport(List<InventaireResponse> inventaires,
      Map<Long, List<LigneInventaireResponse>> inventaireIdToLignes, String pharmacieNom)
      throws JRException {

    log.info("🚀 [ReportService] Génération du rapport liste inventaires: {} inventaires",
        inventaires.size());

    // Charger le template .jrxml
    ClassPathResource resource = new ClassPathResource(REPORTS_DIR + INVENTAIRES_LIST_REPORT);
    if (!resource.exists()) {
      String errorMsg = String.format("Template de rapport introuvable: %s%s", REPORTS_DIR,
          INVENTAIRES_LIST_REPORT);
      log.error(errorMsg);
      throw new RuntimeException(errorMsg);
    }
    log.info("✅ [ReportService] Template de rapport trouvé: {}", resource.getPath());

    // Créer une structure plate : une ligne de rapport par ligne d'inventaire
    List<InventaireLigneFlatReportDTO> reportData = new ArrayList<>();

    for (InventaireResponse inventaire : inventaires) {
      List<LigneInventaireResponse> lignes =
          inventaireIdToLignes.getOrDefault(inventaire.getId(), new ArrayList<>());

      String statutLabel = inventaire.getStatut() != null ? inventaire.getStatut() : "-";
      String typeLabel =
          inventaire.getTypeinventaire() != null ? inventaire.getTypeinventaire() : "-";
      String pharmacieNomLocal =
          inventaire.getPharmacieNom() != null ? inventaire.getPharmacieNom() : "-";
      String dateDebut = inventaire.getDate_debut() != null
          ? inventaire.getDate_debut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
          : "-";
      String dateFin = inventaire.getDate_fin() != null
          ? inventaire.getDate_fin().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
          : "-";
      String commentaire = inventaire.getCommentaire() != null ? inventaire.getCommentaire() : "";
      String dateCreation = inventaire.getDateCreate() != null
          ? inventaire.getDateCreate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
          : "-";

      // Créer une ligne plate pour chaque ligne d'inventaire
      if (lignes.isEmpty()) {
        // Si pas de lignes, créer quand même une ligne avec les infos de l'inventaire
        reportData.add(InventaireLigneFlatReportDTO.builder().inventaireId(inventaire.getId())
            .pharmacieNom(pharmacieNomLocal).statut(statutLabel).typeinventaire(typeLabel)
            .dateDebut(dateDebut).dateFin(dateFin).commentaire(commentaire)
            .dateCreation(dateCreation).numeroLigne(0).produitNom(null).quantiteTheorique(null)
            .quantitePhysique(null).ecart(null).commentaireLigne(null).build());
      } else {
        for (int index = 0; index < lignes.size(); index++) {
          LigneInventaireResponse ligne = lignes.get(index);
          reportData.add(InventaireLigneFlatReportDTO.builder().inventaireId(inventaire.getId())
              .pharmacieNom(pharmacieNomLocal).statut(statutLabel).typeinventaire(typeLabel)
              .dateDebut(dateDebut).dateFin(dateFin).commentaire(commentaire)
              .dateCreation(dateCreation).numeroLigne(index + 1).produitNom(ligne.getProduitNom())
              .quantiteTheorique(ligne.getQuantite_theorique())
              .quantitePhysique(ligne.getQuantite_physique()).ecart(ligne.getEcart())
              .commentaireLigne(ligne.getCommentaire()).build());
        }
      }
    }

    log.info("✅ [ReportService] Structure plate créée: {} lignes (plates) pour {} inventaires",
        reportData.size(), inventaires.size());

    // Préparer les paramètres du rapport
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("TITRE_RAPPORT", "Liste des Inventaires");
    parameters.put("PHARMACIE_NOM", pharmacieNom != null ? pharmacieNom : "Tous les services");
    parameters.put("DATE_GENERATION",
        LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    parameters.put("TOTAL_INVENTAIRES", inventaires.size());

    // Charger et compiler le template
    try (InputStream templateStream = resource.getInputStream()) {
      log.info("📄 [ReportService] Compilation du template JasperReports...");
      JasperReport jasperReport = JasperCompileManager.compileReport(templateStream);
      log.info("✅ [ReportService] Template compilé avec succès");

      // Créer la source de données (structure plate)
      JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);
      log.info("✅ [ReportService] Source de données créée: {} lignes plates", reportData.size());

      // Remplir le rapport
      log.info("🖨️ [ReportService] Remplissage du rapport avec les données...");
      JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
      log.info("✅ [ReportService] Rapport rempli avec succès. Nombre de pages: {}",
          jasperPrint.getPages().size());

      // Exporter en PDF
      log.info("📤 [ReportService] Export du rapport en PDF...");
      byte[] pdfBytes = JasperExportManager.exportReportToPdf(jasperPrint);
      log.info("✅ [ReportService] PDF exporté avec succès: {} bytes", pdfBytes.length);
      return pdfBytes;
    } catch (IOException | JRException e) {
      log.error("Erreur lors de la génération du rapport de liste inventaires", e);
      throw new RuntimeException("Impossible de charger le template de rapport", e);
    }
  }
}


