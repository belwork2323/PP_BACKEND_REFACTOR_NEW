package in.co.bel.acempp.entity.batch;
import in.co.bel.acempp.dto.batch.SubscaleArticleInfo;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.casepreparation.MotorStatusResponse;
import in.co.bel.acempp.dto.rawmaterialpreparation.PremixStatusResponse;
import in.co.bel.acempp.dto.qcdivision.QcStageDivisionStatusDto;
import in.co.bel.acempp.enums.Status;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "batches")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Batch {

    @Id
    private String id;

    @Indexed(unique = true)
    private String batchId;                     // BATCH-2026-0001
    private String formId;
    private String batchType;                  // MAIN, SUBSCALE
    private String subBatchType;               // QUALIFICATION, EXPERIMENTAL (nullable)

    // Common fields
    private String projectId;                  // nullable for EXPERIMENTAL
    private String projectName;
    private Integer motorStage;                 // nullable
    private Integer numberOfMotors;
    private List<String> motorIds;
    private List<SubscaleArticleInfo> articles;
    private String objective;

    @Builder.Default
    private Boolean lockedForEdit = false;

    // Identification sheet (embedded)
    private BatchIdentificationSheet identificationSheet;
    private String identificationSheetStatus;   // DRAFT / COMPLETED

    // Status & audit
//    private Status status;                      // Initiated
    private LocalDateTime createdAt;
    // BatchDetailDto (for details endpoint)
    private PersonInfo systemManager;
    private PersonInfo createdBy;
    private PersonInfo updatedBy;
    private LocalDateTime updatedAt;

    /** When true, MAIN batches use parallel premix/motor routing (flow v2). */
    private Boolean parallelFlowEnabled;

    private boolean isDeleted = false;
    private String deleteReason;
    private LocalDateTime deletedAt;
    private PersonInfo deletedBy;

    /** Motors discarded for this batch (admin/centre-head); irreversible in v1. */
    @Builder.Default
    private List<DiscardedMotor> discardedMotors = new ArrayList<>();

    // 🟢 NEW – stage progression
    @Builder.Default
    private List<StageProgress> stageProgress = new ArrayList<>();   // history of all stages
    @Builder.Default
    private List<StageProgress> currentStage = new ArrayList<>();    // filtered active-stage view

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DiscardedMotor {
        private String motorId;
        private String reason;
        private LocalDateTime discardedAt;
        private PersonInfo discardedBy;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StageProgress {
        private Long departmentId;
        private String departmentName;
        private Long subDepartmentId;
        private String subDepartmentName;
        private Status status;            // TO_BE_INITIATED, IN_PROGRESS, APPROVED, REJECTED, etc.
        private LocalDateTime enteredAt;  // when it moved to this stage
        private LocalDateTime updatedAt;  // last status change
        private String remarks;             // ← new
        private String rejectionReason;
        // Submission
        private PersonInfo submittedBy;
        private LocalDateTime submittedOn;

        // Approval
        private PersonInfo approvedBy;
        private LocalDateTime approvedOn;

        private List<PremixStatusResponse> premixStatuses;
        private List<PremixStatusResponse> finalMixStatuses;
        private List<MotorStatusResponse> motorStatuses;
        private List<QcStageDivisionStatusDto> divisionStatuses;
    }

}
