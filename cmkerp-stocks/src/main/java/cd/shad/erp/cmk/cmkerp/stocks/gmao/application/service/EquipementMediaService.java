package cd.shad.erp.cmk.cmkerp.stocks.gmao.application.service;

import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.application.dto.response.EquipementMediaResponse;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.domain.model.EquipementMedia;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.domain.model.EquipementMedia.TypeMedia;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.infrastructure.persistence.EquipementMediaJdbcRepository;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.infrastructure.storage.GmaoMediaStorageService;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class EquipementMediaService {

  private static final long MAX_BYTES = 25L * 1024 * 1024;
  private static final Set<String> ALLOWED_TYPES = Set.of(
      "image/jpeg", "image/png", "image/webp", "image/gif",
      "application/pdf",
      "video/mp4", "video/webm",
      "application/msword",
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      "application/vnd.ms-excel",
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

  private final EquipementMediaJdbcRepository mediaRepository;
  private final EquipementService equipementService;
  private final GmaoMediaStorageService storageService;

  @Transactional(readOnly = true)
  public List<EquipementMediaResponse> listByEquipement(Long fkEquipement) {
    equipementService.require(fkEquipement);
    return mediaRepository.findByEquipement(fkEquipement).stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public EquipementMedia require(Long id) {
    return mediaRepository.findById(id)
        .orElseThrow(() -> NotFoundException.entity("EquipementMedia", id));
  }

  @Transactional
  public EquipementMediaResponse upload(Long fkEquipement, MultipartFile file, String typeMedia,
      String legende, Boolean estPrincipal, Long userId) {
    equipementService.require(fkEquipement);
    if (file == null || file.isEmpty()) {
      throw new BusinessException("Aucun fichier fourni");
    }
    if (file.getSize() > MAX_BYTES) {
      throw new BusinessException("Fichier trop volumineux (max 25 Mo)");
    }

    String contentType = normalizeContentType(file);
    if (!ALLOWED_TYPES.contains(contentType) && !contentType.startsWith("image/")) {
      throw new BusinessException("Type de fichier non autorisé : " + contentType);
    }

    TypeMedia type = resolveType(typeMedia, contentType);
    String original = StringUtils.cleanPath(
        file.getOriginalFilename() != null ? file.getOriginalFilename() : "fichier");
    String safeName = sanitizeFileName(original);
    String storageKey = fkEquipement + "/" + UUID.randomUUID() + "_" + safeName;

    try (InputStream in = file.getInputStream()) {
      storageService.store(storageKey, in);
    } catch (Exception ex) {
      throw new BusinessException("Échec de l'enregistrement du fichier : " + ex.getMessage());
    }

    boolean principal = Boolean.TRUE.equals(estPrincipal)
        || (type == TypeMedia.PHOTO && mediaRepository.countByEquipement(fkEquipement) == 0);
    if (principal) {
      mediaRepository.clearPrincipal(fkEquipement);
    }

    EquipementMedia entity = EquipementMedia.builder()
        .fkEquipement(fkEquipement)
        .typeMedia(type)
        .nomFichier(safeName)
        .nomOriginal(original)
        .contentType(contentType)
        .tailleOctets(file.getSize())
        .storageKey(storageKey)
        .legende(StringUtils.hasText(legende) ? legende.trim() : null)
        .estPrincipal(principal)
        .userCreateId(userId)
        .build();

    Long id = mediaRepository.insert(entity);
    return toResponse(require(id));
  }

  @Transactional
  public EquipementMediaResponse setPrincipal(Long mediaId) {
    EquipementMedia media = require(mediaId);
    mediaRepository.setPrincipal(mediaId, media.getFkEquipement());
    return toResponse(require(mediaId));
  }

  @Transactional
  public void delete(Long mediaId) {
    EquipementMedia media = require(mediaId);
    storageService.delete(media.getStorageKey());
    mediaRepository.delete(mediaId);
  }

  @Transactional(readOnly = true)
  public MediaContent loadContent(Long mediaId) {
    EquipementMedia media = require(mediaId);
    InputStream stream = storageService.open(media.getStorageKey());
    MediaType mediaType = MediaType.parseMediaType(media.getContentType());
    return new MediaContent(new InputStreamResource(stream), mediaType, media.getNomOriginal(),
        media.getTailleOctets());
  }

  public Long findPrincipalId(Long fkEquipement) {
    return mediaRepository.findPrincipalId(fkEquipement);
  }

  public int countByEquipement(Long fkEquipement) {
    return mediaRepository.countByEquipement(fkEquipement);
  }

  private EquipementMediaResponse toResponse(EquipementMedia m) {
    boolean image = m.getContentType() != null && m.getContentType().startsWith("image/");
    return EquipementMediaResponse.builder()
        .id(m.getId())
        .fkEquipement(m.getFkEquipement())
        .typeMedia(m.getTypeMedia().name())
        .nomFichier(m.getNomFichier())
        .nomOriginal(m.getNomOriginal())
        .contentType(m.getContentType())
        .tailleOctets(m.getTailleOctets())
        .legende(m.getLegende())
        .estPrincipal(m.isEstPrincipal())
        .image(image)
        .contentUrl("/api/v1/gmao/medias/" + m.getId() + "/content")
        .dateCreate(m.getDateCreate())
        .build();
  }

  private static String normalizeContentType(MultipartFile file) {
    String ct = file.getContentType();
    if (!StringUtils.hasText(ct) || "application/octet-stream".equals(ct)) {
      String name = file.getOriginalFilename() != null
          ? file.getOriginalFilename().toLowerCase(Locale.ROOT) : "";
      if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
      if (name.endsWith(".png")) return "image/png";
      if (name.endsWith(".webp")) return "image/webp";
      if (name.endsWith(".gif")) return "image/gif";
      if (name.endsWith(".pdf")) return "application/pdf";
      if (name.endsWith(".mp4")) return "video/mp4";
      if (name.endsWith(".webm")) return "video/webm";
      return "application/octet-stream";
    }
    return ct.toLowerCase(Locale.ROOT);
  }

  private static TypeMedia resolveType(String requested, String contentType) {
    if (StringUtils.hasText(requested)) {
      try {
        return TypeMedia.valueOf(requested.trim().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException ex) {
        throw new BusinessException("Type média invalide : " + requested);
      }
    }
    if (contentType.startsWith("image/")) return TypeMedia.PHOTO;
    if (contentType.startsWith("video/")) return TypeMedia.VIDEO;
    if (contentType.equals("application/pdf")) return TypeMedia.DOCUMENT;
    return TypeMedia.DOCUMENT;
  }

  private static String sanitizeFileName(String name) {
    String base = name.replace("\\", "/");
    int slash = base.lastIndexOf('/');
    if (slash >= 0) {
      base = base.substring(slash + 1);
    }
    base = base.replaceAll("[^a-zA-Z0-9._\\-]", "_");
    if (base.isBlank()) {
      base = "fichier";
    }
    if (base.length() > 180) {
      base = base.substring(base.length() - 180);
    }
    return base;
  }

  public record MediaContent(
      InputStreamResource resource,
      MediaType contentType,
      String filename,
      long size) {}
}
