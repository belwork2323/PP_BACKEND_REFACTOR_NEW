package in.co.bel.acempp.dto.admin.motor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MotorDiscardResponse {
    private String motorId;
    private String reason;
    private int affectedBatchCount;
    private List<String> affectedBatchIds;
}
