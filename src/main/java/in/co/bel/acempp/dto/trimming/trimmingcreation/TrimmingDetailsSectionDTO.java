package in.co.bel.acempp.dto.trimming.trimmingcreation;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import in.co.bel.acempp.validation.groups.DraftGroup;
import in.co.bel.acempp.validation.groups.SubmitGroup;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class TrimmingDetailsSectionDTO extends SectionDTO<TrimmingDetailsSectionDTO.TrimmingDetailData> {

    @Valid
    private List<TrimmingDetailData> sectionData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TrimmingDetailData {

        @Pattern(
                regexp = "^$|^[A-Za-z0-9\\/_\\-\\s]+$",
                message = "Machine Details contains invalid characters",
                groups = {DraftGroup.class, SubmitGroup.class}
        )
        private String machineDetails;

        @JsonAlias({"startDate", "startDateTime", "startdatetime"})
        @Field("startDate")
        private LocalDateTime startDatetime;

        @JsonAlias({"completionDate", "completionDateTime", "completiondatetime"})
        @Field("completionDate")
        private LocalDateTime completionDatetime;

        @Pattern(
                regexp = "^$|^[A-Za-z0-9\\/_\\-\\s\\.]+$",
                message = "Arbor Size contains invalid characters",
                groups = {DraftGroup.class, SubmitGroup.class}
        )
        private String arborSize;

        @Pattern(
                regexp = "^$|^[A-Za-z0-9\\/_\\-\\s\\.]+$",
                message = "Cutter Size contains invalid characters",
                groups = {DraftGroup.class, SubmitGroup.class}
        )
        private String cutterSize;

        @Pattern(
                regexp = "^$|^[A-Za-z0-9\\/_\\-\\s\\.]+$",
                message = "Ingest Size contains invalid characters",
                groups = {DraftGroup.class, SubmitGroup.class}
        )
        private String ingestSize;

        @Pattern(
                regexp = "^$|^[A-Za-z0-9]+$",
                message = "Motor Truing must contain only alphanumeric characters",
                groups = {DraftGroup.class, SubmitGroup.class}
        )
        private String motorTruing;

        @Pattern(
                regexp = "^$|^[A-Za-z0-9]+$",
                message = "Arbor Turing must contain only alphanumeric characters",
                groups = {DraftGroup.class, SubmitGroup.class}
        )
        private String arborTuring;

        @Pattern(
                regexp = "^$|^[A-Za-z0-9\\s\\-\\.,\\/\\n\\r_]+$",
                message = "Remarks contain invalid characters",
                groups = {DraftGroup.class, SubmitGroup.class}
        )
        private String remarks;
    }
}
