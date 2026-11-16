package in.co.bel.acempp.dto.subscale.subscalecreation;

import in.co.bel.acempp.enums.Status;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class CreateSubscaleProcessingRequest {

    private String batchId;

    private String formId;

    @NotBlank
    private String batchType;

    @NotNull
    private Long subDepartmentId;

    @NotNull
    private Status formSubmissionType;

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