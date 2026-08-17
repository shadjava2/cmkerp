package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.util.List;

public record PilotageAiDecisionDTO(
    String synthese,
    String niveauRisqueGlobal,
    List<String> actionsPrioritaires,
    List<String> risquesIdentifies,
    String commentaireExpert) {}
