package in.co.bel.acempp.controller.sourcing;

import in.co.bel.acempp.dto.AcemResponse;
import in.co.bel.acempp.dto.general.StatsRequest;
import in.co.bel.acempp.dto.rawmaterialsourcing.*;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.exception.AcemTemplate;
import in.co.bel.acempp.service.sourcing.RawMaterialSourcingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user/raw-material-sourcing")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('User')")
public class RawMaterialSourcingController {

    private final RawMaterialSourcingService rmpService;

    private String getUserId(Authentication auth) {
        return (String) auth.getDetails(); // userId from JWT
    }

    @PostMapping("/form/create")
    public ResponseEntity<AcemResponse<RmpFormBasicResponse>> createForm(
            @Valid @RequestBody CreateRmpFormRequest request,
            Authentication authentication) {

        String message = request.getSubmissionType() == Status.DRAFT
                ? "Raw Material Sourcing details saved as draft successfully"
                : "Raw Material Sourcing details submitted for approval successfully";

        return AcemTemplate.created(
                rmpService.createForm(request, getUserId(authentication)),
                message
        );
    }

    @PutMapping("/form/update")
    public ResponseEntity<AcemResponse<UpdateLotResponse>> updateLot(
            @Valid @RequestBody UpdateLotRequest request,
            Authentication authentication) {

        String message = request.getSubmissionType() == Status.DRAFT
                ? "Raw Material Sourcing details saved as draft successfully"
                : "Raw Material Sourcing details submitted for approval successfully";

        return AcemTemplate.created(
                rmpService.updateLot(request, getUserId(authentication)),
                message
        );
    }
    @PreAuthorize("hasAnyAuthority('User', 'Approver')")
    @PostMapping("/form/details")
    public ResponseEntity<AcemResponse<LotDetailsResponse>> getLotDetails(
            @Valid @RequestBody LotDetailsRequest request) {

        return AcemTemplate.success(
                rmpService.getLotDetails(request),
                "Raw material lot details fetched successfully"
        );
    }

    @PreAuthorize("hasAnyAuthority('Admin', 'User', 'Approver')")
    @PostMapping("/form/lot-list")
    public ResponseEntity<AcemResponse<LotListResponse>> getLotList(
            @Valid @RequestBody LotListRequest request) {

        return AcemTemplate.success(
                rmpService.getLotList(request),
                "Raw material lot list fetched successfully"
        );
    }

    @PreAuthorize("hasAuthority('User')")
    @DeleteMapping("/form/delete")
    public ResponseEntity<AcemResponse<DeleteLotResponse>> deleteLot(
            @Valid @RequestBody DeleteLotRequest request,
            Authentication authentication) {
        String userId = (String) authentication.getDetails();
        DeleteLotResponse response = rmpService.softDeleteLot(request, userId);
        return AcemTemplate.success(response, "Raw material lot deleted successfully");
    }

    @PreAuthorize("hasAnyAuthority('User')")
    @PostMapping("/stats")
    public ResponseEntity<AcemResponse<LotStatsResponse>> getLotStats(@Valid @RequestBody StatsRequest request) {
        return AcemTemplate.success(rmpService.getLotStats(request.getSubDepartmentId()),
                "Raw material sourcing statistics fetched successfully"
        );
    }

}