package in.co.bel.acempp.entity.manufacturing;
import com.fasterxml.jackson.annotation.JsonInclude;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.subscale.subscalecreation.*;
import in.co.bel.acempp.enums.Status;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "subscale_processing")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SubscaleProcessing {

    @Id
    private String id;

    @Indexed(unique = true)
    private String formId;                     // SSP-2026-0001

    @Indexed(unique = true)
    private String batchId;                    // one form per batch

    private String batchType;                  // MAIN_SCALE / SUBSCALE
    private Long subDepartmentId;
    private Status formSubmissionType;         // DRAFT / SUBMIT
    private Status status;                     // DRAFT / WAITING_FOR_APPROVAL

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private SubscaleDetailsDTO subscaleDetails;

    private HardwarePreparationDetailsDTO hardwarePreparationDetails;
    private List<HardwarePreparationTableDTO> hardwarePreparationTable;
    private CastingDetailsDTO castingDetails;
    private CuringDetailsDTO curingDetails;
    private NdtDetailsDTO ndtDetails;
    private TrimmingDetailsDTO trimmingDetails;
    private InhibitionDetailsDTO inhibitionDetails;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private StaticTestingDetailsDTO staticTestingDetails;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private MechanicalInterfacePropertiesDTO mechanicalInterfaceProperties;

    private PersonInfo createdBy;
    private LocalDateTime createdAt;
    private PersonInfo submittedBy;
    private LocalDateTime submittedOn;
    private PersonInfo approvedBy;
    private LocalDateTime approvedOn;
    private PersonInfo updatedBy;
    private LocalDateTime updatedAt;
    private String remarks;
    private String rejectionReason;
    private boolean isDeleted = false;
}
