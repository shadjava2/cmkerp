package cd.shad.erp.cmk.cmkerp.pos.transferts.application.service;

import cd.shad.erp.cmk.cmkerp.pos.transferts.application.dto.report.TransfertInterneReportRow;
import cd.shad.erp.cmk.cmkerp.pos.transferts.application.dto.response.TransfertInterneResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Génération des rapports PDF/Excel pour les transferts internes POS.
 */
@Service("posTransfertInterneReportService")
@Slf4j
public class TransfertInterneReportService {

  private static final String REPORTS_DIR = "reports/";
  private static final String TRANSFERTS_INTERNES_LIST_REPORT = "transferts-internes-list.jrxml";
  private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  public byte[] generateListPdf(List<TransfertInterneResponse> transferts, String pharmacieNom)
      throws JRException {
    List<TransfertInterneReportRow> rows = toReportRows(transferts);

    ClassPathResource resource = new ClassPathResource(REPORTS_DIR + TRANSFERTS_INTERNES_LIST_REPORT);
    if (!resource.exists()) {
      throw new RuntimeException("Template de rapport introuvable: " + TRANSFERTS_INTERNES_LIST_REPORT);
    }

    Map<String, Object> parameters = new HashMap<>();
    parameters.put("TITRE_RAPPORT", "Liste des transferts internes (POS)");
    parameters.put("PHARMACIE_NOM", pharmacieNom != null ? pharmacieNom : "Toutes les pharmacies");
    parameters.put("DATE_GENERATION", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    parameters.put("TOTAL_TRANSFERTS", transferts.size());

    try (InputStream templateStream = resource.getInputStream()) {
      JasperReport jasperReport = JasperCompileManager.compileReport(templateStream);
      JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(rows);
      JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
      return JasperExportManager.exportReportToPdf(jasperPrint);
    } catch (IOException e) {
      throw new RuntimeException("Impossible de charger le template de rapport", e);
    }
  }

  public byte[] generateListExcel(List<TransfertInterneResponse> transferts, String pharmacieNom) {
    try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Transferts internes");

      CellStyle headerStyle = workbook.createCellStyle();
      Font headerFont = workbook.createFont();
      headerFont.setBold(true);
      headerStyle.setFont(headerFont);
      headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
      headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

      int rowNum = 0;
      Row titleRow = sheet.createRow(rowNum++);
      Cell titleCell = titleRow.createCell(0);
      titleCell.setCellValue("Rapport des transferts internes (POS)");
      titleCell.setCellStyle(headerStyle);
      sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

      rowNum++;
      sheet.createRow(rowNum++).createCell(0).setCellValue("Filtre pharmacie:");
      sheet.getRow(rowNum - 1).createCell(1)
          .setCellValue(pharmacieNom != null ? pharmacieNom : "Toutes");
      sheet.createRow(rowNum++).createCell(0).setCellValue("Total:");
      sheet.getRow(rowNum - 1).createCell(1).setCellValue(transferts.size());

      rowNum++;
      String[] headers =
          {"ID", "Pharmacie source", "Pharmacie destination", "Statut", "Date création", "Commentaire"};
      Row columnHeaderRow = sheet.createRow(rowNum++);
      for (int i = 0; i < headers.length; i++) {
        Cell cell = columnHeaderRow.createCell(i);
        cell.setCellValue(headers[i]);
        cell.setCellStyle(headerStyle);
      }

      for (TransfertInterneResponse t : transferts) {
        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue(t.getId() != null ? t.getId() : 0);
        row.createCell(1).setCellValue(t.getPharmacieSourceNom() != null ? t.getPharmacieSourceNom() : "-");
        row.createCell(2)
            .setCellValue(t.getPharmacieDestinationNom() != null ? t.getPharmacieDestinationNom() : "-");
        row.createCell(3).setCellValue(t.getStatut() != null ? t.getStatut() : "-");
        row.createCell(4).setCellValue(
            t.getDateCreate() != null ? t.getDateCreate().format(DATE_TIME_FMT) : "-");
        row.createCell(5).setCellValue(t.getCommentaire() != null ? t.getCommentaire() : "");
      }

      for (int i = 0; i < headers.length; i++) {
        sheet.autoSizeColumn(i);
      }

      workbook.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      log.error("Erreur génération Excel transferts internes POS", e);
      throw new RuntimeException("Impossible de générer le rapport Excel", e);
    }
  }

  private List<TransfertInterneReportRow> toReportRows(List<TransfertInterneResponse> transferts) {
    return transferts.stream()
        .map(t -> TransfertInterneReportRow.builder()
            .transfertId(t.getId())
            .pharmacieSourceNom(t.getPharmacieSourceNom() != null ? t.getPharmacieSourceNom() : "-")
            .pharmacieDestinationNom(
                t.getPharmacieDestinationNom() != null ? t.getPharmacieDestinationNom() : "-")
            .statut(t.getStatut() != null ? t.getStatut() : "-")
            .dateCreation(
                t.getDateCreate() != null ? t.getDateCreate().format(DATE_TIME_FMT) : "-")
            .commentaire(t.getCommentaire())
            .build())
        .collect(Collectors.toList());
  }
}
