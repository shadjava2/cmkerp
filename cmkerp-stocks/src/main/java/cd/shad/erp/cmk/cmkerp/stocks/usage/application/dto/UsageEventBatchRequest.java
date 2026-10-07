package cd.shad.erp.cmk.cmkerp.stocks.usage.application.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UsageEventBatchRequest {

  @NotEmpty
  @Size(max = 50)
  @Valid
  private List<UsageEventItemRequest> events = new ArrayList<>();
}
