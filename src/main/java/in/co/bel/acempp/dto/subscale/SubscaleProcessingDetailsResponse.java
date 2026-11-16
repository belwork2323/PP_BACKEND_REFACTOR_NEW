package in.co.bel.acempp.dto.subscale;

import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.subscale.subscalecreation.*;
import in.co.bel.acempp.enums.Status;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SubscaleProcessingDetailsResponse {
    private String subscaleProcessingId;     // formId
    private String batchId;
    private String batchType;
    private Long subDepartmentId;
    private Status formSubmissionType;
    private Status status;
    private String createdBy;
    private LocalDateTime createdAt;
    private PersonInfo submittedBy;
    private LocalDateTime submittedAt;
    private PersonInfo approvedBy;
    private LocalDateTime approvedOn;
    private SubscaleDetailsDTO subscaleDetails;
    private HardwarePreparationDetailsDTO hardwarePreparationDetails;
    private List<HardwarePreparationTableDTO> hardwarePreparationTable;
    private CastingDetailsDTO castingDetails;
    private CuringDetailsDTO curingDetails;
    private NdtDetailsDTO ndtDetails;
    private TrimmingDetailsDTO trimmingDetails;
    private InhibitionDetailsDTO inhibitionDetails;
    private StaticTestingDetailsDTO staticTestingDetails;
    private MechanicalInterfacePropertiesDTO mechanicalInterfaceProperties;
}
