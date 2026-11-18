package in.co.bel.acempp.service.sourcing;

import in.co.bel.acempp.dto.general.PaginationDto;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.general.StatusCountsDto;
import in.co.bel.acempp.dto.rawmaterialsourcing.LotListRequest;
import in.co.bel.acempp.dto.rawmaterialsourcing.LotListResponse;
import in.co.bel.acempp.dto.rawmaterialsourcing.LotStatsResponse;
import in.co.bel.acempp.dto.rawmaterialsourcing.LotSummary;
import in.co.bel.acempp.dto.user.operations.MaterialGrade;
import in.co.bel.acempp.entity.sourcing.Lot;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.repository.sourcing.LotRepository;
import in.co.bel.acempp.util.DateTimeConversionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RawMaterialSourcingQuerySupport {

    private final LotRepository lotRepository;
    private final MongoTemplate mongoTemplate;

    LotListResponse getLotList(LotListRequest request) {

        Long subDeptId = request.getSubDepartmentId();

        // 1. Status counts – direct repository calls (identical to old JPA logic)
        StatusCountsDto statusCounts = StatusCountsDto.builder()
                .initiated(lotRepository.countBySubDepartmentIdAndStatus(subDeptId, Status.TO_BE_INITIATED))
                .inProgress(lotRepository.countBySubDepartmentIdAndStatus(subDeptId, Status.IN_PROGRESS))
                .waitingForApproval(lotRepository.countBySubDepartmentIdAndStatus(subDeptId, Status.WAITING_FOR_APPROVAL))
                .approved(lotRepository.countBySubDepartmentIdAndStatus(subDeptId, Status.APPROVED))
                .rejected(lotRepository.countBySubDepartmentIdAndStatus(subDeptId, Status.REJECTED))
                .build();

        // 2. Dynamic filter using MongoTemplate (unchanged from previous answer)
        List<Criteria> criteriaList = new ArrayList<>();
        criteriaList.add(Criteria.where("subDepartmentId").is(subDeptId));

        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            criteriaList.add(Criteria.where("status").in(request.getStatus()));
        }
        if (request.getMaterialCode() != null && !request.getMaterialCode().isEmpty()) {
            criteriaList.add(Criteria.where("materialCode").in(request.getMaterialCode()));
        }
        if (request.getManufacturerName() != null && !request.getManufacturerName().isBlank()) {
            // Case-insensitive contains (matches acem / ACEM / Acem / "acem labs 1")
            String manufacturerPattern =
                    ".*" + java.util.regex.Pattern.quote(request.getManufacturerName().trim()) + ".*";
            criteriaList.add(Criteria.where("manufacturerName").regex(manufacturerPattern, "i"));
        }
        if (request.getSearch() != null && !request.getSearch().isBlank()) {
            // Case-insensitive contains across visible list columns (User + Approver share this API).
            String searchPattern =
                    ".*" + java.util.regex.Pattern.quote(request.getSearch().trim()) + ".*";
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("lotId").regex(searchPattern, "i"),
                    Criteria.where("sourcingId").regex(searchPattern, "i"),
                    Criteria.where("materialCode").regex(searchPattern, "i"),
                    Criteria.where("materialName").regex(searchPattern, "i"),
                    Criteria.where("supplyOrderNo").regex(searchPattern, "i"),
                    Criteria.where("manufacturerName").regex(searchPattern, "i"),
                    Criteria.where("status").regex(searchPattern, "i"),
                    Criteria.where("createdByUserId").regex(searchPattern, "i"),
                    Criteria.where("createdByUsername").regex(searchPattern, "i"),
                    Criteria.where("grade.gradeCode").regex(searchPattern, "i"),
                    Criteria.where("grade.gradeName").regex(searchPattern, "i")
            ));
        }
        if (request.getFromDate() != null && !request.getFromDate().isBlank()) {
            LocalDate from = DateTimeConversionUtil.parseLocalDate(request.getFromDate());
            criteriaList.add(Criteria.where("receiptDate").gte(from));
        }
        if (request.getToDate() != null && !request.getToDate().isBlank()) {
            LocalDate to = DateTimeConversionUtil.parseLocalDate(request.getToDate());
            criteriaList.add(Criteria.where("receiptDate").lte(to));
        }
        // Exclude soft‑deleted lots
        criteriaList.add(
                Criteria.where("status").ne(Status.DELETED.name())
        );

        Criteria finalCriteria = new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
        Query query = Query.query(finalCriteria).with(Sort.by(Sort.Direction.DESC, "createdAt"));

        Pageable pageable = PageRequest.of(request.getPage() - 1, request.getLimit());
        long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Lot.class);
        query.with(pageable);
        List<Lot> lots = mongoTemplate.find(query, Lot.class);
        Page<Lot> lotPage = PageableExecutionUtils.getPage(lots, pageable, () -> total);

        // 3. Map to DTOs
        List<LotSummary> lotSummaries = lotPage.getContent().stream()
                .map(lot -> {
                    MaterialGrade grade = lot.getGrade();

                    return LotSummary.builder()
                            .lotId(lot.getLotId())
                            .sourcingId(lot.getSourcingId())
                            .materialCode(lot.getMaterialCode())
                            .grade(grade)
                            .materialName(lot.getMaterialName())
                            .supplyOrderNo(lot.getSupplyOrderNo())
                            .receiptDate(lot.getReceiptDate())
                            .manufacturerName(lot.getManufacturerName())
                            .status(lot.getStatus())
                            .createdBy(PersonInfo.builder()
                                    .id(lot.getCreatedByUserId())
                                    .fullName(lot.getCreatedByUsername())
                                    .build())
                            .createdOn(lot.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());

        return LotListResponse.builder()
                .statusCounts(statusCounts)
                .lots(lotSummaries)
                .pagination(PaginationDto.builder()
                        .page(request.getPage())
                        .pageSize(request.getLimit())
                        .totalRecords(lotPage.getTotalElements())
                        .totalPages(lotPage.getTotalPages())
                        .build())
                .build();
    }

    LotStatsResponse getLotStats(Long subDepartmentId) {
        long createdLots = lotRepository.countBySubDepartmentIdAndStatusNot(subDepartmentId, Status.DELETED);
        long pendingLots = lotRepository.countBySubDepartmentIdAndStatusIn(subDepartmentId, List.of(
                Status.TO_BE_INITIATED, Status.IN_PROGRESS));
        long waitingForApprovalLots = lotRepository.countBySubDepartmentIdAndStatus(subDepartmentId,
                Status.WAITING_FOR_APPROVAL);
        long approvedLots = lotRepository.countBySubDepartmentIdAndStatus(subDepartmentId, Status.APPROVED);
        long rejectedLots = lotRepository.countBySubDepartmentIdAndStatus(subDepartmentId, Status.REJECTED);
        return LotStatsResponse.builder()
                .createdLots(createdLots)
                .pendingLots(pendingLots)
                .waitingForApprovalLots(waitingForApprovalLots)
                .approvedLots(approvedLots)
                .rejectedLots(rejectedLots)
                .build();
    }
}