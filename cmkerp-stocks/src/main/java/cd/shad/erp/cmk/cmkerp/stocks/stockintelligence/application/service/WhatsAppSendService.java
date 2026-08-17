package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.WhatsAppSendDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.WhatsAppSendRequest;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.persistence.WhatsAppSendRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class WhatsAppSendService {

  private final WhatsAppSendRepository repository;

  @Transactional(readOnly = true)
  public List<WhatsAppSendDTO> findAll() {
    return repository.findAll();
  }

  @Transactional(readOnly = true)
  public List<String> findActivePhones() {
    return repository.findActivePhones();
  }

  @Transactional(readOnly = true)
  public WhatsAppSendDTO findById(Long id) {
    return repository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Numéro introuvable"));
  }

  public WhatsAppSendDTO create(WhatsAppSendRequest request) {
    String phone = normalizePhone(request.phone());
    if (repository.existsByPhone(phone, null)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce numéro existe déjà");
    }
    boolean active = request.actif() == null || Boolean.TRUE.equals(request.actif());
    Long id = repository.insert(phone, trimLabel(request.label()), active);
    return new WhatsAppSendDTO(id, phone, trimLabel(request.label()), active);
  }

  public WhatsAppSendDTO update(Long id, WhatsAppSendRequest request) {
    findById(id);
    String phone = normalizePhone(request.phone());
    if (repository.existsByPhone(phone, id)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce numéro existe déjà");
    }
    boolean active = Boolean.TRUE.equals(request.actif());
    if (!repository.update(id, phone, trimLabel(request.label()), active)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Numéro introuvable");
    }
    return findById(id);
  }

  public WhatsAppSendDTO toggleActive(Long id, boolean actif) {
    WhatsAppSendDTO current = findById(id);
    if (!repository.update(id, current.phone(), current.label(), actif)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Numéro introuvable");
    }
    return findById(id);
  }

  public void delete(Long id) {
    if (!repository.deleteById(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Numéro introuvable");
    }
  }

  static String normalizePhone(String phone) {
    if (phone == null || phone.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Numéro requis");
    }
    String digits = phone.replaceAll("[^0-9]", "");
    if (digits.length() < 9) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Numéro invalide (min. 9 chiffres)");
    }
    return digits;
  }

  private static String trimLabel(String label) {
    if (label == null || label.isBlank()) {
      return null;
    }
    return label.trim();
  }
}
