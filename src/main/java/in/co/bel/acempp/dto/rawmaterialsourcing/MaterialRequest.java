package in.co.bel.acempp.dto.rawmaterialsourcing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class MaterialRequest {
    @NotBlank(message = "materialCode is required")
    private String materialCode;

    private String grade;

    private String supplyOrderNo;

    private LocalDate receiptDate;

    @NotBlank(message = "manufacturerName is required")
    private String manufacturerName;

    @NotEmpty(message = "lots list cannot be empty")
    @Valid
    private List<LotRequest> lots;
}