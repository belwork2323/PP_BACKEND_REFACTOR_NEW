package in.co.bel.acempp.dto.dashboard;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AlertDto {
    private String id;
    private String type;
    private String msg;
    private String time;
    private String batchId;
    private String motorId;
    private String stage;
}
