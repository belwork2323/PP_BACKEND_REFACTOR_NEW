package in.co.bel.acempp.controller.sourcing;

import in.co.bel.acempp.dto.AcemResponse;
import in.co.bel.acempp.dto.general.StatsRequest;
import in.co.bel.acempp.dto.rocketmotorcasing.*;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.exception.AcemTemplate;
import in.co.bel.acempp.service.sourcing.RocketMotorCasingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user/rocket-motor-casing")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('User')")
public class RocketMotorCasingController {
    private final RocketMotorCasingService rmcService;

    private String getUserId(Authentication auth) {
        return (String) auth.getDetails();
    }


    @PostMapping("/form/create")
    public ResponseEntity<AcemResponse<RmcFormResponse>> createForm(@Valid @RequestBody CreateRmcFormRequest request,
            Authentication authentication) {

        String message = request.getFormSubmissionType() == Status.DRAFT
                ? "Rocket Motor Casing details saved as draft successfully"
                : "Rocket Motor Casing details submitted for approval successfully";

        return AcemTemplate.created(rmcService.createForm(request, getUserId(authentication)),
                message);
    }

    @PreAuthorize("hasAnyAuthority('User', 'Approver', 'Admin', 'Centre Head', 'System Manager')")
    @PostMapping("/form/details")
    public ResponseEntity<AcemResponse<RmcDetailsResponse>> getFormDetails(
            @Valid @RequestBody RmcDetailsRequest request) {
        return AcemTemplate.success(
                rmcService.getFormDetails(request),
                "Casing details fetched successfully");
    }

    @PutMapping("/form/update")
    public ResponseEntity<AcemResponse<RmcFormResponse>> updateForm(@Valid @RequestBody UpdateRmcFormRequest request,
                                                                    Authentication authentication) {

        String message = request.getFormSubmissionType() == Status.DRAFT
                        ? "Rocket Motor Casing details saved as draft successfully"
                        : "Rocket Motor Casing details updated for approval successfully";

        return AcemTemplate.created(
                rmcService.updateForm(request, getUserId(authentication)),
                message
        );
    }


    @DeleteMapping("/form/delete")
    public ResponseEntity<AcemResponse<RmcFormResponse>> deleteForm(@Valid @RequestBody RmcDetailsRequest request,
            Authentication authentication) {

        return AcemTemplate.success(rmcService.deleteForm(request, getUserId(authentication)),
                "Rocket motor casing record deleted successfully"
        );
    }


    @PreAuthorize("hasAuthority('Approver')")
    @PostMapping("/form/approved-motors-list")
    public ResponseEntity<AcemResponse<AvailableMotorListResponse>> getAvailableMotors(@Valid @RequestBody
            AvailableMotorRequest request) {
        return AcemTemplate.success(rmcService.getAvailableMotors(request),
                "Available motor casings fetched successfully"
        );
    }

    @PreAuthorize("hasAnyAuthority('User')")
    @PostMapping("/stats")
    public ResponseEntity<AcemResponse<MotorsStatsResponse>> getMotorStats(@Valid @RequestBody StatsRequest request) {
        return AcemTemplate.success(rmcService.getMotorStats(request.getSubDepartmentId()),
                "Rocket motor casing statistics fetched successfully"
        );
    }

}
