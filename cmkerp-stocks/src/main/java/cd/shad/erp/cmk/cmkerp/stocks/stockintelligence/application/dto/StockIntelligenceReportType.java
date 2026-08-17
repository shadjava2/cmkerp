package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

public enum StockIntelligenceReportType {
  MORNING("Rapport matin — pilotage & actions prioritaires"),
  EVENING("Rapport soir — bilan journée & préparation lendemain"),
  ON_DEMAND("Rapport à la demande");

  private final String label;

  StockIntelligenceReportType(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
