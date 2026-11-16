package in.co.bel.acempp.controller.manufacturing;

import in.co.bel.acempp.dto.AcemResponse;
import in.co.bel.acempp.dto.castingcuring.CastingCuringFormResponse;
import in.co.bel.acempp.dto.castingcuring.CastingCuringMotor;
import in.co.bel.acempp.dto.castingcuring.CastingCuringRequest;
import in.co.bel.acempp.dto.castingcuring.CuringCyclesResponse;
import in.co.bel.acempp.dto.common.MotoStageRequest;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.exception.AcemTemplate;
import in.co.bel.acempp.service.manufacturing.CastingCuringService;
import in.co.bel.acempp.validation.DtoValidationService;
import in.co.bel.acempp.validation.groups.CreateGroup;
import in.co.bel.acempp.validation.groups.DetailGroup;
import in.co.bel.acempp.validation.groups.UpdateGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user/casting-curing")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('User')")
public class CastingCuringController {

    private final CastingCuringService ccService;
    private final DtoValidationService dtoValidationService;

    @PostMapping("/curing-cycles")
    public ResponseEntity<AcemResponse<CuringCyclesResponse>> getCuringCycles(
            @RequestBody MotoStageRequest request) {
        CuringCyclesResponse data = ccService.getCuringCycles(request);
        return AcemTemplate.success(data, "Curing cycle details fetched successfully.");
    }

    @PostMapping("/form/create")
    public ResponseEntity<AcemResponse<CastingCuringFormResponse>> createForm(
            @Validated(CreateGroup.class) @RequestBody CastingCuringRequest request,
            Authentication authentication) {
//        validateMotors(request.getMotors());
        String userId = (String) authentication.getDetails();
        CastingCuringFormResponse response = ccService.createForm(request, userId);
        return AcemTemplate.created(response, "Casting and curing form submitted successfully");
    }

    @PutMapping("/form/update")
    public ResponseEntity<AcemResponse<CastingCuringFormResponse>> updateForm(
            @Validated(UpdateGroup.class) @RequestBody CastingCuringRequest request,
            Authentication authentication) {
//        validateMotors(request.getMotors());
        String userId = (String) authentication.getDetails();
        CastingCuringFormResponse response = ccService.updateForm(request, userId);
        return AcemTemplate.success(response,
                request.getFormSubmissionType() == Status.SUBMIT
                        ? "Casting and Curing form submitted successfully"
                        : "Casting and Curing form updated successfully");
    }

    @PreAuthorize("hasAnyAuthority('User', 'Approver', 'System Manager', 'Admin', 'Centre Head')")
    @PostMapping("/form/details")
    public ResponseEntity<AcemResponse<CastingCuringFormResponse>> getFormDetails(
            @Validated(DetailGroup.class) @RequestBody CastingCuringRequest request) {
        CastingCuringFormResponse data = ccService.getFormDetails(request.getFormId());
        return AcemTemplate.success(data, "Casting and Curing form details fetched successfully");
    }

//    private void validateMotors(List<CastingCuringMotor> motors) {
//        if (motors == null) return;
//
//        for (CastingCuringMotor motor : motors) {
//            if (motor == null) continue;
//            dtoValidationService.validateDto(motor, motor.getMotorSubmissionType());
//        }
//    }
}
