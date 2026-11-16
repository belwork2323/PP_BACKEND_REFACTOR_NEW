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
public class TrimmingDrawingSectionDTO extends SectionDTO<TrimmingDrawingSectionDTO.TrimmingDrawingData> {

    @Valid
    private List<TrimmingDrawingData> sectionData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TrimmingDrawingData {

        @Pattern(
                regexp = "^$|^[A-Za-z0-9\\s\\-\\.,\\/\\n\\r_]+$",
                message = "Drawing description contains invalid characters",
                groups = {DraftGroup.class, SubmitGroup.class}
        )
        private String description;

        @Valid
        private List<DocumentDto> drawingFile;
    }
}
