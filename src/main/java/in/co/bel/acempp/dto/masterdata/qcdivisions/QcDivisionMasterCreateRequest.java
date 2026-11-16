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
public class QcDivisionMasterCreateRequest {
    private Long divisionId;

    @NotBlank
    private String divisionName;

    private Integer displayOrder;
    private Boolean isActive;

    @Valid
    @Builder.Default
    private List<DivisionTypeWriteDto> types = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DivisionTypeWriteDto {
        private Long divisionId;
        @NotBlank
        private String divisionName;
        private Integer displayOrder;
        private Boolean isActive;
    }
}
