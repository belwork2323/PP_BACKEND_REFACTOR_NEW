package in.co.bel.acempp.dto.masterdata.qcdivisions;

import in.co.bel.acempp.dto.general.PersonInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QcDivisionMasterRecordDto {
    private String id;
    private Long divisionId;
    private String divisionName;
    private Integer displayOrder;
    private Boolean isActive;
    @Builder.Default
    private List<DivisionTypeDto> types = new ArrayList<>();

    private PersonInfo createdBy;
    private LocalDateTime createdOn;
    private PersonInfo updatedBy;
    private LocalDateTime updatedOn;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DivisionTypeDto {
        private Long divisionId;
        private String divisionName;
        private Integer displayOrder;
        private Boolean isActive;
    }
}
