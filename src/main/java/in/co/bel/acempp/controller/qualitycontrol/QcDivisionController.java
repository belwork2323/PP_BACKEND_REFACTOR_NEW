package in.co.bel.acempp.controller.qualitycontrol;
import in.co.bel.acempp.dto.AcemResponse;
import in.co.bel.acempp.dto.qcdivision.*;
import in.co.bel.acempp.exception.AcemTemplate;
import in.co.bel.acempp.service.qualitycontrol.QcDivisionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/v1/user/qc-division")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('User')")
public class QcDivisionController {

    private final QcDivisionService qcDivisionService;
    @PreAuthorize("hasAnyAuthority('User', 'Approver')")
    @GetMapping("/divisions")
    public ResponseEntity<AcemResponse<List<QcDivisionOptionDto>>> getDivisions() {
        return AcemTemplate.success(
                qcDivisionService.getDivisionOptions(),
                "QC divisions fetched successfully");
    }

    @PostMapping("/division-details")
    public ResponseEntity<AcemResponse<Map<String, Object>>> autoPopulateDivision(
            @Valid @RequestBody QcDivisionAutoPopulateRequest request) {
        return AcemTemplate.success(
                qcDivisionService.autoPopulateDivision(request),
                "QC division data populated successfully");
    }

    @PostMapping("/create")
    public ResponseEntity<AcemResponse<QcDivisionFormResponse>> createForm(
            @Valid @RequestBody CreateQcDivisionRequest request,
            Authentication authentication) {
        String userId = (String) authentication.getDetails();
        QcDivisionFormResponse response = qcDivisionService.createForm(request, userId);

        return AcemTemplate.created(response, "QC division form saved successfully");
    }
    @PutMapping("/update")
    public ResponseEntity<AcemResponse<QcDivisionFormResponse>> updateForm(
            @Valid @RequestBody UpdateQcDivisionRequest request,
            Authentication authentication) {
        String userId = (String) authentication.getDetails();
        QcDivisionFormResponse response = qcDivisionService.updateForm(request, userId);

        return AcemTemplate.success(response, "QC division form updated successfully");
    }
    @PreAuthorize("hasAnyAuthority('User', 'Approver', 'System Manager', 'Admin', 'Centre Head')")
    @PostMapping("/details")
    public ResponseEntity<AcemResponse<QcDivisionFormResponse>> getFormDetails(
            @Valid @RequestBody QcDivisionDetailsRequest request) {
        return AcemTemplate.success(
                qcDivisionService.getFormDetails(request.getFormId(), request.getSubDepartmentId()),
                "QC division form details fetched successfully");
    }
}
