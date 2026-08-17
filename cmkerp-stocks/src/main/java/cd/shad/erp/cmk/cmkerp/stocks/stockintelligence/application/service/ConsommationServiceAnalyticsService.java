package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service;

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.config.ConditionalOnStockIntelligenceEnabled;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.persistence.ConsommationServiceAnalyticsRepository;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnStockIntelligenceEnabled
@RequiredArgsConstructor
@Slf4j
public class ConsommationServiceAnalyticsService {

  private final ConsommationServiceAnalyticsRepository repository;

  public Map<String, Object> kpis(Long pharmacieId, LocalDate from, LocalDate to, String q) {
    return repository.kpis(pharmacieId, from, to, q);
  }

  public List<Map<String, Object>> statsMensuel(
      Long pharmacieId, LocalDate from, LocalDate to, String q) {
    return repository.statsMensuel(pharmacieId, from, to, q);
  }

  public List<Map<String, Object>> statsProduits(
      Long pharmacieId, LocalDate from, LocalDate to, String q, int limit) {
    return repository.statsProduits(pharmacieId, from, to, q, limit);
  }

  /**
   * Tableau croisé produits × mois : chaque mois de la période devient une colonne.
   */
  public Map<String, Object> statsProduitsMensuel(
      Long pharmacieId, LocalDate from, LocalDate to, String q, int limit) {
    List<Map<String, Object>> cells =
        repository.statsProduitsParMois(pharmacieId, from, to, q, limit);

    List<String> mois = buildMoisColumns(from, to, cells);
    Map<Long, Map<String, Object>> byProduit = new LinkedHashMap<>();

    for (Map<String, Object> cell : cells) {
      Long produitId = toLong(cell.get("produit_id"));
      if (produitId == null) {
        continue;
      }
      Map<String, Object> row =
          byProduit.computeIfAbsent(
              produitId,
              id -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("produit_id", id);
                m.put("produit", str(cell.get("produit")));
                m.put("codebarre", str(cell.get("codebarre")));
                m.put("quantite_totale", 0d);
                Map<String, Double> parMois = new LinkedHashMap<>();
                for (String mo : mois) {
                  parMois.put(mo, 0d);
                }
                m.put("par_mois", parMois);
                return m;
              });

      String periode = str(cell.get("periode"));
      double qty = toDouble(cell.get("quantite"));
      @SuppressWarnings("unchecked")
      Map<String, Double> parMois = (Map<String, Double>) row.get("par_mois");
      if (periode != null && !periode.isBlank()) {
        if (!parMois.containsKey(periode)) {
          // Mois hors plage générée (données seules) : l'ajouter
          parMois.put(periode, 0d);
          if (!mois.contains(periode)) {
            mois.add(periode);
            mois.sort(String::compareTo);
          }
        }
        parMois.put(periode, parMois.getOrDefault(periode, 0d) + qty);
      }
      row.put("quantite_totale", toDouble(row.get("quantite_totale")) + qty);
    }

    // Re-trier mois si des extras ont été ajoutés, et réordonner par_mois
    mois.sort(String::compareTo);
    List<Map<String, Object>> lignes = new ArrayList<>(byProduit.values());
    lignes.sort(
        (a, b) -> Double.compare(toDouble(b.get("quantite_totale")), toDouble(a.get("quantite_totale"))));

