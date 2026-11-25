package cd.shad.erp.cmk.cmkerp.platform.security.domain.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.Role;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.RoleRepository;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.UtilisateurRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.SystemRoleConstants;

class RoleDomainServiceAdminProtectionTest {

  private RoleRepository roleRepository;
  private UtilisateurRepository utilisateurRepository;
  private RoleDomainService service;

  @BeforeEach
  void setUp() {
    roleRepository = mock(RoleRepository.class);
    utilisateurRepository = mock(UtilisateurRepository.class);
    service = new RoleDomainService(roleRepository, utilisateurRepository);
  }

  @Test
  void refuseModificationDuRoleAdmin() {
    assertThrows(BusinessException.class,
        () -> service.validerRoleNonSysteme(SystemRoleConstants.ADMIN_ROLE_ID));
  }

  @Test
  void refuseSuppressionDuRoleAdmin() {
    assertThrows(BusinessException.class,
        () -> service.validerSuppressionRole(SystemRoleConstants.ADMIN_ROLE_ID));
  }

  @Test
  void autoriseSuppressionRoleMetierLibre() {
    when(utilisateurRepository.existsByFkRole(12L)).thenReturn(false);
    assertDoesNotThrow(() -> service.validerSuppressionRole(12L));
  }

  @Test
  void refuseNomAdminPourAutreRole() {
    when(roleRepository.findByNom("ADMIN")).thenReturn(Optional.empty());
    assertThrows(BusinessException.class,
        () -> service.validerNomUnique("ADMIN", 99L));
  }

  @Test
  void autoriseNomUniqueRoleMetier() {
    when(roleRepository.findByNom("MEDECIN")).thenReturn(Optional.empty());
    assertDoesNotThrow(() -> service.validerNomUnique("MEDECIN", null));
  }

  @Test
  void refuseDoublonNom() {
    when(roleRepository.findByNom("MEDECIN"))
        .thenReturn(Optional.of(Role.builder().id(3L).nom("MEDECIN").build()));
    assertThrows(BusinessException.class,
        () -> service.validerNomUnique("MEDECIN", null));
  }
}
