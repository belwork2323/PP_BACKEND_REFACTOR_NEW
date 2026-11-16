package in.co.bel.acempp.service.masterdata;

import in.co.bel.acempp.dto.masterdata.MasterDataCreateRequest;
import in.co.bel.acempp.dto.masterdata.MasterDataRecordDto;
import in.co.bel.acempp.dto.masterdata.MasterDataUpdateRequest;

import java.util.List;

public interface MasterDataTypeHandler {

    String type();

    List<MasterDataRecordDto> listAll();

    MasterDataRecordDto create(MasterDataCreateRequest request);

    MasterDataRecordDto update(MasterDataUpdateRequest request);

    MasterDataRecordDto softDisable(Long id);

    /**
     * Re-activate a soft-disabled record. Override this instead of relying on
     * {@link #update} when the type requires name/attributes on update.
     */
    default MasterDataRecordDto enable(Long id) {
        MasterDataUpdateRequest request = MasterDataUpdateRequest.builder()
                .id(id)
                .isActive(true)
                .build();
        return update(request);
    }
}
