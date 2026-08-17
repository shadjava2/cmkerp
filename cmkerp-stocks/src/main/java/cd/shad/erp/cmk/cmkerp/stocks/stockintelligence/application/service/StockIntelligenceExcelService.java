package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockIntelligenceMultiSnapshotDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockIntelligenceReportType;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockIntelligenceSnapshotDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockProductCategory;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockProductInsightDTO;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class StockIntelligenceExcelService {

  private static final String[] HEADERS = {
      "Pharmacie", "Produit", "Nom scientifique", "Forme", "Dosage", "Conditionnement",
      "Stock actuel", "Seuil critique", "Entrées mois", "Entrées mois-1",
      "Sorties mois", "Sorties mois-1", "Dernier mouvement", "Tendance", "Jours couverture", "Rupture"
  };

  public byte[] generateWorkbook(StockIntelligenceMultiSnapshotDTO multi, StockIntelligenceReportType reportType) {
    try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      CellStyle headerStyle = createHeaderStyle(workbook);
      writeSummarySheet(workbook, multi, reportType, headerStyle);
      writeCategorySheet(workbook, "Actifs ce mois", mergeCategory(multi, StockProductCategory.AVEC_MOUVEMENT), headerStyle);
      writeCategorySheet(workbook, "Inactifs stock OK", mergeCategory(multi, StockProductCategory.STOCK_SANS_MOUVEMENT), headerStyle);
      writeCategorySheet(workbook, "Inactifs rupture", mergeCategory(multi, StockProductCategory.RUPTURE_SANS_MOUVEMENT), headerStyle);
      workbook.write(out);
      return out.toByteArray();
    } catch (Exception e) {
      log.error("Génération Excel stock intelligence échouée", e);
      throw new IllegalStateException("Impossible de générer le fichier Excel", e);
    }
  }

  public String buildFilename(StockIntelligenceMultiSnapshotDTO multi, StockIntelligenceReportType reportType) {
    String ts = multi.generatedAt().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"));
    return "cmk-stock-" + reportType.name().toLowerCase() + "-" + ts + ".xlsx";
  }

  private List<StockProductInsightDTO> mergeCategory(
      StockIntelligenceMultiSnapshotDTO multi,
      StockProductCategory category) {

    List<StockProductInsightDTO> all = new ArrayList<>();
    for (StockIntelligenceSnapshotDTO snap : multi.pharmacies()) {
      all.addAll(snap.produitsParCategorie().getOrDefault(category, List.of()));
    }
    return all;
  }

  private void writeSummarySheet(
      Workbook workbook,
      StockIntelligenceMultiSnapshotDTO multi,
      StockIntelligenceReportType reportType,
      CellStyle headerStyle) {

    Sheet sheet = workbook.createSheet("Synthèse");
    int rowIdx = 0;
    rowIdx = writeLabelValue(sheet, rowIdx, "Type rapport", reportType.label());
    rowIdx = writeLabelValue(sheet, rowIdx, "Généré le", multi.generatedAt().toString());
    rowIdx = writeLabelValue(sheet, rowIdx, "Pharmacies centrales", String.valueOf(multi.pharmacies().size()));
    rowIdx++;
    rowIdx = writeLabelValue(sheet, rowIdx, "Actifs ce mois (total)", String.valueOf(multi.resumeGlobal().totalAvecMouvement()));
    rowIdx = writeLabelValue(sheet, rowIdx, "Inactifs ce mois — stock OK (total)", String.valueOf(multi.resumeGlobal().totalStockSansMouvement()));
    rowIdx = writeLabelValue(sheet, rowIdx, "Inactifs ce mois — rupture (total)", String.valueOf(multi.resumeGlobal().totalRuptureSansMouvement()));
    rowIdx = writeLabelValue(sheet, rowIdx, "Total ruptures", String.valueOf(multi.resumeGlobal().totalRuptures()));
    rowIdx = writeLabelValue(sheet, rowIdx, "Total analysés", String.valueOf(multi.resumeGlobal().totalProduitsAnalyses()));
    rowIdx++;

    Row phHeader = sheet.createRow(rowIdx++);
    phHeader.createCell(0).setCellValue("Pharmacie");
    phHeader.createCell(1).setCellValue("Analysés");
    phHeader.createCell(2).setCellValue("Actifs ce mois");
    phHeader.createCell(3).setCellValue("Inactifs stock OK");
    phHeader.createCell(4).setCellValue("Inactifs rupture");
    phHeader.createCell(5).setCellValue("Ruptures");
    for (int c = 0; c <= 5; c++) {
      phHeader.getCell(c).setCellStyle(headerStyle);
    }

    for (StockIntelligenceSnapshotDTO ph : multi.pharmacies()) {
      var r = ph.resume();
      Row row = sheet.createRow(rowIdx++);
      row.createCell(0).setCellValue(ph.pharmacieLabel());
      row.createCell(1).setCellValue(r.totalProduitsAnalyses());
      row.createCell(2).setCellValue(r.totalAvecMouvement());
      row.createCell(3).setCellValue(r.totalStockSansMouvement());
      row.createCell(4).setCellValue(r.totalRuptureSansMouvement());
      row.createCell(5).setCellValue(r.totalRuptures());
    }

    for (int c = 0; c < 6; c++) {
      sheet.autoSizeColumn(c);
    }
  }

  private void writeCategorySheet(Workbook workbook, String name, List<StockProductInsightDTO> items, CellStyle headerStyle) {
    String sheetName = name.length() > 31 ? name.substring(0, 31) : name;
    Sheet sheet = workbook.createSheet(sheetName);
    Row header = sheet.createRow(0);
    for (int i = 0; i < HEADERS.length; i++) {
      Cell cell = header.createCell(i);
      cell.setCellValue(HEADERS[i]);
      cell.setCellStyle(headerStyle);
    }
    int rowIdx = 1;
    for (StockProductInsightDTO p : items) {
      Row row = sheet.createRow(rowIdx++);
      int c = 0;
      row.createCell(c++).setCellValue(nullSafe(p.pharmacie()));
      row.createCell(c++).setCellValue(nullSafe(p.nomCommercial()));
      row.createCell(c++).setCellValue(nullSafe(p.nomScientifique()));
      row.createCell(c++).setCellValue(nullSafe(p.forme()));
      row.createCell(c++).setCellValue(nullSafe(p.dosage()));
      row.createCell(c++).setCellValue(nullSafe(p.conditionnement()));
      setNumeric(row.createCell(c++), p.stockActuel());
      setNumeric(row.createCell(c++), p.seuilCritique());
      setNumeric(row.createCell(c++), p.entreesMoisEnCours());
      setNumeric(row.createCell(c++), p.entreesMoisPrecedent());
      setNumeric(row.createCell(c++), p.sortiesMoisEnCours());
      setNumeric(row.createCell(c++), p.sortiesMoisPrecedent());
      row.createCell(c++).setCellValue(p.dateDernierMouvement() != null ? p.dateDernierMouvement().toString() : "");
      row.createCell(c++).setCellValue(p.tendanceSorties().name());
      if (p.joursCouvertureEstimes() != null) {
        setNumeric(row.createCell(c++), p.joursCouvertureEstimes());
      } else {
        row.createCell(c++).setCellValue("");
      }
      row.createCell(c).setCellValue(p.enRupture() ? "OUI" : "NON");
    }
    for (int i = 0; i < HEADERS.length; i++) {
      sheet.autoSizeColumn(i);
    }
  }

  private int writeLabelValue(Sheet sheet, int rowIdx, String label, String value) {
    Row row = sheet.createRow(rowIdx);
    row.createCell(0).setCellValue(label);
    row.createCell(1).setCellValue(value != null ? value : "");
    return rowIdx + 1;
  }

  private CellStyle createHeaderStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    Font font = workbook.createFont();
    font.setBold(true);
    style.setFont(font);
    return style;
  }

  private void setNumeric(Cell cell, java.math.BigDecimal value) {
    if (value != null) {
      cell.setCellValue(value.doubleValue());
    }
  }

  private String nullSafe(String s) {
    return s != null ? s : "";
  }
}
