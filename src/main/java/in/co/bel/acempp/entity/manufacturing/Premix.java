package in.co.bel.acempp.entity.manufacturing;

import in.co.bel.acempp.enums.Status;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder @NoArgsConstructor @AllArgsConstructor
public class Premix {
    private Integer premixNo;
    private LocalDate premixDate;
    private String materialType;             // BOTH, SOLID, LIQUID

    private Status premixSubmissionType;
    private Status premixSubmissionStatus;

    private List<MaterialProcess> solidProcess;
    private List<MaterialProcess> liquidProcess;
}