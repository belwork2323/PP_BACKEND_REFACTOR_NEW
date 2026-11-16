package in.co.bel.acempp.controller.dispatch;
import in.co.bel.acempp.dto.AcemResponse;
import in.co.bel.acempp.dto.dispatch.*;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.exception.AcemTemplate;
import in.co.bel.acempp.service.dispatch.DispatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user/dispatch")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('User')")
public class DispatchController {

    private final DispatchService dispatchService;

    @PostMapping("/form/create")
    public ResponseEntity<AcemResponse<DispatchCreateResponse>> createForm(
            @Valid @RequestBody CreateDispatchRequest request,
            Authentication authentication) {
        String userId = (String) authentication.getDetails();
        DispatchCreateResponse response = dispatchService.createForm(request, userId);

        String message = request.getFormSubmissionType() == Status.DRAFT
                ? "Dispatch form saved as draft successfully"
                : "Dispatch form submitted successfully";

        return AcemTemplate.created(response, message);
    }
    @PutMapping("/form/update")
    public ResponseEntity<AcemResponse<DispatchUpdateResponse>> updateForm(
            @Valid @RequestBody UpdateDispatchRequest request,
            Authentication authentication) {
        String userId = (String) authentication.getDetails();
        DispatchUpdateResponse response = dispatchService.updateForm(request, userId);

        String message = request.getFormSubmissionType() == Status.DRAFT
                ? "Dispatch form updated successfully"
                : "Dispatch form submitted successfully";

        return AcemTemplate.success(response, message);
    }
    @PreAuthorize("hasAnyAuthority('User', 'Approver', 'System Manager', 'Admin', 'Centre Head')")
    @PostMapping("/form/details")
    public ResponseEntity<AcemResponse<DispatchDetailsResponse>> getFormDetails(
            @Valid @RequestBody DispatchDetailsRequest request) {
        return AcemTemplate.success(
                dispatchService.getFormDetails(request.getFormId()),
                "Dispatch record details fetched successfully");
    }

    /**
     * Auto-populate Dispatch seed fields from approved QC Division details
     * (casting date, NDT clearance, propellant properties).
     */
    @PostMapping("/motor-details")
    public ResponseEntity<AcemResponse<DispatchMotorDetailsResponse>> motorDetails(
            @Valid @RequestBody DispatchMotorDetailsRequest request) {
        return AcemTemplate.success(
                dispatchService.autoPopulateFromQcDivision(request),
                "Dispatch motor details populated from approved QC Division");
    }
}
