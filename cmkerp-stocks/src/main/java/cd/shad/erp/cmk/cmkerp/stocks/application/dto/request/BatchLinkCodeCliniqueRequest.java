package cd.shad.erp.cmk.cmkerp.stocks.application.dto.request;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Liaison multiple CODECLINIQUE en une requête. */
@Data
public class BatchLinkCodeCliniqueRequest {

  @NotEmpty
  @Size(max = 100)
  @Valid
  private List<Item> links = new ArrayList<>();

  @Data
  public static class Item {
    @NotNull
    private Long produitId;

    @NotNull
    @Size(min = 1, max = 255)
    private String codeClinique;
  }
}
