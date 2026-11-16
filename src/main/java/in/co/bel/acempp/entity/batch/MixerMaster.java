package in.co.bel.acempp.entity.batch;

import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.service.masterdata.MasterDataAuditable;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "mixer_master")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MixerMaster implements MasterDataAuditable {

    @Id
    private String mongoId;

    private Long id;

    private String code;

    private String name;

    /** Null treated as active for documents created before soft-disable. */
    private Boolean isActive;

    private PersonInfo createdBy;
    private LocalDateTime createdOn;
    private PersonInfo updatedBy;
    private LocalDateTime updatedOn;
}