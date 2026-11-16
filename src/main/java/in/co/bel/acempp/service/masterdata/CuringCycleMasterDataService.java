package in.co.bel.acempp.service.masterdata;

import in.co.bel.acempp.dto.masterdata.MasterDataStatsDto;
import in.co.bel.acempp.dto.masterdata.curingcycles.*;
import in.co.bel.acempp.entity.manufacturing.CuringCycleMaster;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.manufacturing.CuringCycleMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CuringCycleMasterDataService {

    private final CuringCycleMasterRepository repository;

    public static boolean isActive(CuringCycleMaster entity) {
        return MasterDataValidation.isActiveFlag(entity.getIsActive());
    }

    public static boolean isStepActive(CuringCycleMaster.Cycle step) {
        return step == null || step.getIsActive() == null || Boolean.TRUE.equals(step.getIsActive());
    }

    public static List<CuringCycleMaster.Cycle> activeSteps(CuringCycleMaster entity) {
        if (entity == null || entity.getCycles() == null) {
            return List.of();
        }
        return entity.getCycles().stream()
                .filter(CuringCycleMasterDataService::isStepActive)
                .toList();
    }

    public CuringCycleMasterListResponse list(CuringCycleListRequest request) {
        CuringCycleListRequest req = request == null ? new CuringCycleListRequest() : request;
        List<CuringCycleMaster> all = repository.findAll();

        long active = all.stream().filter(CuringCycleMasterDataService::isActive).count();
        long inactive = all.size() - active;

        List<CuringCycleMasterRecordDto> items = all.stream()
                .filter(e -> matchesActive(e, req.getIsActive()))
                .filter(e -> matchesProjectId(e, req.getProjectId()))
                .filter(e -> req.getMotorStage() == null || Objects.equals(e.getMotorStage(), req.getMotorStage()))
                .filter(e -> matchesSearch(e, req.getSearch()))
                .sorted(Comparator
                        .comparing((CuringCycleMaster e) -> nullToEmpty(e.getProjectId()))
                        .thenComparing(e -> e.getMotorStage() == null ? Integer.MIN_VALUE : e.getMotorStage())
                        .thenComparing(e -> nullToEmpty(e.getCuringCycleCode())))
                .map(this::toDto)
                .toList();

        return CuringCycleMasterListResponse.builder()
                .items(items)
                .stats(MasterDataStatsDto.builder()
                        .total(all.size())
                        .active(active)
                        .inactive(inactive)
                        .build())
                .build();
    }

    public CuringCycleMasterRecordDto create(CuringCycleMasterCreateRequest request) {
        String projectId = MasterDataValidation.requireTrimmed(request.getProjectId(), "Project");
        Integer motorStage = request.getMotorStage();
        if (motorStage == null) {
            throw new AcemException(ErrorCode.VALIDATION_ERROR, "Motor stage is required");
        }
        String curingType = MasterDataValidation.requireTrimmed(request.getCuringType(), "Curing type");
        boolean willBeActive = request.getIsActive() == null || Boolean.TRUE.equals(request.getIsActive());
        if (willBeActive) {
            assertNoOtherActiveProjectStage(projectId, motorStage, curingType, null);
        }

        long id = nextId();
        String code;
        while (true) {
            code = MasterDataCatalog.generatedCode(MasterDataCodePrefixes.CURING_CYCLE, id);
            if (repository.findByCuringCycleCode(code).isEmpty()) {
                break;
            }
            id++;
        }

        CuringCycleMaster entity = CuringCycleMaster.builder()
                .id(id)
                .curingCycleCode(code)
                .projectId(projectId)
                .motorStage(motorStage)
                .motorStageName(defaultMotorStageName(motorStage))
                .curingType(curingType)
                .cycles(mapCycles(request.getCycles()))
                .isActive(willBeActive)
                .build();

        MasterDataAuditSupport.stampOnCreate(entity);
        return toDto(repository.save(entity));
    }

    public CuringCycleMasterRecordDto update(CuringCycleMasterUpdateRequest request) {
        CuringCycleMaster entity = repository.findById(request.getId())
                .orElseThrow(() -> new AcemException(ErrorCode.MASTER_DATA_NOT_FOUND, "Curing cycle not found"));
        if (request.getProjectId() != null
                && !request.getProjectId().isBlank()
                && !Objects.equals(entity.getProjectId(), request.getProjectId().trim())) {
            throw new AcemException(ErrorCode.READONLY_FIELD, "Project cannot be changed");
        }
        if (request.getMotorStage() != null && !Objects.equals(entity.getMotorStage(), request.getMotorStage())) {
            throw new AcemException(ErrorCode.READONLY_FIELD, "Motor stage cannot be changed");
        }
        if (request.getCuringType() != null
                && !request.getCuringType().isBlank()
                && !Objects.equals(entity.getCuringType(), request.getCuringType().trim())) {
            throw new AcemException(ErrorCode.READONLY_FIELD, "Curing type cannot be changed");
        }
        boolean enabling = Boolean.TRUE.equals(request.getIsActive()) && !isActive(entity);
        if (enabling) {
            assertNoOtherActiveProjectStage(
                    entity.getProjectId(), entity.getMotorStage(), entity.getCuringType(), entity.getId());
        }
        entity.setCycles(mapCyclesForUpdate(request.getCycles(), entity.getCycles()));
        entity.setIsActive(Boolean.TRUE.equals(request.getIsActive()));
        MasterDataAuditSupport.stampOnUpdate(entity);
        return toDto(repository.save(entity));
    }

    public CuringCycleMasterRecordDto softDisable(CuringCycleMasterDeleteRequest request) {
        CuringCycleMaster entity = repository.findById(request.getId())
                .orElseThrow(() -> new AcemException(ErrorCode.MASTER_DATA_NOT_FOUND, "Curing cycle not found"));
        entity.setIsActive(false);
        MasterDataAuditSupport.stampOnUpdate(entity);
        return toDto(repository.save(entity));
    }

    public CuringCycleMasterRecordDto enable(CuringCycleMasterDeleteRequest request) {
        CuringCycleMaster entity = repository.findById(request.getId())
                .orElseThrow(() -> new AcemException(ErrorCode.MASTER_DATA_NOT_FOUND, "Curing cycle not found"));
        if (isActive(entity)) {
            return toDto(entity);
        }
        assertNoOtherActiveProjectStage(
                entity.getProjectId(), entity.getMotorStage(), entity.getCuringType(), entity.getId());
        entity.setIsActive(true);
        MasterDataAuditSupport.stampOnUpdate(entity);
        return toDto(repository.save(entity));
    }

    /** Prefer the active configuration for a project + stage (ops / QC). */
    public static Optional<CuringCycleMaster> findActiveByProjectIdAndMotorStage(
            CuringCycleMasterRepository repository, String projectId, Integer motorStage) {
        if (projectId == null || projectId.isBlank() || motorStage == null) {
            return Optional.empty();
        }
        return repository.findAllByProjectIdAndMotorStage(projectId.trim(), motorStage).stream()
                .filter(CuringCycleMasterDataService::isActive)
                .findFirst();
    }

    /** Prefer the active configuration for a project + stage + curing type (CC lookup). */
    public static Optional<CuringCycleMaster> findActiveByProjectIdAndMotorStageAndCuringType(
            CuringCycleMasterRepository repository,
            String projectId,
            Integer motorStage,
            String curingType) {
        if (projectId == null || projectId.isBlank()
                || motorStage == null
                || curingType == null || curingType.isBlank()) {
            return Optional.empty();
        }
        String trimmedCuringType = curingType.trim();
        return repository.findAllByProjectIdAndMotorStage(projectId.trim(), motorStage).stream()
                .filter(CuringCycleMasterDataService::isActive)
                .filter(e -> Objects.equals(
                        e.getCuringType() == null ? null : e.getCuringType().trim(),
                        trimmedCuringType))
                .findFirst();
    }

    private void assertNoOtherActiveProjectStage(
            String projectId, Integer motorStage, String curingType, Long excludeId) {
        String pid = projectId == null ? "" : projectId.trim();
        String type = curingType == null ? "" : curingType.trim();
        if (pid.isEmpty() || motorStage == null || type.isEmpty()) {
            throw new AcemException(
                    ErrorCode.VALIDATION_ERROR,
                    "Project, motor stage and curing type are required");
        }
        boolean conflict = repository.findAllByProjectIdAndMotorStage(pid, motorStage).stream()
                .filter(CuringCycleMasterDataService::isActive)
                .filter(existing -> Objects.equals(
                        existing.getCuringType() == null ? null : existing.getCuringType().trim(),
                        type))
                .anyMatch(existing -> excludeId == null || !Objects.equals(existing.getId(), excludeId));
        if (conflict) {
            throw new AcemException(ErrorCode.DUPLICATE_MASTER_CODE,
                    "Curing cycle already exists and is active for this project, motor stage "
                            + motorStage + " and curing type " + type);
        }
    }

    private List<CuringCycleMaster.Cycle> mapCycles(List<CuringCycleMasterRecordDto.CycleDto> cycles) {
        if (cycles == null || cycles.isEmpty()) {
            return new ArrayList<>();
        }
        long nextStepId = 1L;
        List<CuringCycleMaster.Cycle> result = new ArrayList<>();
        int seq = 1;
        for (CuringCycleMasterRecordDto.CycleDto cycle : cycles) {
            Long stepId = cycle.getStepId();
            if (stepId == null) {
                stepId = nextStepId++;
            } else {
                nextStepId = Math.max(nextStepId, stepId + 1);
            }
            Integer sequenceNo = cycle.getSequenceNo() == null ? seq++ : cycle.getSequenceNo();
            result.add(buildCycle(cycle, stepId, sequenceNo));
        }
        return result;
    }

    private List<CuringCycleMaster.Cycle> mapCyclesForUpdate(
            List<CuringCycleMasterRecordDto.CycleDto> incoming,
            List<CuringCycleMaster.Cycle> existing) {
        if (incoming == null) {
            incoming = List.of();
        }
        List<CuringCycleMaster.Cycle> existingSteps = existing == null ? List.of() : existing;
        Map<Long, CuringCycleMaster.Cycle> existingById = indexStepsById(existingSteps);
        Set<Long> seenIds = new HashSet<>();
        List<CuringCycleMaster.Cycle> result = new ArrayList<>();
        long nextStepId = nextStepId(existingSteps);
        int nextSeq = nextSequenceNo(existingSteps);
        for (CuringCycleMasterRecordDto.CycleDto cycle : incoming) {
            Long stepId = cycle.getStepId();
            if (stepId != null) {
                if (!seenIds.add(stepId)) {
                    throw new AcemException(ErrorCode.DUPLICATE_MASTER_CODE,
                            "Duplicate step id: " + stepId);
                }
                CuringCycleMaster.Cycle existingStep = existingById.get(stepId);
                if (existingStep == null) {
                    throw new AcemException(ErrorCode.INVALID_REQUEST,
                            "Unknown step id: " + stepId);
                }
                assertExistingStepUnchanged(existingStep, cycle, stepId);
                result.add(CuringCycleMaster.Cycle.builder()
                        .stepId(existingStep.getStepId())
                        .sequenceNo(existingStep.getSequenceNo())
                        .temperature(existingStep.getTemperature())
                        .durationMinutes(existingStep.getDurationMinutes())
                        .startDateTime(existingStep.getStartDateTime())
                        .endDateTime(existingStep.getEndDateTime())
                        .hotWaterCirculation(existingStep.getHotWaterCirculation())
                        .isActive(resolveNestedActive(cycle.getIsActive()))
                        .build());
                continue;
            }
            Integer sequenceNo = cycle.getSequenceNo() == null ? nextSeq++ : cycle.getSequenceNo();
            result.add(buildCycle(cycle, nextStepId++, sequenceNo));
        }
        for (Long existingId : existingById.keySet()) {
            if (!seenIds.contains(existingId)) {
                throw new AcemException(ErrorCode.READONLY_FIELD,
                        "Existing step cannot be removed: " + existingId);
            }
        }
        return result;
    }

    private static CuringCycleMaster.Cycle buildCycle(
            CuringCycleMasterRecordDto.CycleDto cycle,
            Long stepId,
            Integer sequenceNo) {
        return CuringCycleMaster.Cycle.builder()
                .stepId(stepId)
                .sequenceNo(sequenceNo)
                .temperature(cycle.getTemperature())
                .durationMinutes(cycle.getDurationMinutes())
                .startDateTime(cycle.getStartDateTime())
                .endDateTime(cycle.getEndDateTime())
                .hotWaterCirculation(cycle.getHotWaterCirculation())
                .isActive(resolveNestedActive(cycle.getIsActive()))
                .build();
    }

    private static Map<Long, CuringCycleMaster.Cycle> indexStepsById(List<CuringCycleMaster.Cycle> steps) {
        Map<Long, CuringCycleMaster.Cycle> indexed = new HashMap<>();
        for (CuringCycleMaster.Cycle step : steps) {
            if (step.getStepId() == null) {
                continue;
            }
            indexed.put(step.getStepId(), step);
        }
        return indexed;
    }

    private static void assertExistingStepUnchanged(
            CuringCycleMaster.Cycle existing,
            CuringCycleMasterRecordDto.CycleDto incoming,
            Long stepId) {
        if (!Objects.equals(existing.getSequenceNo(), incoming.getSequenceNo())) {
            throw new AcemException(ErrorCode.READONLY_FIELD,
                    "Existing step sequence cannot be changed: " + stepId);
        }
        if (!Objects.equals(existing.getTemperature(), incoming.getTemperature())) {
            throw new AcemException(ErrorCode.READONLY_FIELD,
                    "Existing step temperature cannot be changed: " + stepId);
        }
        if (!Objects.equals(existing.getDurationMinutes(), incoming.getDurationMinutes())) {
            throw new AcemException(ErrorCode.READONLY_FIELD,
                    "Existing step duration cannot be changed: " + stepId);
        }
    }

    private static long nextStepId(List<CuringCycleMaster.Cycle> steps) {
        long max = 0L;
        for (CuringCycleMaster.Cycle step : steps) {
            if (step.getStepId() != null) {
                max = Math.max(max, step.getStepId());
            }
        }
        return max + 1L;
    }

    private static int nextSequenceNo(List<CuringCycleMaster.Cycle> steps) {
        int max = 0;
        for (CuringCycleMaster.Cycle step : steps) {
            if (step.getSequenceNo() != null) {
                max = Math.max(max, step.getSequenceNo());
            }
        }
        return max + 1;
    }

    private CuringCycleMasterRecordDto toDto(CuringCycleMaster entity) {
        List<CuringCycleMasterRecordDto.CycleDto> cycles = entity.getCycles() == null
                ? List.of()
                : entity.getCycles().stream()
                .map(this::toCycleDto)
                .toList();
        return CuringCycleMasterRecordDto.builder()
                .id(entity.getId())
                .curingCycleCode(entity.getCuringCycleCode())
                .projectId(entity.getProjectId())
                .motorStage(entity.getMotorStage())
                .motorStageName(entity.getMotorStageName())
                .curingType(entity.getCuringType())
                .isActive(isActive(entity))
                .cycles(cycles)
                .createdBy(MasterDataValidation.auditCreatedBy(entity))
                .createdOn(MasterDataValidation.auditCreatedOn(entity))
                .updatedBy(MasterDataValidation.auditUpdatedBy(entity))
                .updatedOn(MasterDataValidation.auditUpdatedOn(entity))
                .build();
    }

    private CuringCycleMasterRecordDto.CycleDto toCycleDto(CuringCycleMaster.Cycle cycle) {
        return CuringCycleMasterRecordDto.CycleDto.builder()
                .stepId(cycle.getStepId())
                .sequenceNo(cycle.getSequenceNo())
                .temperature(cycle.getTemperature())
                .durationMinutes(cycle.getDurationMinutes())
                .startDateTime(cycle.getStartDateTime())
                .endDateTime(cycle.getEndDateTime())
                .hotWaterCirculation(cycle.getHotWaterCirculation())
                .isActive(resolveNestedActive(cycle.getIsActive()))
                .build();
    }

    private static boolean resolveNestedActive(Boolean isActive) {
        return isActive == null || Boolean.TRUE.equals(isActive);
    }

    private static String defaultMotorStageName(Integer motorStage) {
        return motorStage == null ? "" : "Stage " + motorStage;
    }

    private boolean matchesActive(CuringCycleMaster e, Boolean filter) {
        if (filter == null) return true;
        return filter == isActive(e);
    }

    private boolean matchesProjectId(CuringCycleMaster e, String projectId) {
        if (projectId == null || projectId.isBlank()) return true;
        return Objects.equals(e.getProjectId(), projectId.trim());
    }

    private boolean matchesSearch(CuringCycleMaster e, String search) {
        if (search == null || search.isBlank()) return true;
        String q = search.trim().toLowerCase(Locale.ROOT);
        return (e.getCuringCycleCode() != null && e.getCuringCycleCode().toLowerCase(Locale.ROOT).contains(q))
                || (e.getProjectId() != null && e.getProjectId().toLowerCase(Locale.ROOT).contains(q))
                || (e.getCuringType() != null && e.getCuringType().toLowerCase(Locale.ROOT).contains(q))
                || (e.getMotorStageName() != null && e.getMotorStageName().toLowerCase(Locale.ROOT).contains(q))
                || (e.getMotorStage() != null && String.valueOf(e.getMotorStage()).contains(q));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private long nextId() {
        return repository.findAll().stream()
                .map(CuringCycleMaster::getId)
                .filter(Objects::nonNull)
                .max(Long::compareTo)
                .orElse(0L) + 1L;
    }
}
