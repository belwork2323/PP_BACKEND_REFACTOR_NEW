package in.co.bel.acempp.controller.manufacturing;
import in.co.bel.acempp.dto.AcemResponse;
import in.co.bel.acempp.dto.subscale.*;
import in.co.bel.acempp.dto.subscale.subscalecreation.CreateSubscaleProcessingRequest;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.exception.AcemTemplate;
import in.co.bel.acempp.service.manufacturing.SubscaleProcessingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user/subscale")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('User')")

public class SubscaleProcessingController {

    private final SubscaleProcessingService service;

    @PostMapping("/form/create")
    public ResponseEntity<AcemResponse<SubscaleProcessingResponse>> createForm(
            @RequestBody CreateSubscaleProcessingRequest body,
            Authentication authentication) {

       String userId = (String) authentication.getDetails();
        SubscaleProcessingResponse response = service.createForm(body, userId);

        String message = body.getFormSubmissionType() == Status.DRAFT
                ? "Subscale Processing form saved as draft successfully"
                : "Subscale Processing form submitted successfully";

        return AcemTemplate.created(response, message);
    }
    @PutMapping("/form/update")
    public ResponseEntity<AcemResponse<UpdateSubscaleProcessingResponse>> updateForm(
            @RequestBody CreateSubscaleProcessingRequest body,
            Authentication authentication) {


        String userId = (String) authentication.getDetails();
        UpdateSubscaleProcessingResponse response = service.updateForm(body, userId);

        String message = body.getFormSubmissionType() == Status.DRAFT
                ? "Subscale Processing form created successfully"
                : "Subscale Processing form submitted successfully";

        return AcemTemplate.success(response, message);
    }

//    @PreAuthorize("hasAnyAuthority('User', 'Approver')")
//    @PostMapping("/form/details")
//    public ResponseEntity<AcemResponse<Map<String, Object>>> getFormDetails(
//            @Valid @RequestBody SubscaleProcessingDetailsRequest request) {
//        Map<String, Object> data = service.getFormDetailsAsMap(request.getFormId());
//        return AcemTemplate.success(data, "Subscale Processing details fetched successfully");
//    }
    @PreAuthorize("hasAnyAuthority('User', 'Approver', 'System Manager', 'Admin', 'Centre Head')")
    @PostMapping("/form/details")
    public ResponseEntity<AcemResponse<SubscaleProcessingDetailsResponse>> getFormDetails(
            @Valid @RequestBody SubscaleProcessingDetailsRequest request) {
        return AcemTemplate.success(service.getFormDetails(request.getFormId()),
                "Subscale Processing form details fetched successfully");
    }
}