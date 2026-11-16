package in.co.bel.acempp.dto.trimming.trimmingcreation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import in.co.bel.acempp.validation.groups.DraftGroup;
import in.co.bel.acempp.validation.groups.SubmitGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "sectionId",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TrimmingDetailsSectionDTO.class, name = "TRIMMING_DETAILS"),
        @JsonSubTypes.Type(value = TrimmingCommonFormatSectionDTO.class, name = "TRIMMING_COMMON_FORMAT"),
        @JsonSubTypes.Type(value = TrimmingDrawingSectionDTO.class, name = "TRIMMING_DRAWING"),
        @JsonSubTypes.Type(value = TrimmingRemarksSectionDTO.class, name = "TRIMMING_REMARKS")
})
@AllArgsConstructor
@NoArgsConstructor
public abstract class SectionDTO<T> {

    @NotBlank(message = "Section ID is required", groups = {SubmitGroup.class})
    @Pattern(
            regexp = "^$|^[A-Za-z0-9\\/_\\-\\s]+$",
            message = "Section ID contains invalid characters",
            groups = {DraftGroup.class, SubmitGroup.class}
    )
    private String sectionId;

    public abstract List<T> getSectionData();
    public abstract void setSectionData(List<T> sectionData);
}
