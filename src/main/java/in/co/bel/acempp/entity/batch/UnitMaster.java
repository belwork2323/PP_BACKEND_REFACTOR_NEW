package in.co.bel.acempp.entity.batch;

import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.service.masterdata.MasterDataAuditable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "unit_master")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnitMaster implements MasterDataAuditable {

    @Id
    private String mongoId;

    private Long id;

    private String code;

    private String name;

    private String unitCode;

    private String symbol;

    private String description;

    /** Null treated as active for documents created before soft-disable. */
    private Boolean isActive;

    private PersonInfo createdBy;
    private LocalDateTime createdOn;
    private PersonInfo updatedBy;
    private LocalDateTime updatedOn;
}
