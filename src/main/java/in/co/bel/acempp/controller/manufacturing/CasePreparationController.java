package in.co.bel.acempp.controller.manufacturing;

import in.co.bel.acempp.dto.AcemResponse;
import in.co.bel.acempp.dto.casepreparation.*;
import in.co.bel.acempp.exception.AcemTemplate;
import in.co.bel.acempp.service.manufacturing.CasePreparationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user/case-preparation")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('User')")
public class CasePreparationController {

    private final CasePreparationService casePrepService;

    @PostMapping("/form/create")
    public ResponseEntity<AcemResponse<CasePreparationResponse>> createForm(
            @Valid @RequestBody CreateCasePreparationRequest request,
            Authentication authentication) {
        String userId = (String) authentication.getDetails();
        CasePreparationResponse response = casePrepService.createForm(request, userId);
        return AcemTemplate.created(response, "Case preparation form submitted successfully");
    }
    @PutMapping("/form/update")
    public ResponseEntity<AcemResponse<CasePreparationResponse>> updateForm(
            @Valid @RequestBody UpdateCasePreparationRequest request,
            Authentication authentication) {
        String userId = (String) authentication.getDetails();
        CasePreparationResponse response = casePrepService.updateForm(request, userId);
        return AcemTemplate.success(response, "Case preparation form updated successfully");
    }
    @PreAuthorize("hasAnyAuthority('User', 'Approver', 'System Manager', 'Admin', 'Centre Head')")
    @PostMapping("/form/details")
    public ResponseEntity<AcemResponse<CasePrepFormDetailsResponse>> getFormDetails(
            @Valid @RequestBody CasePrepFormDetailsRequest request) {
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
