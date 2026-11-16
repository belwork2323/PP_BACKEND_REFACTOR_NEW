package in.co.bel.acempp.dto.batch;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.Valid;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class IdentificationSheetDto {
    private LocalDate date;
    private Integer batchSize;
    private String bondingSheetNo;
    /** Mixer master ref — request may send code string or object; response returns id/code/name. */
    @JsonDeserialize(using = MixerTypeDeserializer.class)
    private MasterDataDto mixerType;
    private Integer numberOfPremix;
    private String remarks;
    private LocalDate prcApprovalDate;

    @Valid
    private List<BatchMaterialDto> materials;
    private BatchMetadata metadata;

}