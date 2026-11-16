package in.co.bel.acempp.dto.masterdata.qualitychecks;

import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.mixing.qualitychecks.QualityCheckParams;
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
public class QualityCheckMasterRecordDto {
    private Long id;
    private String qualityCheckCode;
    private String mixType;
    private Integer motorStage;
    private Boolean isActive;
    @Builder.Default
    private List<QualityCheckParams> qualityChecks = new ArrayList<>();

    private PersonInfo createdBy;
    private LocalDateTime createdOn;
    private PersonInfo updatedBy;
    private LocalDateTime updatedOn;
}
