package in.co.bel.acempp.dto.batch;

import in.co.bel.acempp.enums.MaterialType;
import in.co.bel.acempp.enums.RmpFormTemplate;
import in.co.bel.acempp.validation.ValidationConstants;
import in.co.bel.acempp.validation.groups.CreateGroup;
import in.co.bel.acempp.validation.groups.SubmitGroup;
import in.co.bel.acempp.validation.groups.UpdateGroup;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class BatchMaterialDto {

    private Integer srNo;

    private Long materialId;

    @NotBlank(message = "Material Code is required", groups = {CreateGroup.class, UpdateGroup.class, SubmitGroup.class})
    @Size(max = ValidationConstants.MAX_SHORT, groups = {CreateGroup.class, UpdateGroup.class, SubmitGroup.class})
    @Pattern(
            regexp = ValidationConstants.ALPHABET_WITH_SPECIAL,
            message = "Invalid Material Code",
            groups = {CreateGroup.class, UpdateGroup.class, SubmitGroup.class}
    )
    private String materialCode;

    @Size(max = ValidationConstants.MAX_SHORT, groups = {CreateGroup.class, UpdateGroup.class, SubmitGroup.class})
    private String gradeCode;

    @Size(max = ValidationConstants.MAX_STANDARD, groups = {CreateGroup.class, UpdateGroup.class, SubmitGroup.class})
    private String gradeName;

    private String materialName;

    private MaterialType materialType;
    private RmpFormTemplate rmpFormTemplate;

    @NotEmpty(message = "At least one Lot ID is required", groups = {CreateGroup.class, UpdateGroup.class, SubmitGroup.class})
    private List<
            @NotBlank(message = "Lot ID cannot be blank", groups = {CreateGroup.class, UpdateGroup.class, SubmitGroup.class})
            @Size(max = ValidationConstants.MAX_SHORT, groups = {CreateGroup.class, UpdateGroup.class, SubmitGroup.class})
                    String
            > lotIds;

    @Size(max = ValidationConstants.MAX_STANDARD, groups = {CreateGroup.class, UpdateGroup.class, SubmitGroup.class})
    private String make;

    @NotNull(message = "Required Composition is required", groups = {CreateGroup.class, UpdateGroup.class, SubmitGroup.class})
    @DecimalMin(value = "0.01", message = "Composition must be ≥ 0.01", groups = {CreateGroup.class, UpdateGroup.class, SubmitGroup.class})
    @DecimalMax(value = "100.00", message = "Composition must be ≤ 100", groups = {CreateGroup.class, UpdateGroup.class, SubmitGroup.class})
    private Double requiredComposition;

    @NotNull(message = "Quantity per Premix is required", groups = {CreateGroup.class, UpdateGroup.class, SubmitGroup.class})
    @DecimalMin(value = "0.0", inclusive = true, message = "Quantity per Premix cannot be negative", groups = {CreateGroup.class, UpdateGroup.class, SubmitGroup.class})
    private Double quantityPerPremix;

    // Optional per UI rules
    private LocalDate revalidationFromDate;
    private LocalDate revalidationToDate;
}