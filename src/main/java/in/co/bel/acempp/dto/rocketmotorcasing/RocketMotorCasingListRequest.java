package in.co.bel.acempp.dto.rocketmotorcasing;
import in.co.bel.acempp.enums.CasingType;
//import in.co.bel.acempp.enums.MotorStage;
import in.co.bel.acempp.enums.Status;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
public class RocketMotorCasingListRequest {

    @NotNull
    private Long  subDepartmentId;

    @NotNull
    private Integer page;

    @NotNull
    private Integer limit;

    private List<Status> status;
    private List<Integer> motorStage;
    private String projectId;

    private List<CasingType> casingType;
    private List<String> insulationType;
    private String search;
    private LocalDate fromDate;
    private LocalDate toDate;
}