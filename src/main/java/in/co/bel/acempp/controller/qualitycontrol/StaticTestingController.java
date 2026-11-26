package in.co.bel.acempp.controller.qualitycontrol;

import in.co.bel.acempp.dto.AcemResponse;
import in.co.bel.acempp.dto.statictestingfacility.*;
import in.co.bel.acempp.dto.statictestingfacility.bem.*;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.exception.AcemTemplate;
import in.co.bel.acempp.service.qualitycontrol.BemMotorService;
import in.co.bel.acempp.service.qualitycontrol.StaticTestingService;
import in.co.bel.acempp.validation.DtoValidationService;
import in.co.bel.acempp.validation.groups.CreateGroup;
import in.co.bel.acempp.validation.groups.DetailGroup;
import in.co.bel.acempp.validation.groups.ListGroup;
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
@RequestMapping("/api/v1/user/stf")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('User', 'Approver')")
public class StaticTestingController {

    private final StaticTestingService stfService;
    private final BemMotorService bemMotorService;
    private final DtoValidationService dtoValidationService;

    @PostMapping("/form/create")
    public ResponseEntity<AcemResponse<StfResponse>> createForm(
            @Validated(CreateGroup.class) @RequestBody CreateStfRequest request,
            Authentication authentication) {
        String userId = (String) authentication.getDetails();
        validateMotor(request);
        StfResponse response = stfService.createForm(request, userId);

        String message = request.getFormSubmissionType() == Status.DRAFT
                ? "Static testing form created successfully"
                : "Static testing form submitted successfully";

        return AcemTemplate.created(response, message);
    }

    @PutMapping("/form/update")
    public ResponseEntity<AcemResponse<StfResponse>> updateForm(
            @Validated(UpdateGroup.class) @RequestBody CreateStfRequest request,
            Authentication authentication) {
        validateMotor(request);
        String userId = (String) authentication.getDetails();
        StfResponse response = stfService.updateForm(request, userId);

        String message = request.getFormSubmissionType() == Status.DRAFT
                ? "Static testing form updated successfully"
                : "Static testing form submitted successfully";

        return AcemTemplate.success(response, message);
    }

    @PreAuthorize("hasAnyAuthority('User', 'Approver', 'System Manager', 'Admin', 'Centre Head')")
    @PostMapping("/form/details")
    public ResponseEntity<AcemResponse<StfResponse>> getFormDetails(
            @Validated(DetailGroup.class) @RequestBody CreateStfRequest request) {
        return AcemTemplate.success(
                stfService.getFormDetails(request.getFormId()),
                "STF details fetched successfully");
    }

    @PostMapping("/bem-motor/create")
    public ResponseEntity<AcemResponse<BemMotorActionResponseDto>> createBemMotor(
            @Validated(CreateGroup.class) @RequestBody BemMotorRequest request,
            Authentication authentication) {

        String userId = (String) authentication.getDetails();
        BemMotorActionResponseDto data = bemMotorService.createBemMotor(request, userId);

        return AcemTemplate.created(data, "BEM motor created successfully");
    }

    @PutMapping("/bem-motor/update")
    public ResponseEntity<AcemResponse<BemMotorActionResponseDto>> updateBemMotor(
            @Validated(UpdateGroup.class) @RequestBody BemMotorRequest request,
            Authentication authentication) {

        String userId = (String) authentication.getDetails();
        BemMotorActionResponseDto data = bemMotorService.updateBemMotor(request, userId);

        return AcemTemplate.success(data, "BEM motor updated successfully");
    }

    @PreAuthorize("hasAnyAuthority('User', 'Approver')")
    @PostMapping("/bem-motor/details")
    public ResponseEntity<AcemResponse<BemMotorDetailsResponseDto>> getBemMotorDetails(@Validated(DetailGroup.class) @RequestBody BemMotorRequest request) {
        String motorId = request.getMotorId();
        BemMotorDetailsResponseDto motor = bemMotorService.getBemMotorDetails(motorId);
        return AcemTemplate.success(motor, "BEM motor details retrieved successfully");

    }

    @PostMapping("/bem-motor/list")
    public ResponseEntity<AcemResponse<BemMotorListResponseDto>> getBemMotorList(
            @Validated(ListGroup.class)
            @RequestBody BemMotorRequest request) {

        BemMotorListResponseDto data = bemMotorService.getBemMotorList(request);
        return AcemTemplate.success(data, "BEM motor list retrieved successfully");

    }

    private void validateMotor(CreateStfRequest request) {
        List<StfMotorRequest> motors = request.getMotors();
        if (motors != null) {
            for (StfMotorRequest motor : motors) {
                if (motor == null) {
                    continue;
                }
                dtoValidationService.validateDto(motor, request.getSubDepartmentId());
            }
        }
    }

}
