package cd.shad.erp.cmk.cmkerp.stocks.commandes;

import static org.junit.jupiter.api.Assertions.*;

import cd.shad.erp.cmk.cmkerp.stocks.commandes.application.service.MoneyConversionService;
import cd.shad.erp.cmk.cmkerp.stocks.commandes.application.service.StatutTransitionRules;
import cd.shad.erp.cmk.cmkerp.stocks.commandes.domain.model.CommandesStatuts.BonCommande;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MoneyConversion / Reliquat / Transitions")
class MoneyConversionAndStatutTest {

  @Test
  @DisplayName("Reliquat = max(0, commandée - reçue)")
  void reliquatCalculation() {
    assertEquals(0, MoneyConversionService.calculerReliquat(new BigDecimal("10"), new BigDecimal("10")).compareTo(BigDecimal.ZERO));
    assertEquals(0, MoneyConversionService.calculerReliquat(new BigDecimal("10"), new BigDecimal("3")).compareTo(new BigDecimal("7")));
    assertEquals(0, MoneyConversionService.calculerReliquat(new BigDecimal("5"), new BigDecimal("8")).compareTo(BigDecimal.ZERO));
    assertTrue(MoneyConversionService.isTotalementLivre(new BigDecimal("10"), new BigDecimal("10")));
    assertTrue(MoneyConversionService.isPartiellementLivre(new BigDecimal("10"), new BigDecimal("4")));
    assertFalse(MoneyConversionService.isPartiellementLivre(new BigDecimal("10"), BigDecimal.ZERO));
  }

  @Test
  @DisplayName("Transitions BC valides / invalides")
  void bonTransitions() {
    assertDoesNotThrow(() -> StatutTransitionRules.assertBonTransition("BROUILLON", "EN_VALIDATION"));
    assertDoesNotThrow(() -> StatutTransitionRules.assertBonTransition("VALIDE", "ENVOYE"));
    assertThrows(BusinessException.class,
        () -> StatutTransitionRules.assertBonTransition("ANNULE", "VALIDE"));
    assertThrows(BusinessException.class,
        () -> StatutTransitionRules.assertBonTransition("CLOTURE", "BROUILLON"));
    assertEquals(BonCommande.VALIDE.name(), StatutTransitionRules.targetStatutForAction("EN_VALIDATION", "valider"));
  }

  @Test
  @DisplayName("Transitions demande cotation")
  void demandeTransitions() {
    assertDoesNotThrow(() -> StatutTransitionRules.assertDemandeTransition("BROUILLON", "EN_VALIDATION_INTERNE"));
    assertDoesNotThrow(() -> StatutTransitionRules.assertDemandeTransition("BROUILLON", "APPROUVEE"));
    assertDoesNotThrow(() -> StatutTransitionRules.assertDemandeTransition("EN_VALIDATION_INTERNE", "APPROUVEE"));
    assertDoesNotThrow(() -> StatutTransitionRules.assertDemandeTransition("EN_VALIDATION_INTERNE", "BROUILLON"));
    assertDoesNotThrow(() -> StatutTransitionRules.assertDemandeTransition("APPROUVEE", "ENVOYEE"));
    assertDoesNotThrow(() -> StatutTransitionRules.assertDemandeTransition("ENVOYEE", "EN_ANALYSE"));
    assertThrows(BusinessException.class,
        () -> StatutTransitionRules.assertDemandeTransition("BROUILLON", "ENVOYEE"));
    assertThrows(BusinessException.class,
        () -> StatutTransitionRules.assertDemandeTransition("ANNULEE", "ENVOYEE"));
  }
}
