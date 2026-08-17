package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StockProductInsightDTO(
    Long idStock,
    Long idPharmacie,
    String pharmacie,
    String nomCommercial,
    String nomScientifique,
    String forme,
    String dosage,
    String conditionnement,
    BigDecimal stockActuel,
    BigDecimal seuilCritique,
    BigDecimal entreesMoisEnCours,
    BigDecimal entreesMoisPrecedent,
    BigDecimal sortiesMoisEnCours,
    BigDecimal sortiesMoisPrecedent,
    LocalDate dateDernierMouvement,
    ConsumptionTrend tendanceSorties,
    BigDecimal sortieJournaliereMoyenneMoisEnCours,
    BigDecimal joursCouvertureEstimes,
    StockProductCategory categorie,
    boolean enRupture
) {
}
