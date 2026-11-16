package in.co.bel.acempp.dto.trimming.trimmingcreation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import in.co.bel.acempp.dto.rocketmotorcasing.DocumentDto;
import in.co.bel.acempp.validation.groups.DraftGroup;
import in.co.bel.acempp.validation.groups.SubmitGroup;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class TrimmingRemarksSectionDTO extends SectionDTO<TrimmingRemarksSectionDTO.TrimmingRemarksData> {

    @Valid
    private List<TrimmingRemarksData> sectionData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TrimmingRemarksData {

        @Pattern(
                regexp = "^$|^[A-Za-z0-9\\s\\-\\.,\\/\\n\\r_]+$",
                message = "Remarks contain invalid characters",
                groups = {DraftGroup.class, SubmitGroup.class}
        )
        private String remarks;

        @Pattern(
                regexp = "^$|^[A-Za-z0-9\\s\\-\\.,\\/\\n\\r_\\(\\)]+$",
                message = "Report Link contains invalid characters",
                groups = {DraftGroup.class, SubmitGroup.class}
        )
        private List<DocumentDto> reportFile;
    }
}
