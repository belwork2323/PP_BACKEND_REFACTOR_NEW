package in.co.bel.acempp.controller.manufacturing;

import in.co.bel.acempp.dto.AcemResponse;
import in.co.bel.acempp.dto.casepreparation.*;
import in.co.bel.acempp.exception.AcemTemplate;
import in.co.bel.acempp.service.manufacturing.CasePreparationService;
import in.co.bel.acempp.validation.DtoValidationService;
import in.co.bel.acempp.validation.groups.CreateGroup;
import in.co.bel.acempp.validation.groups.DetailGroup;
import in.co.bel.acempp.validation.groups.UpdateGroup;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user/case-preparation")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('User')")
public class CasePreparationController {

    private final CasePreparationService casePrepService;
    private final DtoValidationService dtoValidationService;

    @PostMapping("/form/create")
    public ResponseEntity<AcemResponse<CasePreparationResponse>> createForm(
            @Validated(CreateGroup.class) @RequestBody CreateCasePreparationRequest request,
            Authentication authentication) {
        String userId = (String) authentication.getDetails();
        dtoValidationService.validateDto(request, request.getFormSubmissionType());
        if (request.getCasePreparationDetails() != null
                && request.getCasePreparationDetails().getMotors() != null) {
            for (CasePreparationMotorRequest motor : request.getCasePreparationDetails().getMotors()) {
                dtoValidationService.validateDto(motor, motor.getMotorSubmissionType());
            }
        }
        CasePreparationResponse response = casePrepService.createForm(request, userId);
        return AcemTemplate.created(response, "Case preparation form submitted successfully");
    }

    @PutMapping("/form/update")
    public ResponseEntity<AcemResponse<CasePreparationResponse>> updateForm(
            @Validated(UpdateGroup.class) @RequestBody CreateCasePreparationRequest request,
            Authentication authentication) {
        dtoValidationService.validateDto(request, request.getFormSubmissionType());
        if (request.getCasePreparationDetails() != null
                && request.getCasePreparationDetails().getMotors() != null) {
            for (CasePreparationMotorRequest motor : request.getCasePreparationDetails().getMotors()) {
                dtoValidationService.validateDto(motor, motor.getMotorSubmissionType());
            }
        }
        String userId = (String) authentication.getDetails();
        CasePreparationResponse response = casePrepService.updateForm(request, userId);
        return AcemTemplate.success(response, "Case preparation form updated successfully");
    }

    @PreAuthorize("hasAnyAuthority('User', 'Approver', 'System Manager', 'Admin', 'Centre Head')")
    @PostMapping("/form/details")
    public ResponseEntity<AcemResponse<CasePreparationResponse>> getFormDetails(
            @Validated(DetailGroup.class) @RequestBody CreateCasePreparationRequest request) {
        return AcemTemplate.success(casePrepService.getFormDetails(request.getFormId()),
                "Case preparation form details fetched successfully");
    }

    @PreAuthorize("hasAnyAuthority('User', 'Approver', 'System Manager', 'Admin', 'Centre Head')")
    @PostMapping("/liner-ingredients")
    public ResponseEntity<AcemResponse<CasePrepLinerIngredientsResponse>> getLinerIngredients(
            @Valid @RequestBody CasePrepLinerIngredientsRequest request) {
        return AcemTemplate.success(
                casePrepService.getLinerIngredients(request.getLinerType()),
                "Case preparation liner ingredients fetched successfully");
    }

}
