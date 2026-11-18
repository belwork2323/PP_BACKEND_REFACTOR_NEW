package in.co.bel.acempp.service.sourcing;

import in.co.bel.acempp.dto.rawmaterialsourcing.*;
import in.co.bel.acempp.dto.user.operations.MaterialGrade;
import in.co.bel.acempp.entity.MaterialMaster;
import in.co.bel.acempp.entity.MaterialSpecification;
import in.co.bel.acempp.entity.sourcing.Lot;
import in.co.bel.acempp.entity.sourcing.Sourcing;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.enums.PreparationType;
import in.co.bel.acempp.enums.RawMaterialCategory;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.MaterialRepository;
import in.co.bel.acempp.repository.sourcing.LotRepository;
import in.co.bel.acempp.repository.sourcing.SourcingRepository;
import in.co.bel.acempp.service.masterdata.MaterialMasterDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RawMaterialSourcingDetailsMapper {

    private final LotRepository lotRepository;
    private final SourcingRepository sourcingRepository;
    private final MaterialRepository materialRepository;
    private final RawMaterialSourcingCertificateSupport certificateSupport;

    LotDetailsResponse toLotDetails(LotDetailsRequest request) {

        // 1. Find the lot
        Lot lot = lotRepository.findByLotId(request.getLotId())
                .orElseThrow(() -> new AcemException(ErrorCode.LOT_NOT_FOUND,
                        "Lot not found with ID: " + request.getLotId()));

        if (lot.getStatus() == Status.DELETED) {
            throw new AcemException(ErrorCode.RESOURCE_NOT_FOUND,
                    "Lot not found");
        }

        // 2. Fetch the material (for total specs count)
        MaterialMaster material = materialRepository
                .findFirstByMaterialCode(lot.getMaterialCode())
                .orElse(null);

        MaterialGrade gradeDto = null;

        if (lot.getGrade() != null) {
            gradeDto = MaterialGrade.builder()
                    .gradeId(lot.getGrade().getGradeId())
                    .gradeCode(lot.getGrade().getGradeCode())
                    .gradeName(lot.getGrade().getGradeName())
                    .build();
        }

        // Sourcing header is optional. Approved lots already carry subDepartmentId,
        // and QC revalidation must still list identification-sheet lots (including AP)
        // when the parent sourcing document is absent.
        Sourcing sourcing = lot.getSourcingId() == null || lot.getSourcingId().isBlank()
                ? null
                : sourcingRepository.findBySourcingId(lot.getSourcingId()).orElse(null);
        Long subDepartmentId = sourcing != null && sourcing.getSubDepartmentId() != null
                ? sourcing.getSubDepartmentId()
                : lot.getSubDepartmentId();

        List<Lot.LotSpec> lotSpecs = lot.getSpecifications() == null
                ? List.of()
                : lot.getSpecifications();

        // 4. Build specification responses (from embedded lot specs)
        List<SpecificationResponse> specResponses = lotSpecs.stream()
                .map(s -> SpecificationResponse.builder()
                        .specificationCode(s.getSpecificationCode())
                        .specificationName(s.getSpecificationName())
                        .referenceRange(ReferenceRange.builder()
                                .minValue(s.getMinValue())
                                .maxValue(s.getMaxValue())
                                .unitId(s.getUnitId())
                                .unit(s.getUnit())
                                .build())
                        .analysedResult(s.getAnalysedResult())
                        .isOutOfRange(s.isOutOfRange())
                        .acemQcResult(s.getAcemQcResult())
                        .status(s.getStatus())
                        .build())
                .collect(Collectors.toList());

        // 5. Build certificate responses
        List<CertificateResponse> certResponses =
                certificateSupport.buildCertificateResponses(lot, subDepartmentId);

        // 6. Progress insights
        List<MaterialSpecification> configuredSpecifications =
                material != null && material.getSpecifications() != null
                        ? material.getSpecifications()
                        : Collections.emptyList();
        int totalSpecs = configuredSpecifications.size();
        int filledSpecs = lotSpecs.size();

        int completionPct = totalSpecs > 0
                ? (filledSpecs * 100 / totalSpecs)
                : 0;

        // Fetch all lots belonging to this sourcing. Fall back to the current lot
        // when the sourcing id is missing so a single AP lot can still be shown.
        List<Lot> sourcingLots = lot.getSourcingId() == null || lot.getSourcingId().isBlank()
                ? List.of(lot)
                : lotRepository.findBySourcingId(lot.getSourcingId());
        if (sourcingLots.isEmpty()) {
            sourcingLots = List.of(lot);
        }

        // All lots are considered entered only if every lot is submitted
        boolean allLotsEntered = sourcingLots.stream()
                .allMatch(l -> l.getSubmissionType() == Status.SUBMIT);

        // Current lot is ready for approval if it has been submitted
        // and is currently waiting for approval
        boolean isReadyForApproval =
                lot.getSubmissionType() == Status.SUBMIT
                        && lot.getStatus() == Status.WAITING_FOR_APPROVAL;

        RmpProgressInsightsDto progressInsights = RmpProgressInsightsDto.builder()
                .materialsCount(1)
                .totalSpecs(totalSpecs)
                .filledSpecs(filledSpecs)
                .completionPercentage(completionPct)
                .allLotsEntered(allLotsEntered)
                .isReadyForApproval(isReadyForApproval)
                .build();

        // 7. Quality insights
        long passedCount = lotSpecs.stream()
                .filter(s -> "Passed".equals(s.getStatus())).count();
        long failedCount = lotSpecs.stream()
                .filter(s -> "Failed".equals(s.getStatus())).count();
        int totalTests = lotSpecs.size();
        double passRate = totalTests > 0 ? ((double) passedCount / totalTests) * 100 : 0;

        List<String> nonConformances = lotSpecs.stream()
                .filter(s -> s.isOutOfRange())
                .map(s -> s.getSpecificationName())
                .collect(Collectors.toList());

        RmpQualityInsightsDto qualityInsights = RmpQualityInsightsDto.builder()
                .totalTests(totalTests)
                .passed((int) passedCount)
                .failed((int) failedCount)
                .passRate(Math.round(passRate * 100.0) / 100.0)
                .nonConformances(nonConformances)
                .build();

        // 8. Workflow insights
        RmpWorkflowInsightsDto workflowInsights = RmpWorkflowInsightsDto.builder()
                .currentStatus(String.valueOf(lot.getStatus()))
                .rejectionReason(lot.getRejectionReason())
                .approvalPending(lot.getStatus() == Status.WAITING_FOR_APPROVAL)
                .reworkRequired(lot.getStatus() == Status.REJECTED)
                .resubmissionCount(0)
                .build();

        RawMaterialCategory rawMaterialType = lot.getRawMaterialType();
        PreparationType preparationType = lot.getPreparationType();
        if (rawMaterialType == null && material != null) {
            rawMaterialType = MaterialMasterDataService.resolveRawMaterialCategory(material.getRawMaterialType());
            preparationType = material.getPreparationType();
        }

        return LotDetailsResponse.builder()
                .lotId(lot.getLotId())
                .submissionType(lot.getSubmissionType())
                .subDepartmentId(subDepartmentId)
                .supplyOrderNo(lot.getSupplyOrderNo())
                .receiptDate(lot.getReceiptDate())
                .manufacturerName(lot.getManufacturerName())
                .materialCode(lot.getMaterialCode())
                .grade(gradeDto)
                .rawMaterialType(rawMaterialType)
                .preparationType(preparationType)
                .adductPreparation(lot.getAdductPreparation())
                .htpbBlendingPreparation(lot.getHtpbBlendingPreparation())
                .specifications(specResponses)
                .certificates(certResponses)
                .progressInsights(progressInsights)
                .qualityInsights(qualityInsights)
                .workflowInsights(workflowInsights)
                .status(lot.getStatus())
                .rejectionReason(lot.getRejectionReason())
                .build();
    }
}