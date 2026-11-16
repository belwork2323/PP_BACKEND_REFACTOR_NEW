package in.co.bel.acempp.dto.rocketmotorcasing;

import lombok.*;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AvailableMotorListResponse {

    private String projectId;

    private Integer motorStage;

    private List<AvailableMotorResponse> motors;
}
