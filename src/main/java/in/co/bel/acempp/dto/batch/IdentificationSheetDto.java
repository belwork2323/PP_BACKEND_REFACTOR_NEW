package in.co.bel.acempp.dto.batch;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import in.co.bel.acempp.validation.ValidationConstants;
import in.co.bel.acempp.validation.groups.CreateGroup;
import in.co.bel.acempp.validation.groups.SubmitGroup;
import in.co.bel.acempp.validation.groups.UpdateGroup;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class IdentificationSheetDto {
    @NotNull(message = "Date is required", groups = {SubmitGroup.class})
    private LocalDate date;

    @NotNull(message = "Batch Size is required", groups = {SubmitGroup.class})
    @Min(value = 1, message = "Batch Size must be at least 1", groups = {CreateGroup.class, UpdateGroup.class, SubmitGroup.class})
    private Integer batchSize;

    @NotBlank(message = "Bonding Sheet No is required", groups = {SubmitGroup.class})
    @Size(max = ValidationConstants.MAX_SHORT, groups = {CreateGroup.class, UpdateGroup.class, SubmitGroup.class})
    @Pattern(
            regexp = ValidationConstants.ALPHABET_WITH_SPECIAL,
            message = "Invalid Bonding Sheet No",
            groups = {CreateGroup.class, UpdateGroup.class, SubmitGroup.class}
    )
    private String bondingSheetNo;

    @NotNull(message = "Mixer Type is required", groups = {SubmitGroup.class})
    @JsonDeserialize(using = MixerTypeDeserializer.class)
    private MasterDataDto mixerType;

    @NotNull(message = "Number of Premix is required", groups = {SubmitGroup.class})
    @Min(value = 1, message = "Number of Premix must be at least 1", groups = {CreateGroup.class, UpdateGroup.class, SubmitGroup.class})
    private Integer numberOfPremix;

    @NotBlank(message = "Remarks is required", groups = {SubmitGroup.class})
    @Size(max = ValidationConstants.MAX_LONG_TEXT, groups = {CreateGroup.class, UpdateGroup.class, SubmitGroup.class})
    @Pattern(
            regexp = ValidationConstants.ALPHABET_WITH_SPECIAL,
            message = "Invalid characters in remarks",
            groups = {CreateGroup.class, UpdateGroup.class, SubmitGroup.class}
    )
    private String remarks;

    // Optional per UI rules
    private LocalDate prcApprovalDate;

    @Valid
    @NotEmpty(message = "At least one material is required", groups = {SubmitGroup.class})
    private List<
            @NotNull(message = "Material entry cannot be null", groups = {CreateGroup.class, UpdateGroup.class, SubmitGroup.class})
            @Valid
                    BatchMaterialDto
            > materials;

    private BatchMetadata metadata;

}