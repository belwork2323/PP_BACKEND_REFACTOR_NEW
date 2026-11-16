package in.co.bel.acempp.dto.masterdata.qcdivisions;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QcDivisionMasterUpdateRequest {
    @NotBlank
    private String id;

    private Long divisionId;

    @NotBlank
    private String divisionName;

    private Integer displayOrder;

    @NotNull
    private Boolean isActive;

    @Valid
    @Builder.Default
    private List<QcDivisionMasterCreateRequest.DivisionTypeWriteDto> types = new ArrayList<>();
}
