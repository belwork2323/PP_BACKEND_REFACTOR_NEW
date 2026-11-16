package in.co.bel.acempp.entity.manufacturing;

import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.service.masterdata.MasterDataAuditable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "curing_cycle_master")
@CompoundIndex(name = "projectId_motorStage_idx", def = "{'projectId': 1, 'motorStage': 1}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CuringCycleMaster implements MasterDataAuditable {

    @Id
    private Long id;

    private String curingCycleCode;

    private String projectId;

    private Integer motorStage;

    private String motorStageName;
    private String curingType;
    private List<Cycle> cycles;

    /** Null treated as active for documents created before soft-disable. */
    private Boolean isActive;

    private PersonInfo createdBy;
    private LocalDateTime createdOn;
    private PersonInfo updatedBy;
    private LocalDateTime updatedOn;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Cycle {

        private Long stepId;
        private Integer sequenceNo;
        private Integer temperature;
        private Integer durationMinutes;
        private LocalDateTime startDateTime;
        private LocalDateTime endDateTime;
        private String hotWaterCirculation;
        /** Null treated as active for steps created before soft-disable. */
        private Boolean isActive;
    }
}
