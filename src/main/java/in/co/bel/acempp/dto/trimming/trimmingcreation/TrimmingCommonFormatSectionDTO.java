package in.co.bel.acempp.dto.trimming.trimmingcreation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import in.co.bel.acempp.validation.groups.DraftGroup;
import in.co.bel.acempp.validation.groups.SubmitGroup;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class TrimmingCommonFormatSectionDTO extends SectionDTO<TrimmingCommonFormatSectionDTO.CommonFormatData> {

    @Valid
    private List<CommonFormatData> sectionData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CommonFormatData {

        @Pattern(
                regexp = "^$|^[A-Za-z0-9\\/_\\-\\s]+$",
                message = "Parameter ID contains invalid characters",
                groups = {DraftGroup.class, SubmitGroup.class}
        )
        private String parameterId;

        @Pattern(
                regexp = "^$|^[A-Za-z0-9\\/_\\-\\s]+$",
                message = "Parameter Name contains invalid characters",
                groups = {DraftGroup.class, SubmitGroup.class}
        )
        private String parameterName;

        @Valid
        private List<StageData> stages;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StageData {

        @Pattern(
                regexp = "^$|^[A-Za-z0-9\\/_\\-\\s]+$",
                message = "Stage Name contains invalid characters",
                groups = {DraftGroup.class, SubmitGroup.class}
        )
        private String stageName;

        @Valid
        @NotNull(message = "Readings map cannot be null")
        @NotEmpty(message = "Readings map cannot be empty")
        private Map<
                @NotNull(message = "Reading key cannot be null")
                @NotBlank(message = "Reading key cannot be blank")
                @Pattern(
                        regexp = "^[a-zA-Z0-9]+$",
                        message = "Reading key must contain only letters and numbers with no special characters"
                )
                        String,

                @NotNull(message = "Reading value cannot be null")
                @NotBlank(message = "Reading value cannot be blank")
                @Pattern(
                        regexp = "^[a-zA-Z0-9]+$",
                        message = "Reading key must contain only letters and numbers with no special characters"
                )
                        String
                > readings;

        @Pattern(
                regexp = "^$|^[A-Za-z0-9\\/_\\-\\s\\.]+$",
                message = "Specification contains invalid characters",
                groups = {DraftGroup.class, SubmitGroup.class}
        )
        private String specification;
    }
}