package cd.shad.erp.cmk.cmkerp.gateway.dmi;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cd.shad.erp.cmk.cmkerp.gateway.facturation.ErpAyantService;
import cd.shad.erp.cmk.cmkerp.gateway.facturation.ErpFacturationRelationalStore;
import cd.shad.erp.cmk.cmkerp.gateway.mediline.MedilinePatientWriteService;

@Service
public class PatientRegistrationOrchestrator {

  private static final Logger log = LoggerFactory.getLogger(PatientRegistrationOrchestrator.class);

  private final ErpFacturationRelationalStore store;
  private final ErpAyantService ayantService;
  private final MedilinePatientWriteService medilineWrite;
  private final ObjectMapper objectMapper;

  public PatientRegistrationOrchestrator(
      ErpFacturationRelationalStore store,
      ErpAyantService ayantService,
      MedilinePatientWriteService medilineWrite,
      ObjectMapper objectMapper) {
    this.store = store;
    this.ayantService = ayantService;
    this.medilineWrite = medilineWrite;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public Map<String, Object> register(String actor, Long userId, Map<String, Object> body) {
    String requestIdRaw = str(body.get("requestId"));
    if (requestIdRaw == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identifiant de requête obligatoire.");
    }
    UUID requestId;
    try {
      requestId = UUID.fromString(requestIdRaw);
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identifiant de requête invalide.");
    }

  @SuppressWarnings("unchecked")
    Map<String, Object> identity = body.get("identity") instanceof Map<?, ?> m
        ? (Map<String, Object>) m
        : null;
    if (identity == null || str(identity.get("nom")) == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identité patient incomplète.");
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> affiliation = body.get("affiliation") instanceof Map<?, ?> m
        ? (Map<String, Object>) m
        : null;
    boolean skipAyant = affiliation == null || Boolean.TRUE.equals(affiliation.get("skip"));
    Long societeId = skipAyant ? null : longOrNull(affiliation.get("societeId"));
    Long contratId = skipAyant ? null : longOrNull(affiliation.get("contratId"));

    String payloadJson;
    try {
      payloadJson = objectMapper.writeValueAsString(body);
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête invalide.");
    }

    Map<String, Object> existing = store.findPatientRegistrationByRequestId(requestId.toString());
    if (existing == null) {
      store.insertPatientRegistration(requestId.toString(), societeId, contratId, payloadJson, userId);
      existing = store.findPatientRegistrationByRequestId(requestId.toString());
    }

    String medilineIpp = str(existing.get("medilineIpp"));
    String medilineStatus = str(existing.get("medilineStatus"));
    if (!"CREATED".equalsIgnoreCase(medilineStatus)) {
      try {
        if (!skipAyant) {
          if (societeId == null || contratId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Société et contrat obligatoires.");
          }
          if (!store.societeMedilineMappingReady(societeId)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "La société sélectionnée n’est pas reliée à un compte Mediline.");
          }
        }
        String compteCode = null;
        if (societeId != null) {
          compteCode = store.medilineCompteCodeForSociete(societeId).orElse(null);
        }
        if (compteCode == null && str(identity.get("compteCode")) != null) {
          compteCode = str(identity.get("compteCode"));
        }
        Map<String, String> patientFields = patientFieldsFromIdentity(identity, compteCode);
        medilineIpp = medilineWrite.createPatient(actor, requestId, patientFields);
        store.updatePatientRegistrationMediline(requestId.toString(), medilineIpp, "CREATED", null, userId);
        medilineStatus = "CREATED";
      } catch (ResponseStatusException ex) {
        store.updatePatientRegistrationMediline(requestId.toString(), null, "FAILED", ex.getReason(), userId);
        throw ex;
      } catch (RuntimeException ex) {
        store.updatePatientRegistrationMediline(requestId.toString(), null, "FAILED", ex.getMessage(), userId);
        throw ex;
      }
    }

    String ayantStatus = str(existing.get("ayantStatus"));
    Long ayantId = longOrNull(existing.get("ayantId"));
    if (skipAyant) {
      if (!"SKIPPED".equalsIgnoreCase(ayantStatus)) {
        store.updatePatientRegistrationAyant(requestId.toString(), null, "SKIPPED", null, userId);
        ayantStatus = "SKIPPED";
      }
    } else if (!"CREATED".equalsIgnoreCase(ayantStatus)) {
      try {
        Map<String, Object> ayantBody = ayantPayload(medilineIpp, identity, affiliation);
        Map<String, Object> ayant = ayantService.create(ayantBody, userId);
        ayantId = longOrNull(ayant.get("id"));
        store.updatePatientRegistrationAyant(requestId.toString(), ayantId, "CREATED", null, userId);
        ayantStatus = "CREATED";
      } catch (ResponseStatusException ex) {
        store.updatePatientRegistrationAyant(requestId.toString(), null, "FAILED", ex.getReason(), userId);
        log.warn("Création ayant après dossier Mediline {} : {}", medilineIpp, ex.getReason());
        throw ex;
      }
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("requestId", requestId.toString());
    result.put("medilineIpp", medilineIpp);
    result.put("medilineStatus", medilineStatus);
    result.put("ayantId", ayantId);
    result.put("ayantStatus", ayantStatus);
    result.put(
        "message",
        "CREATED".equalsIgnoreCase(ayantStatus)
            ? "Dossier patient et prise en charge enregistrés."
            : "SKIPPED".equalsIgnoreCase(ayantStatus)
                ? "Dossier patient enregistré."
                : "Dossier patient enregistré — affiliation en cours de finalisation.");
    return result;
  }

  private static Map<String, String> patientFieldsFromIdentity(Map<String, Object> identity, String compteCode) {
    Map<String, String> fields = new LinkedHashMap<>();
    putIfPresent(fields, "nom", identity.get("nom"));
    putIfPresent(fields, "postnom", identity.get("postnom"));
    putIfPresent(fields, "prenom", identity.get("prenom"));
    putIfPresent(fields, "sexe", identity.get("sexe"));
    putIfPresent(fields, "dateNaissance", identity.get("dateNaissance"));
    putIfPresent(fields, "lieuNaissance", identity.get("lieuNaissance"));
    putIfPresent(fields, "telephone", identity.get("telephone"));
    putIfPresent(fields, "email", identity.get("email"));
    putIfPresent(fields, "adresse", identity.get("adresse"));
    putIfPresent(fields, "pec", identity.get("pec"));
    putIfPresent(fields, "fonction", identity.get("fonction"));
    putIfPresent(fields, "categorieCode", identity.get("categorieCode"));
    if (compteCode != null) {
      fields.put("compteCode", compteCode);
    }
    return fields;
  }

  private static Map<String, Object> ayantPayload(
      String ipp, Map<String, Object> identity, Map<String, Object> affiliation) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("medilineIpp", ipp);
    body.put("nomComplet", composeName(identity));
    body.put("sexe", identity.get("sexe"));
    body.put("dateNaissance", identity.get("dateNaissance"));
    body.put("nationalite", affiliation.get("nationalite"));
    body.put("societeId", affiliation.get("societeId"));
    body.put("contratId", affiliation.get("contratId"));
    body.put("relation", affiliation.getOrDefault("relation", "A"));
    body.put("titulaireId", affiliation.get("titulaireId"));
    body.put("titulaireIpp", affiliation.get("titulaireIpp"));
    body.put("locationId", affiliation.get("locationId"));
    body.put("source", affiliation.getOrDefault("source", "CM"));
    body.put("assurance", affiliation.get("assurance"));
    body.put("covid", affiliation.get("covid"));
    return body;
  }

  private static String composeName(Map<String, Object> identity) {
    String explicit = str(identity.get("nomComplet"));
    if (explicit != null) {
      return explicit;
    }
    String nom = str(identity.get("nom"));
    String postnom = str(identity.get("postnom"));
    String prenom = str(identity.get("prenom"));
    return String.join(" ", java.util.stream.Stream.of(nom, postnom, prenom)
        .filter(s -> s != null && !s.isBlank())
        .toList()).trim();
  }

  private static void putIfPresent(Map<String, String> target, String key, Object raw) {
    String v = str(raw);
    if (v != null) {
      target.put(key, v);
    }
  }

  private static String str(Object raw) {
    if (raw == null) {
      return null;
    }
    String text = String.valueOf(raw).trim();
    return text.isEmpty() || "null".equalsIgnoreCase(text) ? null : text;
  }

  private static Long longOrNull(Object raw) {
    if (raw == null) {
      return null;
    }
    if (raw instanceof Number number) {
      return number.longValue();
    }
    try {
      return Long.parseLong(String.valueOf(raw).trim());
    } catch (NumberFormatException ex) {
      return null;
    }
  }
}