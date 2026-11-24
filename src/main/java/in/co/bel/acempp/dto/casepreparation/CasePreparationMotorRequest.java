package in.co.bel.acempp.dto.casepreparation;

import in.co.bel.acempp.dto.casepreparation.CasePreparationSectionPayloads.*;
import in.co.bel.acempp.enums.Status;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CasePreparationMotorRequest {
    @NotBlank(message = "Motor ID is required")
    private String motorId;

    @NotNull(message = "PRRC Clearance Date is required")
    private LocalDate prrcClearanceDate;
    private Status motorSubmissionType;
    private Status motorSubmissionStatus;
    private String rejectionReason;
    @Valid
    private AbradingOperation abradingOperation;
    @Valid
    private BellowBonding bellowBonding;
    @Valid
    private TceCleaning tceCleaning;
    @Valid
    private PreHeating preHeating;
    @Valid
    private LinerCoatingOperation linerCoatingOperation;
    @Valid
    private DispatchToCasting dispatchToCasting;
}
