package in.co.bel.acempp.controller;

import in.co.bel.acempp.audit.Audit;
import in.co.bel.acempp.dto.AcemResponse;
import in.co.bel.acempp.dto.admin.motor.*;
import in.co.bel.acempp.dto.auth.resetpswd.ChangeResetStatusRequest;
import in.co.bel.acempp.dto.auth.resetpswd.ChangeResetStatusResponse;
import in.co.bel.acempp.dto.auth.resetpswd.PasswordResetListRequest;
import in.co.bel.acempp.dto.auth.resetpswd.PasswordResetListResponse;
import in.co.bel.acempp.dto.batch.*;
import in.co.bel.acempp.dto.dashboard.*;
import in.co.bel.acempp.dto.project.*;
import in.co.bel.acempp.dto.user.*;
import in.co.bel.acempp.enums.AuditAction;
import in.co.bel.acempp.exception.AcemTemplate;
import in.co.bel.acempp.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/blockchain")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('Admin', 'Centre Head')")
public class BlockchainEventsController {

    private final BlockchainEventsService blockchainEventsService;
    private final ApprovalBlockchainAssetService approvalBlockchainAssetService;

    @PostMapping("/events")
    public ResponseEntity<AcemResponse<ActiveBatchesResponse>> getBlockchainTransactionList(
            @Valid @RequestBody BlockchainEventsRequest request) {
        return AcemTemplate.success(
                blockchainEventsService.getBlockchainEvents(request),
                "Blockchain transactions fetched successfully");
    }

    @PostMapping("/events-by-batchId")
    public ResponseEntity<AcemResponse<BlockchainEventsResponse>> getBlockchainTransactionByBatchId(
            @Valid @RequestBody BatchDetailsRequest request) {
        return AcemTemplate.success(
                blockchainEventsService.getBlockchainEventsByBatchId(request),
                "Blockchain transactions fetched successfully");
    }

    @PostMapping("/fetch")
    public ResponseEntity<AcemResponse<Object>> fetchBatchDetails(
            @Valid @RequestBody BatchDetailsRequest request
    ) {
        Object response = approvalBlockchainAssetService.fetchBlockchainAsset(request);
        return AcemTemplate.success(
                response,
                "Batch details fetched successfully"
        );
    }


}
