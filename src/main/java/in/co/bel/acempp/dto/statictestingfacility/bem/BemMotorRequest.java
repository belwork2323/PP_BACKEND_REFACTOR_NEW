package in.co.bel.acempp.dto.statictestingfacility.bem;

import in.co.bel.acempp.dto.statictestingfacility.StfPayloads;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.enums.StfMotorSubType;
import in.co.bel.acempp.validation.groups.CreateGroup;
import in.co.bel.acempp.validation.groups.DetailGroup;
import in.co.bel.acempp.validation.groups.UpdateGroup;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BemMotorRequest {
    @NotBlank(
            message = "Motor ID is required",
            groups = {CreateGroup.class, UpdateGroup.class, DetailGroup.class}
    )
    private String motorId;

    @NotBlank(
            message = "STF test number is required",
            groups = {CreateGroup.class}
    )
    private String stfTestNo;

    @Builder.Default
    private StfMotorSubType subType = StfMotorSubType.BEM;

    @NotNull(
            message = "formSubmissionType is required",
            groups = {CreateGroup.class, UpdateGroup.class}
    )
    private Status formSubmissionType;

    @NotNull(
            message = "subDepartmentId is required",
            groups = {CreateGroup.class}
    )
    private Long subDepartmentId;

    @Valid
    @NotNull(
            message = "Static testing details are required",
            groups = {CreateGroup.class, UpdateGroup.class}
    )
    private StfPayloads.BemStaticTestingDetails staticTestingDetails;

    @Builder.Default
    @Min(value = 1, message = "Page must be greater than 0")
    private Integer page = 1;

    @Builder.Default
    @Min(value = 1, message = "Limit must be greater than 0")
    private Integer limit = 10;

    private List<String> statusFilter; // Renamed to avoid collision with status entity field
    private List<String> status; // Renamed to avoid collision with status entity field

    private String search;

    // Helper methods for safe query building
    public String getCleanSearch() {
        return (search != null && !search.trim().isEmpty()) ? search.trim() : null;
    }

    public List<String> getCleanStatusFilter() {
        return (status != null && !status.isEmpty()) ? status : null;
    }
}
