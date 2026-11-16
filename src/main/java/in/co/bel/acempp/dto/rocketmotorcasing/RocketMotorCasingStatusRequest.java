package in.co.bel.acempp.dto.rocketmotorcasing;
import in.co.bel.acempp.enums.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
public class RocketMotorCasingStatusRequest {

    @NotBlank
    private String motorCasingId;

    @NotNull
    private Long subDepartmentId;

    @NotNull
    private Status actionType;

    private String remarks;

    private String rejectionReason;
}
