package in.co.bel.acempp.dto.rocketmotorcasing;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RmcDetailsRequest {

    @NotBlank(message = "motorCasingId is required")
    private String motorCasingId;
}