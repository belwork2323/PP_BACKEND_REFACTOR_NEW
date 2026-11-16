package in.co.bel.acempp.dto.rocketmotorcasing;
import in.co.bel.acempp.enums.Status;
import lombok.*;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RmcFormResponse {

    private String motorCasingId;
    private Status status;
}
