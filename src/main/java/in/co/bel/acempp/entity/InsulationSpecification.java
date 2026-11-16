package in.co.bel.acempp.entity;

import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments.CategoryDto;
import in.co.bel.acempp.service.masterdata.MasterDataAuditable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "insulation_specifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsulationSpecification implements MasterDataAuditable {

    @Id
    private String id;

    private Long insulationSpecId;

    private String code;

    private String insulationType;

    private List<CategoryDto> specifications;

    /** Null treated as active for documents created before soft-disable. */
    private Boolean isActive;

    private PersonInfo createdBy;
    private LocalDateTime createdOn;
    private PersonInfo updatedBy;
    private LocalDateTime updatedOn;
}