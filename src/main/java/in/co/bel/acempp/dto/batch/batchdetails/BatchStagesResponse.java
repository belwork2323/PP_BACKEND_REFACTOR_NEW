package in.co.bel.acempp.dto.batch.batchdetails;

import lombok.*;

@Data
@Builder
public class BatchStagesResponse {
//    private BatchHeaderDto batch;
    private BatchDeptStagesDto batch;
}