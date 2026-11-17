package in.co.bel.acempp.controller.manufacturing;

import in.co.bel.acempp.dto.AcemResponse;
import in.co.bel.acempp.dto.subscale.*;
import in.co.bel.acempp.dto.subscale.subscalecreation.CreateSubscaleProcessingRequest;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.exception.AcemTemplate;
import in.co.bel.acempp.service.manufacturing.SubscaleProcessingService;
import in.co.bel.acempp.validation.DtoValidationService;
import in.co.bel.acempp.validation.groups.CreateGroup;
import in.co.bel.acempp.validation.groups.UpdateGroup;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user/subscale")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('User')")

public class SubscaleProcessingController {

    private final SubscaleProcessingService service;
    private final DtoValidationService dtoValidationService;

    @PostMapping("/form/create")
    public ResponseEntity<AcemResponse<SubscaleProcessingResponse>> createForm(@Validated(CreateGroup.class)
                                                                               @RequestBody CreateSubscaleProcessingRequest body,
                                                                               Authentication authentication) {
        validateSubscaleProcessing(body);
        String userId = (String) authentication.getDetails();
        SubscaleProcessingResponse response = service.createForm(body, userId);

        String message = body.getFormSubmissionType() == Status.DRAFT
                ? "Subscale Processing form saved as draft successfully"
                : "Subscale Processing form submitted successfully";

        return AcemTemplate.created(response, message);
    }

    @PutMapping("/form/update")
    public ResponseEntity<AcemResponse<UpdateSubscaleProcessingResponse>> updateForm(@Validated(UpdateGroup.class)
                                                                                     @RequestBody CreateSubscaleProcessingRequest body,
                                                                                     Authentication authentication) {

        validateSubscaleProcessing(body);
        String userId = (String) authentication.getDetails();
        UpdateSubscaleProcessingResponse response = service.updateForm(body, userId);

        String message = body.getFormSubmissionType() == Status.DRAFT
                ? "Subscale Processing form created successfully"
                : "Subscale Processing form submitted successfully";

        return AcemTemplate.success(response, message);
    }

    @PreAuthorize("hasAnyAuthority('User', 'Approver', 'System Manager', 'Admin', 'Centre Head')")
    @PostMapping("/form/details")
    public ResponseEntity<AcemResponse<SubscaleProcessingDetailsResponse>> getFormDetails(
            @Valid @RequestBody SubscaleProcessingDetailsRequest request) {
        return AcemTemplate.success(service.getFormDetails(request.getFormId()),
                "Subscale Processing form details fetched successfully");
    }

    private void validateSubscaleProcessing(CreateSubscaleProcessingRequest request) {
        if (request == null || request.getSubDepartmentId() == null) return;
        Long subDeptId = request.getSubDepartmentId();
        dtoValidationService.validateDto(request, subDeptId);

    }

}