package in.co.bel.acempp.service;

import in.co.bel.acempp.dto.batch.BatchDetailsRequest;
import in.co.bel.acempp.dto.dashboard.*;
import in.co.bel.acempp.dto.general.PaginationDto;
import in.co.bel.acempp.entity.batch.Batch;
import in.co.bel.acempp.entity.batch.BlockchainEvent;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.BatchRepository;
import in.co.bel.acempp.repository.BlockchainEventRepository;
import in.co.bel.acempp.util.DateTimeConversionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlockchainEventsService {
    private final BatchRepository batchRepo;
    private final BlockchainEventRepository blockchainEventRepo;

    public ActiveBatchesResponse getBlockchainEvents(BlockchainEventsRequest request) {
        LocalDateTime start = null;
        LocalDateTime end = null;


        start = parseNullableDate(request.getStartDate());
        LocalDateTime parsedEnd = parseNullableDate(request.getEndDate());
        end = (parsedEnd != null) ? parsedEnd.toLocalDate().atTime(LocalTime.MAX) : null;

        if ((start == null) != (end == null)) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "Both startDate and endDate must be provided."
            );
        }

        if (start != null && end != null && end.isBefore(start)) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "End date cannot be before start date."
            );
        }


        List<String> distinctBatchIds = blockchainEventRepo.findDistinctBatchIdsWithFilters(
                request.getSearch(),
                request.getSubDepartment(),
                start,
                end
        );

        if (distinctBatchIds == null || distinctBatchIds.isEmpty()) {
            return ActiveBatchesResponse.builder()
                    .batches(Collections.emptyList())
                    .pagination(PaginationDto.builder()
                            .page(request.getPage())
                            .pageSize(request.getPageSize())
                            .totalRecords(0L)
                            .totalPages(0)
                            .build())
                    .build();
        }

        Pageable pageable = PageRequest.of(
                Math.max(0, request.getPage() - 1),
                request.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Batch> page = batchRepo.findBatchesByBlockchainIdsAndStatus(
                distinctBatchIds,
                request.getSearch(),
                request.getStatus(), // "IN_PROGRESS" or "COMPLETED"
                start,
                end,
                pageable
        );

        List<ActiveBatchItemDto> dtos = page.getContent().stream()
                .map(batch -> {
                    String motorId = (batch.getMotorIds() != null && !batch.getMotorIds().isEmpty())
                            ? batch.getMotorIds().getFirst()
                            : null;

                    String createdDate = batch.getCreatedAt() != null
                            ? batch.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                            : null;

                    return ActiveBatchItemDto.builder()
                            .batchId(batch.getBatchId())
                            .batchType(batch.getBatchType())
                            .motorId(motorId)
                            .projectName(batch.getProjectName())
                            .systemManager(batch.getSystemManager())
                            .date(createdDate)
                            .build();
                })
                .collect(Collectors.toList());

        return ActiveBatchesResponse.builder()
                .batches(dtos)
                .pagination(PaginationDto.builder()
                        .page(request.getPage())
                        .pageSize(request.getPageSize())
                        .totalRecords(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .build())
                .build();
    }

    public BlockchainEventsResponse getBlockchainEventsByBatchId(BatchDetailsRequest request) {
        String batchId = request.getBatchId();

        if (batchId == null || batchId.isBlank()) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "Batch ID must be provided."
            );
        }
        List<BlockchainEvent> events = blockchainEventRepo.findByBatchIdOrderByTimestampDesc(batchId.trim());

        List<BlockchainEventDto> dtos = events.stream()
//                .filter(e -> e.getDepartment() != null && !e.getDepartment().isBlank() &&
//                        e.getSubDepartment() != null && !e.getSubDepartment().isBlank())
                .map(e -> BlockchainEventDto.builder()
                        .transactionId(e.getTransactionId())
                        .batchId(e.getBatchId())
                        .eventType(e.getEventType())
                        .eventStatusMessage(e.getEventStatusMessage())
                        .department(e.getDepartment())
                        .subDepartment(e.getSubDepartment())
                        .assetId(e.getAssetId())
                        .division(e.getDivision())
                        .performedBy(e.getPerformedBy())
                        .timestamp(DateTimeConversionUtil.formatLocalDateTime(e.getTimestamp()))
                        .build())
                .collect(Collectors.toList());

        return BlockchainEventsResponse.builder()
                .events(dtos)
                .build();
    }
    private LocalDateTime parseNullableDate(String date) {
        if (date == null || date.isBlank()) return null;
        return DateTimeConversionUtil.parseLocalDate(date).atStartOfDay();
    }
}
