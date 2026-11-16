package in.co.bel.acempp.dto.casepreparation;

import in.co.bel.acempp.dto.casepreparation.CasePreparationSectionPayloads.AbradingOperation;
import in.co.bel.acempp.dto.casepreparation.CasePreparationSectionPayloads.BellowBonding;
import in.co.bel.acempp.dto.casepreparation.CasePreparationSectionPayloads.DispatchToCasting;
import in.co.bel.acempp.dto.casepreparation.CasePreparationSectionPayloads.LinerCoatingOperation;
import in.co.bel.acempp.dto.casepreparation.CasePreparationSectionPayloads.PreHeating;
import in.co.bel.acempp.dto.casepreparation.CasePreparationSectionPayloads.TceCleaning;
import in.co.bel.acempp.enums.Status;
import jakarta.validation.Valid;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Motor {

    private String motorId;

    private LocalDate prrcClearanceDate;

    private Status motorSubmissionType;      // DRAFT / SUBMIT

    private Status motorSubmissionStatus;    // IN_PROGRESS / WAITING_FOR_APPROVAL / APPROVED / REJECTED

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
