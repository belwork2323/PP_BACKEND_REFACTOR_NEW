package in.co.bel.acempp.controller.manufacturing;

import in.co.bel.acempp.dto.AcemResponse;
import in.co.bel.acempp.dto.rawmaterialpreparation.*;
import in.co.bel.acempp.exception.AcemTemplate;
import in.co.bel.acempp.service.manufacturing.RawMaterialPrepService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user/raw-material-preparation")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('User')")
public class RawMaterialPrepController {

    private final RawMaterialPrepService preparationService;

    @PostMapping("/form/create")
    public ResponseEntity<AcemResponse<PreparationResponse>> createForm(
            @Valid @RequestBody CreatePrepRequest request,
            Authentication authentication) {
        String userId = (String) authentication.getDetails();
        PreparationResponse response = preparationService.createPreparation(request, userId);
        return AcemTemplate.created(response, "Raw material preparation form submitted successfully");
    }
    @PutMapping("/form/update")
    public ResponseEntity<AcemResponse<PreparationResponse>> updateForm(
            @Valid @RequestBody UpdatePreparationRequest request,
            Authentication authentication) {
        String userId = (String) authentication.getDetails();
        PreparationResponse response = preparationService.updatePreparation(request, userId);
        return AcemTemplate.success(response, "Raw material preparation form updated successfully");
    }
    @PreAuthorize("hasAnyAuthority('User', 'Approver', 'System Manager', 'Admin', 'Centre Head')")
    @PostMapping("/form/details")
    public ResponseEntity<AcemResponse<PreparationFormDetailsResponse>> getFormDetails(
            @Valid @RequestBody FormDetailsRequest request) {
        return AcemTemplate.success(
                preparationService.getFormDetails(request.getFormId()),
                "Raw material preparation form details fetched successfully");
    }
}