    for (Map<String, Object> row : lignes) {
      @SuppressWarnings("unchecked")
      Map<String, Double> parMois = (Map<String, Double>) row.get("par_mois");
      Map<String, Double> ordered = new LinkedHashMap<>();
      for (String mo : mois) {
        ordered.put(mo, parMois.getOrDefault(mo, 0d));
      }
      row.put("par_mois", ordered);
    }

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("mois", mois);
    out.put("lignes", lignes);
    return out;
  }

  public List<Map<String, Object>> details(
      Long pharmacieId, LocalDate from, LocalDate to, String usageType, String q, int limit) {
    return repository.details(pharmacieId, from, to, usageType, q, limit);
  }

  public byte[] exportExcel(
      Long pharmacieId,
      LocalDate from,
      LocalDate to,
      String usageType,
      String q,
      String pharmacieLabel) {
    Map<String, Object> kpis = repository.kpis(pharmacieId, from, to, q);
    List<Map<String, Object>> mensuel = repository.statsMensuel(pharmacieId, from, to, q);
    List<Map<String, Object>> produits = repository.statsProduits(pharmacieId, from, to, q, 500);
    Map<String, Object> produitsMensuel = statsProduitsMensuel(pharmacieId, from, to, q, 500);
    List<Map<String, Object>> lignes =
        repository.details(pharmacieId, from, to, usageType, q, 10_000);

    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      CellStyle headerStyle = headerStyle(workbook);
      writeSynthese(workbook, headerStyle, kpis, pharmacieLabel, from, to, usageType, q);
      writeSheet(
          workbook,
          headerStyle,
          "Mensuel",
          new String[] {
            "Période",
            "Nb sorties",
            "Nb lignes",
            "Nb produits",
            "Qté totale",
            "Qté vente privée",
            "Qté facturée",
            "Qté usage",
            "Montant total",
            "Montant vente privée",
            "Montant facturée",
            "Montant usage"
          },
          mensuel,
          new String[] {
            "periode",
            "nb_sorties",
            "nb_lignes",
            "nb_produits",
            "quantite_totale",
            "qty_vente_privee",
            "qty_facturee",
            "qty_usage",
            "montant_total",
            "montant_vente_privee",
            "montant_facturee",
            "montant_usage"
          });
      writeSheet(
          workbook,
          headerStyle,
          "Produits",
          new String[] {
            "Produit",
            "Code-barres",
            "Nb lignes",
            "Nb sorties",
            "Qté totale",
            "Qté vente privée",
            "Qté facturée",
            "Qté usage",
            "Montant total",
            "Montant vente privée",
            "Montant facturée",
            "Montant usage"
          },
          produits,
          new String[] {
            "produit",
            "codebarre",
            "nb_lignes",
            "nb_sorties",
            "quantite_totale",
            "qty_vente_privee",
            "qty_facturee",
            "qty_usage",
            "montant_total",
            "montant_vente_privee",
            "montant_facturee",
            "montant_usage"
          });
      writeProduitsMensuelSheet(workbook, headerStyle, produitsMensuel);
      writeSheet(
          workbook,
          headerStyle,
          "Détail lignes",
          new String[] {
            "Date",
            "Vente #",
            "Usage",
            "Statut",
            "Service",
            "Produit",
            "Code-barres",
            "Quantité",
            "Prix unitaire",
            "Montant",
            "Patient",
            "Code IPP",
            "Entreprise",
            "Demandeur",
            "Commentaire",
            "Type paiement"
          },
          lignes,
          new String[] {
            "datecreate",
            "vente_id",
            "usage_type",
            "statut",
            "service_nom",
            "produit",
            "codebarre",
            "quantite",
            "prix_unitaire",
            "montant_ligne",
            "patient",
            "patient_codeipp",
            "entreprise",
            "demandeur",
            "commentaire",
            "typepaiement"
          });
      workbook.write(out);
      return out.toByteArray();
    } catch (Exception e) {
      log.error("Export Excel consommation services échoué", e);
      throw new IllegalStateException("Impossible de générer le fichier Excel", e);
    }
  }

  public String exportFilename() {
    return "consommation-service-"
        + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        + ".xlsx";
  }

  private void writeSynthese(
      Workbook workbook,
      CellStyle headerStyle,
      Map<String, Object> kpis,
      String pharmacieLabel,
      LocalDate from,
      LocalDate to,
      String usageType,
      String q) {
    Sheet sheet = workbook.createSheet("Synthèse");
    int r = 0;
    r = kv(sheet, r, "Rapport", "Consommation service");
    r = kv(sheet, r, "Service", pharmacieLabel != null ? pharmacieLabel : "Tous");
    r = kv(sheet, r, "Du", from != null ? from.toString() : "");
    r = kv(sheet, r, "Au", to != null ? to.toString() : "");
    r = kv(sheet, r, "Produit (filtre)", q != null ? q : "");
    r = kv(sheet, r, "Type usage (filtre)", usageType != null ? usageType : "Tous");
    r = kv(sheet, r, "Généré le", LocalDateTime.now().toString());
    r++;
    Row header = sheet.createRow(r++);
    header.createCell(0).setCellValue("Indicateur");
    header.createCell(1).setCellValue("Valeur");
    header.getCell(0).setCellStyle(headerStyle);
    header.getCell(1).setCellStyle(headerStyle);

    String[][] rows = {
      {"Nb sorties", str(kpis.get("nb_sorties"))},
      {"Nb lignes", str(kpis.get("nb_lignes"))},
      {"Nb produits", str(kpis.get("nb_produits"))},
      {"Qté totale", str(kpis.get("quantite_totale"))},
      {"Montant total", str(kpis.get("montant_total"))},
      {"Qté vente privée (PAYEE)", str(kpis.get("qty_vente_privee"))},
      {"Montant vente privée", str(kpis.get("montant_vente_privee"))},
      {"Qté facturée (FACTUREE)", str(kpis.get("qty_facturee"))},
      {"Montant facturée", str(kpis.get("montant_facturee"))},
      {"Qté usage (SORTIE-USAGE)", str(kpis.get("qty_usage"))},
      {"Montant usage", str(kpis.get("montant_usage"))},
    };
    for (String[] row : rows) {
      r = kv(sheet, r, row[0], row[1]);
    }
    sheet.autoSizeColumn(0);
    sheet.autoSizeColumn(1);
  }

  @SuppressWarnings("unchecked")
  private void writeProduitsMensuelSheet(
      Workbook workbook, CellStyle headerStyle, Map<String, Object> produitsMensuel) {
    List<String> mois =
        produitsMensuel.get("mois") instanceof List<?> list
            ? list.stream().map(String::valueOf).toList()
            : List.of();
    List<Map<String, Object>> lignes =
        produitsMensuel.get("lignes") instanceof List<?> list
            ? (List<Map<String, Object>>) list
            : List.of();

    Sheet sheet = workbook.createSheet("Produits x mois");
    Row header = sheet.createRow(0);
    int col = 0;
    Cell c0 = header.createCell(col++);
    c0.setCellValue("Produit");
    c0.setCellStyle(headerStyle);
    Cell c1 = header.createCell(col++);
    c1.setCellValue("Code-barres");
    c1.setCellStyle(headerStyle);
    for (String mo : mois) {
      Cell cell = header.createCell(col++);
      cell.setCellValue(mo);
      cell.setCellStyle(headerStyle);
    }
    Cell cTotal = header.createCell(col);
    cTotal.setCellValue("Total");
    cTotal.setCellStyle(headerStyle);

    int rowIdx = 1;
    for (Map<String, Object> data : lignes) {
      Row row = sheet.createRow(rowIdx++);
      int i = 0;
      setCell(row.createCell(i++), data.get("produit"));
      setCell(row.createCell(i++), data.get("codebarre"));
      Map<String, Double> parMois =
          data.get("par_mois") instanceof Map<?, ?> m
              ? (Map<String, Double>) m
              : Map.of();
      for (String mo : mois) {
        setCell(row.createCell(i++), parMois.getOrDefault(mo, 0d));
      }
      setCell(row.createCell(i), data.get("quantite_totale"));
    }
    for (int i = 0; i <= mois.size() + 2; i++) {
      sheet.autoSizeColumn(i);
    }
  }

  private static List<String> buildMoisColumns(
      LocalDate from, LocalDate to, List<Map<String, Object>> cells) {
    List<String> mois = new ArrayList<>();
    if (from != null && to != null) {
      YearMonth start = YearMonth.from(from);
      YearMonth end = YearMonth.from(to);
      if (end.isBefore(start)) {
        YearMonth tmp = start;
        start = end;
        end = tmp;
      }
      YearMonth cursor = start;
      int guard = 0;
      while (!cursor.isAfter(end) && guard++ < 120) {
        mois.add(cursor.toString());
        cursor = cursor.plusMonths(1);
      }
      return mois;
    }

    Set<String> fromData = new LinkedHashSet<>();
    for (Map<String, Object> cell : cells) {
      String periode = str(cell.get("periode"));
      if (periode != null && !periode.isBlank()) {
        fromData.add(periode);
      }
    }
    if (from != null && to == null) {
      YearMonth start = YearMonth.from(from);
      YearMonth end =
          fromData.stream()
              .map(
                  s -> {
                    try {
                      return YearMonth.parse(s);
                    } catch (Exception e) {
                      return null;
                    }
                  })
              .filter(ym -> ym != null)
              .max(YearMonth::compareTo)
              .orElse(YearMonth.now());
      YearMonth cursor = start;
      int guard = 0;
      while (!cursor.isAfter(end) && guard++ < 120) {
        mois.add(cursor.toString());
        cursor = cursor.plusMonths(1);
      }
      return mois;
    }
    if (to != null && from == null) {
      YearMonth end = YearMonth.from(to);
      YearMonth start =
          fromData.stream()
              .map(
                  s -> {
                    try {
                      return YearMonth.parse(s);
                    } catch (Exception e) {
                      return null;
                    }
                  })
              .filter(ym -> ym != null)
              .min(YearMonth::compareTo)
              .orElse(end);
      YearMonth cursor = start;
      int guard = 0;
      while (!cursor.isAfter(end) && guard++ < 120) {
        mois.add(cursor.toString());
        cursor = cursor.plusMonths(1);
      }
      return mois;
    }

    mois.addAll(fromData);
    mois.sort(String::compareTo);
    return mois;
  }

  private static Long toLong(Object v) {
    if (v == null) {
      return null;
    }
    if (v instanceof Number n) {
      return n.longValue();
    }
    try {
      return Long.parseLong(String.valueOf(v));
    } catch (Exception e) {
      return null;
    }
  }

  private static double toDouble(Object v) {
    if (v == null) {
      return 0d;
    }
    if (v instanceof Number n) {
      return n.doubleValue();
    }
    if (v instanceof BigDecimal bd) {
      return bd.doubleValue();
    }
    try {
      return Double.parseDouble(String.valueOf(v));
    } catch (Exception e) {
      return 0d;
    }
  }

  private void writeSheet(
      Workbook workbook,
      CellStyle headerStyle,
      String name,
      String[] headers,
      List<Map<String, Object>> rows,
      String[] keys) {
    Sheet sheet = workbook.createSheet(name.length() > 31 ? name.substring(0, 31) : name);
    Row header = sheet.createRow(0);
    for (int i = 0; i < headers.length; i++) {
      Cell cell = header.createCell(i);
      cell.setCellValue(headers[i]);
      cell.setCellStyle(headerStyle);
    }
    int rowIdx = 1;
    for (Map<String, Object> data : rows) {
      Row row = sheet.createRow(rowIdx++);
      for (int i = 0; i < keys.length; i++) {
        setCell(row.createCell(i), data.get(keys[i]));
      }
    }
    for (int i = 0; i < headers.length; i++) {
      sheet.autoSizeColumn(i);
    }
  }

  private static int kv(Sheet sheet, int rowIdx, String label, String value) {
    Row row = sheet.createRow(rowIdx);
    row.createCell(0).setCellValue(label);
    row.createCell(1).setCellValue(value != null ? value : "");
    return rowIdx + 1;
  }

  private static void setCell(Cell cell, Object value) {
    if (value == null) {
      cell.setCellValue("");
      return;
    }
    if (value instanceof Number n) {
      cell.setCellValue(n.doubleValue());
      return;
    }
    if (value instanceof BigDecimal bd) {
      cell.setCellValue(bd.doubleValue());
      return;
    }
    if (value instanceof java.sql.Timestamp ts) {
      cell.setCellValue(ts.toLocalDateTime().toString());
      return;
    }
    if (value instanceof java.sql.Date d) {
      cell.setCellValue(d.toLocalDate().toString());
      return;
    }
    if (value instanceof LocalDateTime ldt) {
      cell.setCellValue(ldt.toString());
      return;
    }
    if (value instanceof LocalDate ld) {
      cell.setCellValue(ld.toString());
      return;
    }
    cell.setCellValue(String.valueOf(value));
  }

  private static CellStyle headerStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    Font font = workbook.createFont();
    font.setBold(true);
    style.setFont(font);
    return style;
  }

  private static String str(Object v) {
    return v == null ? "" : String.valueOf(v);
  }
}
