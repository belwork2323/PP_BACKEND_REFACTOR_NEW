package in.co.bel.acempp.dto.user.operations;

import lombok.*;

import java.util.List;

@Data
@Builder
public class MotorListResponse {
    private String batchId;
    private String batchType;
    private String projectId;
    private Integer motorStage;
    private List<MotorDetailDto> motors;
}