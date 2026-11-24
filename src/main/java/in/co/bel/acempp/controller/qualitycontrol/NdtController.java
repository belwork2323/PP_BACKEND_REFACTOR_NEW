package in.co.bel.acempp.controller.qualitycontrol;

import in.co.bel.acempp.dto.AcemResponse;
import in.co.bel.acempp.dto.ndt.*;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.exception.AcemTemplate;
import in.co.bel.acempp.service.qualitycontrol.NdtService;
import in.co.bel.acempp.validation.DtoValidationService;
import in.co.bel.acempp.validation.groups.CreateGroup;
import in.co.bel.acempp.validation.groups.DetailGroup;
import in.co.bel.acempp.validation.groups.DraftGroup;
import in.co.bel.acempp.validation.groups.UpdateGroup;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/user/ndt")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('User')")

public class NdtController {

    private final NdtService ndtService;
    private final DtoValidationService dtoValidationService;

    @PostMapping("/form/create")
    public ResponseEntity<AcemResponse<NdtResponse>> createForm(
            @Validated(CreateGroup.class)
            @RequestBody CreateNdtRequest request,
            Authentication authentication) {
        Status formSubmissionType = request.getFormSubmissionType();
        validateMotor(request);
        String userId = (String) authentication.getDetails();
        NdtResponse response = ndtService.createForm(request, userId);

        String message = formSubmissionType == Status.DRAFT
                ? "NDT form saved as draft successfully"
                : "NDT form submitted successfully";

        return AcemTemplate.created(response, message);
    }

    @PutMapping("/form/update")
    public ResponseEntity<AcemResponse<NdtResponse>> updateForm(
            @Validated(UpdateGroup.class) @RequestBody CreateNdtRequest request,
            Authentication authentication) {
        String userId = (String) authentication.getDetails();
        validateMotor(request);
        NdtResponse response = ndtService.updateForm(request, userId);

        String message = request.getFormSubmissionType() == Status.DRAFT
                ? "NDT form updated successfully"
                : "NDT form submitted successfully";

        return AcemTemplate.success(response, message);
    }

    @PreAuthorize("hasAnyAuthority('User', 'Approver', 'System Manager', 'Admin', 'Centre Head')")
    @PostMapping("/form/details")
    public ResponseEntity<AcemResponse<NdtResponse>> getFormDetails(
            @Validated(DetailGroup.class) @RequestBody CreateNdtRequest request) {
        return AcemTemplate.success(
                ndtService.getFormDetails(request.getFormId()),
                "NDT form details fetched successfully");
    }

    private void validateMotor(CreateNdtRequest ndtRequest) {
        List<NdtMotorDto> motors = ndtRequest.getMotors();
        if (motors != null) {
            for (NdtMotorDto motor : motors) {
                if (motor == null) {
                    continue;
                }
                dtoValidationService.validateDto(motor, ndtRequest.getSubDepartmentId());
            }
        }
    }
}