package in.co.bel.acempp.controller.manufacturing;

import in.co.bel.acempp.dto.AcemResponse;
import in.co.bel.acempp.dto.postcureoperations.PostCureIngredientsRequest;
import in.co.bel.acempp.dto.postcureoperations.PostCureIngredientsResponse;
import in.co.bel.acempp.dto.postcureoperations.PostCureRequest;
import in.co.bel.acempp.dto.postcureoperations.PostCureResponse;
import in.co.bel.acempp.exception.AcemTemplate;
import in.co.bel.acempp.service.manufacturing.PostCureOptService;
import in.co.bel.acempp.validation.custom.ValidatePostCureMotor;
import in.co.bel.acempp.validation.groups.CreateGroup;
import in.co.bel.acempp.validation.groups.DetailGroup;
import in.co.bel.acempp.validation.groups.UpdateGroup;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user/post-cure")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('User')")
public class PostCureController {

    private final PostCureOptService postCureService;
    private final ValidatePostCureMotor validatePostCureMotor;

    @PostMapping("/form/create")
    public ResponseEntity<AcemResponse<PostCureResponse>> createForm(
            @Validated(CreateGroup.class) @RequestBody PostCureRequest request,
            Authentication authentication) {
        validatePostCureMotor.validateMotors(request);
        String userId = (String) authentication.getDetails();
        PostCureResponse response = postCureService.createForm(request, userId);
        return AcemTemplate.created(response, "Post cure form submitted successfully");
    }

    @PutMapping("/form/update")
    public ResponseEntity<AcemResponse<PostCureResponse>> updateForm(
            @Validated(UpdateGroup.class) @RequestBody PostCureRequest request,
            Authentication authentication) {
        validatePostCureMotor.validateMotors(request);
        String userId = (String) authentication.getDetails();
        PostCureResponse response = postCureService.updateForm(request, userId);
        return AcemTemplate.success(response, "Post cure form updated successfully");
    }

    @PreAuthorize("hasAnyAuthority('User', 'Approver', 'System Manager', 'Admin', 'Centre Head')")
    @PostMapping("/form/details")
    public ResponseEntity<AcemResponse<PostCureResponse>> getFormDetails(
            @Validated(DetailGroup.class) @RequestBody PostCureRequest request) {
        return AcemTemplate.success(
                postCureService.getFormDetails(request.getFormId()),
                "Post cure form fetched successfully");
    }

    @PostMapping("/ingredients")
    public ResponseEntity<AcemResponse<PostCureIngredientsResponse>> getIngredients(
            @Valid @RequestBody PostCureIngredientsRequest request) {
        return AcemTemplate.success(
                postCureService.getIngredients(request.getRecipeType()),
                "Post cure ingredients fetched successfully");
    }

}