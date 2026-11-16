package in.co.bel.acempp.entity.qualitycontrol;

import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.service.masterdata.MasterDataAuditable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "qc_division_master")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QcDivisionMaster implements MasterDataAuditable {

    @Id
    private String id;

    @Indexed(unique = true)
    private Long divisionId;

    private String divisionName;
    private Integer displayOrder;
    private Boolean disabled;

    @Builder.Default
    private List<DivisionType> types = new ArrayList<>();

    private PersonInfo createdBy;
    private LocalDateTime createdOn;
    private PersonInfo updatedBy;
    private LocalDateTime updatedOn;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DivisionType {
        private Long divisionId;
        private String divisionName;
        private Integer displayOrder;
        private Boolean disabled;
    }
}
