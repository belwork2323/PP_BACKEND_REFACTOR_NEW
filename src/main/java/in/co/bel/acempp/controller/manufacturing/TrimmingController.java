package in.co.bel.acempp.controller.manufacturing;

import in.co.bel.acempp.dto.AcemResponse;
import in.co.bel.acempp.dto.trimming.*;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.exception.AcemTemplate;
import in.co.bel.acempp.service.manufacturing.TrimmingService;
//import in.co.bel.acempp.validation.DtoValidationService;
import in.co.bel.acempp.validation.groups.CreateGroup;
import in.co.bel.acempp.validation.groups.DetailGroup;
import in.co.bel.acempp.validation.groups.UpdateGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user/trimming")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('User')")
public class TrimmingController {

    private final TrimmingService trimmingService;
//    private final DtoValidationService dtoValidationService;

    @PostMapping("/form/create")
    public ResponseEntity<AcemResponse<TrimmingResponse>> createForm(
            @Validated(CreateGroup.class) @RequestBody TrimmingRequest request,
            Authentication authentication) {


        String userId = (String) authentication.getDetails();
//        dtoValidationService.validateDto(request, request.getFormSubmissionType());
        TrimmingResponse response = trimmingService.createForm(request, userId);

        String message = request.getFormSubmissionType() == Status.DRAFT
                ? "Trimming form saved as draft successfully"
                : "Trimming form submitted successfully";

        return AcemTemplate.created(response, message);
    }

    @PutMapping("/form/update")
    public ResponseEntity<AcemResponse<TrimmingResponse>> updateForm(
            @Validated(UpdateGroup.class) @RequestBody TrimmingRequest request,
            Authentication authentication) {
//        dtoValidationService.validateDto(request, request.getFormSubmissionType());
        String userId = (String) authentication.getDetails();
        TrimmingResponse response = trimmingService.updateForm(request, userId);

        String message = request.getFormSubmissionType() == Status.DRAFT
                ? "Trimming form updated successfully"
                : "Trimming form submitted successfully";

        return AcemTemplate.success(response, message);
    }

    @PreAuthorize("hasAnyAuthority('User', 'Approver', 'System Manager', 'Admin', 'Centre Head')")
    @PostMapping("/form/details")
    public ResponseEntity<AcemResponse<TrimmingDetailsResponse>> getFormDetails(
            @Validated(DetailGroup.class) @RequestBody TrimmingRequest request) {
        TrimmingDetailsResponse data = trimmingService.getFormDetails(request.getFormId());
        return AcemTemplate.success(data, "Trimming form details fetched successfully");
    }
}