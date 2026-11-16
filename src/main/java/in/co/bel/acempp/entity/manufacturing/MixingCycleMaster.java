package in.co.bel.acempp.entity.manufacturing;

import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.mixing.qualitychecks.QualityCheckParams;
import in.co.bel.acempp.service.masterdata.MasterDataAuditable;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "mixing_cycle_master")
@CompoundIndex(name = "projectId_motorStage_idx", def = "{'projectId': 1, 'motorStage': 1}")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MixingCycleMaster implements MasterDataAuditable {

    @Id
    private Long id;

    private String mixingCycleCode;
    private String mixingCycleName;
    private String description;

    private String projectId;
    private Integer motorStage;
    private String motorStageName;

    private CycleGroup cycles;

    /** Null treated as active for documents created before soft-disable. */
    private Boolean isActive;

    private PersonInfo createdBy;
    private LocalDateTime createdOn;
    private PersonInfo updatedBy;
    private LocalDateTime updatedOn;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CycleGroup {

        private List<Operation> premixOperations;

        private List<Operation> finalMixOperations;

        private List<QualityCheckParams> premixQualityChecks;

        private List<QualityCheckParams> finalMixQualityChecks;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Operation {
        private Long operationId;
        private Integer sequenceNo;
        private String operationName;
        /** Null treated as active for operations created before soft-disable. */
        private Boolean isActive;
    }
}
