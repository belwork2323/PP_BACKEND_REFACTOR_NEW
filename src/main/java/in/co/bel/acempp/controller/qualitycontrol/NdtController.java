package in.co.bel.acempp.controller.qualitycontrol;
import in.co.bel.acempp.dto.AcemResponse;
import in.co.bel.acempp.dto.ndt.*;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.exception.AcemTemplate;
import in.co.bel.acempp.service.qualitycontrol.NdtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user/ndt")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('User')")

public class NdtController {

    private final NdtService ndtService;

    @PostMapping("/form/create")
    public ResponseEntity<AcemResponse<NdtResponse>> createForm(
            @RequestBody CreateNdtRequest request,
            Authentication authentication) {

        if (request.getBatchId() == null
                || request.getSubDepartmentId() == null
                || request.getFormSubmissionType() == null
                || request.getMotors() == null) {

            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "batchId, subDepartmentId, formSubmissionType, and motors are required"
            );
        }
        Status formSubmissionType =request.getFormSubmissionType();

        String userId = (String) authentication.getDetails();
        NdtResponse response = ndtService.createForm(request, userId);

        String message = formSubmissionType == Status.DRAFT
                ? "NDT form saved as draft successfully"
                : "NDT form submitted successfully";

        return AcemTemplate.created(response, message);
    }
    @PutMapping("/form/update")
    public ResponseEntity<AcemResponse<UpdateNdtResponse>> updateForm(
            @Valid @RequestBody UpdateNdtRequest request,
            Authentication authentication) {

        if (request.getMotors() == null || request.getMotors().isEmpty()) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "At least one motor is required"
            );
        }

        String userId = (String) authentication.getDetails();
        UpdateNdtResponse response = ndtService.updateForm(request, userId);

        String message = request.getFormSubmissionType() == Status.DRAFT
                ? "NDT form updated successfully"
                : "NDT form submitted successfully";

        return AcemTemplate.success(response, message);
    }

    @PreAuthorize("hasAnyAuthority('User', 'Approver', 'System Manager', 'Admin', 'Centre Head')")
    @PostMapping("/form/details")
    public ResponseEntity<AcemResponse<NdtDetailsResponse>> getFormDetails(
            @Valid @RequestBody NdtDetailsRequest request) {
        return AcemTemplate.success(
                ndtService.getFormDetails(request.getFormId()),
                "NDT form details fetched successfully");
    }

}