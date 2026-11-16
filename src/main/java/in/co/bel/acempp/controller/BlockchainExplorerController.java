package in.co.bel.acempp.controller;

import in.co.bel.acempp.dto.AcemResponse;
import in.co.bel.acempp.dto.batch.BatchDetailsRequest;
import in.co.bel.acempp.dto.blockchain.ExplorerRequest;
import in.co.bel.acempp.exception.AcemTemplate;
import in.co.bel.acempp.service.ApprovalBlockchainAssetService;
import in.co.bel.acempp.service.BlockchainExplorerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/blockchain")          // ACEM style base path
@RequiredArgsConstructor
public class BlockchainExplorerController {

    private final BlockchainExplorerService explorerService;
    private final ApprovalBlockchainAssetService approvalBlockchainAssetService;

    @PreAuthorize("hasAnyAuthority('Admin')")
    @GetMapping("/channels")
    public ResponseEntity<AcemResponse<Object>> getChannels() {
        Map response = (Map) explorerService.get("/api/channels/info");
        return AcemTemplate.success(
                response.get("channels"),
                "Channel info fetched successfully"
        );
    }

    @PreAuthorize("hasAnyAuthority('Admin')")
    @PostMapping("/stats")
    public ResponseEntity<AcemResponse<Object>> getStats(
            @Valid @RequestBody ExplorerRequest.GenesisHashRequest request
    ) {
        Object response = explorerService.get("/api/status/" + request.getGenesisHash());
        return AcemTemplate.success(response, "Blockchain stats fetched successfully");
    }

    @PreAuthorize("hasAnyAuthority('Admin')")
    @PostMapping("/peers")
    public ResponseEntity<AcemResponse<Object>> getPeers(
            @Valid @RequestBody ExplorerRequest.GenesisHashRequest request
    ) {
        Map response = (Map) explorerService.get("/api/peersStatus/" + request.getGenesisHash());
        return AcemTemplate.success(
                response.get("peers"),
                "Peers fetched successfully"
        );
    }

    @PreAuthorize("hasAnyAuthority('Admin')")
    @PostMapping("/tx-by-org")
    public ResponseEntity<AcemResponse<Object>> getTxByOrg(
            @Valid @RequestBody ExplorerRequest.GenesisHashRequest request
    ) {
        Map response = (Map) explorerService.get("/api/txByOrg/" + request.getGenesisHash());
        return AcemTemplate.success(
                response.get("rows"),
                "Transactions by organization fetched successfully"
        );
    }

    @PreAuthorize("hasAnyAuthority('Admin')")
    @PostMapping("/chart")
    public ResponseEntity<AcemResponse<Object>> getChartData(
            @Valid @RequestBody ExplorerRequest.ChartRequest request
    ) {
        String endpoint;

        if ("BLOCKS".equalsIgnoreCase(request.getType())) {
            endpoint = "HOUR".equalsIgnoreCase(request.getInterval())
                    ? "/api/blocksByHour/" + request.getHash() + "/1"
                    : "/api/blocksByMinute/" + request.getHash() + "/1";
        } else {
            endpoint = "HOUR".equalsIgnoreCase(request.getInterval())
                    ? "/api/txByHour/" + request.getHash() + "/1"
                    : "/api/txByMinute/" + request.getHash() + "/1";
        }

        Map response = (Map) explorerService.get(endpoint);
        return AcemTemplate.success(
                response.get("rows"),
                "Chart data fetched successfully"
        );
    }



}