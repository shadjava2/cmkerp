package cd.shad.erp.cmk.cmkerp.stocks.commandes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import cd.shad.erp.cmk.cmkerp.stocks.commandes.application.service.MoneyConversionService;
import cd.shad.erp.cmk.cmkerp.stocks.commandes.application.service.PortailFournisseurService;
import cd.shad.erp.cmk.cmkerp.stocks.commandes.domain.model.InvitationFournisseur;
import cd.shad.erp.cmk.cmkerp.stocks.commandes.domain.model.OffreFournisseur;
import cd.shad.erp.cmk.cmkerp.stocks.commandes.domain.repository.CommandesRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Isolation offres entre fournisseurs (portail)")
class PortailIsolationOffresTest {

  @Mock
  private CommandesRepository repo;

  @Mock
  private MoneyConversionService moneyConversionService;

  @Mock
  private ObjectMapper objectMapper;

  @InjectMocks
  private PortailFournisseurService portailService;

  @Test
  @DisplayName("Une invitation ne peut pas voir l'offre d'un autre fournisseur")
  void cannotSeeCompetitorOffer() {
    InvitationFournisseur invA = InvitationFournisseur.builder().id(1L).fkFournisseur(10L).build();
    OffreFournisseur offreB = OffreFournisseur.builder().id(99L).fkInvitation(2L).fkFournisseur(20L).build();

    when(repo.findInvitationById(1L)).thenReturn(Optional.of(invA));
    when(repo.findOffreById(99L)).thenReturn(Optional.of(offreB));

    assertFalse(portailService.canSeeCompetitorOffer(1L, 99L));
  }

  @Test
  @DisplayName("Une invitation peut voir sa propre offre")
  void canSeeOwnOffer() {
    InvitationFournisseur invA = InvitationFournisseur.builder().id(1L).fkFournisseur(10L).build();
    OffreFournisseur offreA = OffreFournisseur.builder().id(50L).fkInvitation(1L).fkFournisseur(10L).build();

    when(repo.findInvitationById(1L)).thenReturn(Optional.of(invA));
    when(repo.findOffreById(50L)).thenReturn(Optional.of(offreA));

    assertTrue(portailService.canSeeCompetitorOffer(1L, 50L));
  }
}
