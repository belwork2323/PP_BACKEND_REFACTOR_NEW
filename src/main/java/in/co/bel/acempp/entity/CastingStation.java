package in.co.bel.acempp.entity;

import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.service.masterdata.MasterDataAuditable;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "casting_stations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CastingStation implements MasterDataAuditable {

    @Id
    private String id;

    @Indexed(unique = true)
    private Long stationId;

    @Indexed(unique = true)
    private String stationCode;

    private String stationName;

    private String stationType;

    private String status;

    private PersonInfo createdBy;
    private LocalDateTime createdOn;
    private PersonInfo updatedBy;
    private LocalDateTime updatedOn;
}
