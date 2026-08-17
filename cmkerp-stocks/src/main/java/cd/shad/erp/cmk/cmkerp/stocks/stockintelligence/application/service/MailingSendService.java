package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.MailingSendDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.MailingSendRequest;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.persistence.MailingSendRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class MailingSendService {

  private final MailingSendRepository repository;

  @Transactional(readOnly = true)
  public List<MailingSendDTO> findAll() {
    return repository.findAll();
  }

  @Transactional(readOnly = true)
  public MailingSendDTO findById(Long id) {
    return repository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destinataire introuvable"));
  }

  public MailingSendDTO create(MailingSendRequest request) {
    String mail = normalizeMail(request.mail());
    if (repository.existsByMail(mail, null)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet email existe déjà");
    }
    boolean active = Boolean.TRUE.equals(request.actif());
    Long id = repository.insert(mail, active);
    return new MailingSendDTO(id, mail, active);
  }

  public MailingSendDTO update(Long id, MailingSendRequest request) {
    findById(id);
    String mail = normalizeMail(request.mail());
    if (repository.existsByMail(mail, id)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet email existe déjà");
    }
    if (!repository.update(id, mail, Boolean.TRUE.equals(request.actif()))) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Destinataire introuvable");
    }
    return findById(id);
  }

  public MailingSendDTO toggleActive(Long id, boolean actif) {
    MailingSendDTO current = findById(id);
    if (!repository.update(id, current.mail(), actif)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Destinataire introuvable");
    }
    return findById(id);
  }

  public void delete(Long id) {
    if (!repository.deleteById(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Destinataire introuvable");
    }
  }

  private String normalizeMail(String mail) {
    if (mail == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email requis");
    }
    return mail.trim().toLowerCase();
  }
}
