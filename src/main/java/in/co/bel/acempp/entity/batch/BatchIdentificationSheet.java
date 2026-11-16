package in.co.bel.acempp.entity.batch;

import in.co.bel.acempp.dto.batch.BatchMetadata;
import in.co.bel.acempp.dto.batch.MasterDataDto;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder @NoArgsConstructor @AllArgsConstructor
public class BatchIdentificationSheet {
    private LocalDate date;
    private Integer batchSize;
    private String bondingSheetNo;
    private MasterDataDto mixerType;
    private Integer numberOfPremix;
    private String remarks;
    private LocalDate prcApprovalDate;
    private List<BatchMaterial> materials;       // can be empty
    private BatchMetadata metadata;
}
