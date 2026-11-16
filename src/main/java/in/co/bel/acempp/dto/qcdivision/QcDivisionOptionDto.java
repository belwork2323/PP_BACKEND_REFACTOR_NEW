package in.co.bel.acempp.dto.qcdivision;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QcDivisionOptionDto {
    private Long divisionId;
    private String divisionName;
    private Boolean disabled;
    private List<DivisionTypeOptionDto> types;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DivisionTypeOptionDto {
        private Long divisionId;
        private String divisionName;
        private Boolean disabled;
    }
}
