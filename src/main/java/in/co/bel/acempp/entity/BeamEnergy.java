package in.co.bel.acempp.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.service.masterdata.MasterDataAuditable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "beam_energy_master")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeamEnergy implements MasterDataAuditable {

    @Id
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String id;

    private Long beamEnergyId;

    private String beamEnergyCode;

    private String beamEnergyName;

    private String energyUnit;

    /** Null treated as active for documents created before soft-disable. */
    private Boolean isActive;

    private PersonInfo createdBy;
    private LocalDateTime createdOn;
    private PersonInfo updatedBy;
    private LocalDateTime updatedOn;
}
