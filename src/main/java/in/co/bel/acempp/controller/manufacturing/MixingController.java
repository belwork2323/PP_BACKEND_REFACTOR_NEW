package in.co.bel.acempp.controller.manufacturing;

import in.co.bel.acempp.dto.AcemResponse;
import in.co.bel.acempp.dto.mixing.MixingStageDto;
import in.co.bel.acempp.dto.mixing.mixingcycle.MixingDetailsDto;
import in.co.bel.acempp.dto.mixing.mixingform.MixingFormRequest;
import in.co.bel.acempp.dto.mixing.mixingform.MixingFormResponse;
import in.co.bel.acempp.dto.mixing.mixingform.PremixDto;
import in.co.bel.acempp.enums.StageType;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.exception.AcemTemplate;
import in.co.bel.acempp.exception.AcemValidationException;
import in.co.bel.acempp.service.manufacturing.MixingService;
import in.co.bel.acempp.validation.DtoValidationService;
import in.co.bel.acempp.validation.groups.CreateGroup;
import in.co.bel.acempp.validation.groups.PremixStageSubmitGroup;
import in.co.bel.acempp.validation.groups.SubmitGroup;
import in.co.bel.acempp.validation.groups.UpdateGroup;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/v1/user/mixing")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('User')")
public class MixingController {

    private final MixingService mixingService;
    private final DtoValidationService dtoValidationService;

    @PostMapping("/form/create")
    public ResponseEntity<AcemResponse<MixingFormResponse>> createForm(
            @Validated(CreateGroup.class) @RequestBody MixingFormRequest request,
            Authentication authentication) {
        validatePremixes(request.getMixingDetails());
        String userId = (String) authentication.getDetails();
        String message = request.getFormSubmissionType() == Status.DRAFT
                ? "Mixing form saved as draft successfully"
                : "Mixing form submitted successfully";
        return AcemTemplate.created(mixingService.createForm(request, userId), message);
    }

    @PutMapping("/form/update")
    public ResponseEntity<AcemResponse<MixingFormResponse>> updateForm(
            @Validated(UpdateGroup.class) @RequestBody MixingFormRequest request,
            Authentication authentication) {
        validatePremixes(request.getMixingDetails());
        String userId = (String) authentication.getDetails();
        return AcemTemplate.success(mixingService.updateForm(request, userId),
                "Mixing form updated successfully");
    }

    @PreAuthorize("hasAnyAuthority('User', 'Approver', 'System Manager', 'Admin', 'Centre Head')")
    @PostMapping("/form/details")
    public ResponseEntity<AcemResponse<MixingFormResponse>> getFormDetails(
            @Valid @RequestBody MixingFormRequest request) {
        return AcemTemplate.success(mixingService.getFormDetails(request),
                "Mixing form details fetched successfully");
    }

    private void validatePremixes(MixingDetailsDto details) {
        if (details == null || details.getStages() == null) return;

        for (int stageIndex = 0; stageIndex < details.getStages().size(); stageIndex++) {
            MixingStageDto stage = details.getStages().get(stageIndex);
            if (stage == null || stage.getPremixes() == null) continue;

            for (int premixIndex = 0; premixIndex < stage.getPremixes().size(); premixIndex++) {
                PremixDto premix = stage.getPremixes().get(premixIndex);
                if (premix == null) continue;
                dtoValidationService.validateDto(premix, premix.getPremixSubmissionType(), SubmitGroup.class);

            }
        }
    }
}
