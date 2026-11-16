package in.co.bel.acempp.entity;

import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.service.masterdata.MasterDataAuditable;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "dimensional_parameters")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DimensionalParameter implements MasterDataAuditable {

    @Id
    private String id;

    private Long parameterId;

    private String projectId;

    private Integer motorType;

    private String paramId;

    private String paramName;

    private Double minValue;

    private Double maxValue;

    private String unit;

    /** Null treated as active for documents created before soft-disable. */
    private Boolean isActive;

    private PersonInfo createdBy;
    private LocalDateTime createdOn;
    private PersonInfo updatedBy;
    private LocalDateTime updatedOn;
}
