package cd.shad.erp.cmk.cmkerp.stocks.commandes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import cd.shad.erp.cmk.cmkerp.stocks.commandes.application.service.MoneyConversionService;
import cd.shad.erp.cmk.cmkerp.stocks.commandes.application.service.PortailFournisseurService;
import cd.shad.erp.cmk.cmkerp.stocks.commandes.domain.model.DemandeCotation;
import cd.shad.erp.cmk.cmkerp.stocks.commandes.domain.model.InvitationFournisseur;
import cd.shad.erp.cmk.cmkerp.stocks.commandes.domain.repository.CommandesRepository;
import cd.shad.erp.cmk.cmkerp.stocks.commandes.util.Sha256Hasher;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Hash SHA-256 + déverrouillage portail")
class Sha256HasherAndUnlockTest {

  @Mock
  private CommandesRepository repo;

  @Mock
  private MoneyConversionService moneyConversionService;

  @Mock
  private ObjectMapper objectMapper;

  @InjectMocks
  private PortailFournisseurService portailService;

  @Test
  @DisplayName("sha256Hex est déterministe et sensible à la casse / trim côté caller")
  void sha256HexDeterministic() {
    String code = "ENZUCJWR";
    String hash = Sha256Hasher.sha256Hex(code);
    assertNotNull(hash);
    assertEquals(64, hash.length());
    assertEquals(hash, Sha256Hasher.sha256Hex(code));
    assertNotEquals(hash, Sha256Hasher.sha256Hex(code.toLowerCase()));
    assertNotEquals(hash, Sha256Hasher.sha256Hex(" " + code));
  }

  @Test
  @DisplayName("generateAccessCode produit 8 caractères hors ambiguïtés")
  void generateAccessCodeFormat() {
    String code = Sha256Hasher.generateAccessCode();
    assertEquals(8, code.length());
    assertTrue(code.matches("[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8}"));
  }

  @Test
  @DisplayName("unlock accepte le code dont le hash SHA-256 correspond à access_code_hash")
  void unlockAcceptsMatchingCode() {
    String plaintext = "ENZUCJWR";
    String token = "33f7617703ae4f97b3fd0ca7971cb3e8";
    InvitationFournisseur inv = InvitationFournisseur.builder()
        .id(1L)
        .fkDemandeCotation(10L)
        .fkFournisseur(20L)
        .publicToken(token)
        .accessCodeHash(Sha256Hasher.sha256Hex(plaintext))
        .statut("ENVOYEE")
        .unlockAttempts(0)
        .build();
    DemandeCotation demande = DemandeCotation.builder()
        .id(10L)
        .numero("COT-1")
        .objet("Test")
        .build();

    when(repo.findInvitationByPublicToken(token)).thenReturn(Optional.of(inv));
    when(repo.findDemandeById(10L)).thenReturn(Optional.of(demande));
    when(repo.findFournisseurNom(20L)).thenReturn("AFCV");

    Map<String, Object> session = portailService.unlock(token, "  " + plaintext + "  ");

    assertNotNull(session.get("sessionToken"));
    ArgumentCaptor<InvitationFournisseur> captor = ArgumentCaptor.forClass(InvitationFournisseur.class);
    verify(repo, atLeastOnce()).updateInvitation(captor.capture());
    InvitationFournisseur saved = captor.getValue();
    assertEquals(Sha256Hasher.sha256Hex(plaintext), saved.getAccessCodeHash());
    assertNotNull(saved.getSessionTokenHash());
    assertEquals("OUVERTE", saved.getStatut());
  }

  @Test
  @DisplayName("unlock refuse un code dont le hash ne correspond pas")
  void unlockRejectsWrongCode() {
    String token = "tok";
    InvitationFournisseur inv = InvitationFournisseur.builder()
        .id(1L)
        .fkDemandeCotation(10L)
        .fkFournisseur(20L)
        .publicToken(token)
        .accessCodeHash(Sha256Hasher.sha256Hex("CORRECT1"))
        .statut("ENVOYEE")
        .unlockAttempts(0)
        .build();

    when(repo.findInvitationByPublicToken(token)).thenReturn(Optional.of(inv));

    BusinessException ex = assertThrows(BusinessException.class,
        () -> portailService.unlock(token, "WRONGCOD"));
    assertTrue(ex.getMessage().contains("Code d'accès invalide"));
    verify(repo).updateInvitation(any(InvitationFournisseur.class));
  }
}
