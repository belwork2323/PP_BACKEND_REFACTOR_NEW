package in.co.bel.acempp.dto.subscale.subscalecreation;

import in.co.bel.acempp.enums.Status;
import jakarta.validation.Valid;
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

    @Valid
    private SubscaleDetailsDTO subscaleDetails;

    @Valid
    private HardwarePreparationDetailsDTO hardwarePreparationDetails;

    @Valid
    private List<HardwarePreparationTableDTO> hardwarePreparationTable;

    @Valid
    private CastingDetailsDTO castingDetails;

    @Valid
    private CuringDetailsDTO curingDetails;

    @Valid
    private NdtDetailsDTO ndtDetails;

    @Valid
    private TrimmingDetailsDTO trimmingDetails;

    @Valid
    private InhibitionDetailsDTO inhibitionDetails;

    @Valid
    private StaticTestingDetailsDTO staticTestingDetails;

    @Valid
    private MechanicalInterfacePropertiesDTO mechanicalInterfaceProperties;
}