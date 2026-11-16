package in.co.bel.acempp.service.masterdata;

import in.co.bel.acempp.dto.general.PersonInfo;

import java.time.LocalDateTime;

public interface MasterDataAuditable {

    PersonInfo getCreatedBy();

    void setCreatedBy(PersonInfo createdBy);

    LocalDateTime getCreatedOn();

    void setCreatedOn(LocalDateTime createdOn);

    PersonInfo getUpdatedBy();

    void setUpdatedBy(PersonInfo updatedBy);

    LocalDateTime getUpdatedOn();

    void setUpdatedOn(LocalDateTime updatedOn);
}
