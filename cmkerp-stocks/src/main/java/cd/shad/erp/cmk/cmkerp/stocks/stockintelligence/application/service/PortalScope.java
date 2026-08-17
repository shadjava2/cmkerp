package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service;

public enum PortalScope {
  CENTRALE,
  CLIENT;

  public static PortalScope parse(String value) {
    if (value == null || value.isBlank()) {
      return CENTRALE;
    }
    return "CLIENT".equalsIgnoreCase(value.trim()) ? CLIENT : CENTRALE;
  }
}
