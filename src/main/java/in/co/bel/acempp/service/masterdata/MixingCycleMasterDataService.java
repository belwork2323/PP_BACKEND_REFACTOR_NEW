package in.co.bel.acempp.service.masterdata;

import in.co.bel.acempp.dto.masterdata.MasterDataStatsDto;
import in.co.bel.acempp.dto.masterdata.mixingcycles.*;
import in.co.bel.acempp.dto.mixing.mixingcycle.MixingCycleDto;
import in.co.bel.acempp.dto.mixing.qualitychecks.QualityCheckParams;
import in.co.bel.acempp.entity.manufacturing.MixingCycleMaster;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.manufacturing.MixingCycleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MixingCycleMasterDataService {

    private final MixingCycleRepository repository;
    private final QualityCheckParameterMapper qualityCheckParameterMapper;

    public static boolean isActive(MixingCycleMaster entity) {
        return MasterDataValidation.isActiveFlag(entity.getIsActive());
    }

    public static boolean isOperationActive(MixingCycleMaster.Operation operation) {
        return operation == null || operation.getIsActive() == null || Boolean.TRUE.equals(operation.getIsActive());
    }

    public static List<QualityCheckParams> activePremixQualityChecks(MixingCycleMaster entity) {
        if (entity == null || entity.getCycles() == null) {
            return List.of();
        }
        return QualityCheckParameterMapper.activeParameters(entity.getCycles().getPremixQualityChecks());
    }

    public static List<QualityCheckParams> activeFinalMixQualityChecks(MixingCycleMaster entity) {
        if (entity == null || entity.getCycles() == null) {
            return List.of();
        }
        return QualityCheckParameterMapper.activeParameters(entity.getCycles().getFinalMixQualityChecks());
    }

    public MixingCycleMasterListResponse list(MixingCycleListRequest request) {
        MixingCycleListRequest req = request == null ? new MixingCycleListRequest() : request;
        List<MixingCycleMaster> all = repository.findAll().stream()
                .sorted(MasterDataListOrdering.byLongIdDesc(MixingCycleMaster::getId))
                .toList();

        long active = all.stream().filter(MixingCycleMasterDataService::isActive).count();
        long inactive = all.size() - active;

        List<MixingCycleMasterRecordDto> items = all.stream()
                .filter(e -> matchesActive(e, req.getIsActive()))
                .filter(e -> matchesProjectId(e, req.getProjectId()))
                .filter(e -> req.getMotorStage() == null || Objects.equals(e.getMotorStage(), req.getMotorStage()))
                .filter(e -> matchesSearch(e, req.getSearch()))
                .map(this::toDto)
                .toList();

        return MixingCycleMasterListResponse.builder()
                .items(items)
                .stats(MasterDataStatsDto.builder()
                        .total(all.size())
                        .active(active)
                        .inactive(inactive)
                        .build())
                .build();
    }

    public MixingCycleMasterRecordDto create(MixingCycleMasterCreateRequest request) {
        String projectId = MasterDataValidation.requireTrimmed(request.getProjectId(), "Project");
        Integer motorStage = request.getMotorStage();
        if (motorStage == null) {
            throw new AcemException(ErrorCode.VALIDATION_ERROR, "Motor stage is required");
        }
        boolean willBeActive = request.getIsActive() == null || Boolean.TRUE.equals(request.getIsActive());

        long id = nextId();
        String code;
        while (true) {
            code = MasterDataCatalog.generatedCode(MasterDataCodePrefixes.MIXING_CYCLE, id);
            if (repository.findByMixingCycleCode(code).isEmpty()) {
                break;
            }
            id++;
        }
        MixingCycleMaster entity = MixingCycleMaster.builder()
                .id(id)
                .mixingCycleCode(code)
                .projectId(projectId)
                .motorStage(motorStage)
                .motorStageName(defaultMotorStageName(motorStage))
                .mixingCycleName(MasterDataValidation.validateName(request.getMixingCycleName()))
                .description(request.getDescription())
                .cycles(mapCycles(request.getCycles()))
                .isActive(willBeActive)
                .build();
        MasterDataAuditSupport.stampOnCreate(entity);
        return toDto(repository.save(entity));
    }

    public MixingCycleMasterRecordDto update(MixingCycleMasterUpdateRequest request) {
        MixingCycleMaster entity = repository.findById(request.getId())
                .orElseThrow(() -> new AcemException(ErrorCode.MASTER_DATA_NOT_FOUND, "Mixing cycle not found"));
        if (request.getMixingCycleCode() != null && !request.getMixingCycleCode().isBlank()
                && !Objects.equals(entity.getMixingCycleCode(), request.getMixingCycleCode().trim())) {
            throw new AcemException(ErrorCode.READONLY_FIELD, "Mixing cycle code cannot be changed");
        }
        if (request.getProjectId() != null
                && !request.getProjectId().isBlank()
                && !Objects.equals(entity.getProjectId(), request.getProjectId().trim())) {
            throw new AcemException(ErrorCode.READONLY_FIELD, "Project cannot be changed");
        }
        if (request.getMotorStage() != null && !Objects.equals(entity.getMotorStage(), request.getMotorStage())) {
            throw new AcemException(ErrorCode.READONLY_FIELD, "Motor stage cannot be changed");
        }
        entity.setMixingCycleName(MasterDataValidation.validateName(request.getMixingCycleName()));
        entity.setDescription(request.getDescription());
        entity.setCycles(mapCyclesForUpdate(request.getCycles(), entity.getCycles()));
        entity.setIsActive(Boolean.TRUE.equals(request.getIsActive()));
        MasterDataAuditSupport.stampOnUpdate(entity);
        return toDto(repository.save(entity));
    }

    public MixingCycleMasterRecordDto softDisable(MixingCycleMasterDeleteRequest request) {
        MixingCycleMaster entity = repository.findById(request.getId())
                .orElseThrow(() -> new AcemException(ErrorCode.MASTER_DATA_NOT_FOUND, "Mixing cycle not found"));
        entity.setIsActive(false);
        MasterDataAuditSupport.stampOnUpdate(entity);
        return toDto(repository.save(entity));
    }

    public MixingCycleMasterRecordDto enable(MixingCycleMasterDeleteRequest request) {
        MixingCycleMaster entity = repository.findById(request.getId())
                .orElseThrow(() -> new AcemException(ErrorCode.MASTER_DATA_NOT_FOUND, "Mixing cycle not found"));
        if (isActive(entity)) {
            return toDto(entity);
        }
        entity.setIsActive(true);
        MasterDataAuditSupport.stampOnUpdate(entity);
        return toDto(repository.save(entity));
    }

    /** Prefer the active configuration for a project + stage (batch / ops). */
    public List<MixingCycleDto> findActiveByProjectIdAndMotorStage(
            MixingCycleListRequest request) {
        if (request.getProjectId() == null || request.getProjectId().isBlank() || request.getMotorStage() == null) {
            return Collections.emptyList();
        }

        return repository.findAllByProjectIdAndMotorStage(request.getProjectId().trim(), request.getMotorStage())
                .stream()
                .filter(MixingCycleMasterDataService::isActive)
                .map(entity -> {
                    MixingCycleDto dto = new MixingCycleDto();
                    dto.setMixingCycleId(entity.getId()); // Adjust getter based on your entity
                    dto.setMixingCycleCode(entity.getMixingCycleCode()); // Adjust getter based on your entity
                    dto.setMixingCycleName(entity.getMixingCycleName()); // Adjust getter based on your entity
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private void assertNoOtherActiveProjectStage(String projectId, Integer motorStage, Long excludeId) {
        String pid = projectId == null ? "" : projectId.trim();
        if (pid.isEmpty() || motorStage == null) {
            throw new AcemException(ErrorCode.VALIDATION_ERROR, "Project and motor stage are required");
        }
        boolean conflict = repository.findAllByProjectIdAndMotorStage(pid, motorStage).stream()
                .filter(MixingCycleMasterDataService::isActive)
                .anyMatch(existing -> excludeId == null || !Objects.equals(existing.getId(), excludeId));
        if (conflict) {
            throw new AcemException(ErrorCode.DUPLICATE_MASTER_CODE,
                    "Mixing cycle already exists and is active for this project and motor stage "
                            + motorStage);
        }
    }


    private void assertCycleSectionsNotEmpty(MixingCycleMasterCreateRequest.CycleGroupWriteDto cycles) {
        if (cycles == null) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "Premix and final mix operations and quality checks are required");
        }
        if (cycles.getPremixOperations() == null || cycles.getPremixOperations().isEmpty()) {
            throw new AcemException(ErrorCode.INVALID_REQUEST, "Add at least one premix operation");
        }
        if (cycles.getPremixQualityChecks() == null || cycles.getPremixQualityChecks().isEmpty()) {
            throw new AcemException(ErrorCode.INVALID_REQUEST, "Add at least one premix quality check");
        }
        if (cycles.getFinalMixOperations() == null || cycles.getFinalMixOperations().isEmpty()) {
            throw new AcemException(ErrorCode.INVALID_REQUEST, "Add at least one final mix operation");
        }
        if (cycles.getFinalMixQualityChecks() == null || cycles.getFinalMixQualityChecks().isEmpty()) {
            throw new AcemException(ErrorCode.INVALID_REQUEST, "Add at least one final mix quality check");
        }
    }

    private MixingCycleMaster.CycleGroup mapCycles(MixingCycleMasterCreateRequest.CycleGroupWriteDto cycles) {
        assertCycleSectionsNotEmpty(cycles);
        return MixingCycleMaster.CycleGroup.builder()
                .premixOperations(mapOperations(cycles.getPremixOperations()))
                .finalMixOperations(mapOperations(cycles.getFinalMixOperations()))
                .premixQualityChecks(qualityCheckParameterMapper.mapChecks(cycles.getPremixQualityChecks(), true))
                .finalMixQualityChecks(qualityCheckParameterMapper.mapChecks(cycles.getFinalMixQualityChecks(), true))
                .build();
    }

    private MixingCycleMaster.CycleGroup mapCyclesForUpdate(
            MixingCycleMasterCreateRequest.CycleGroupWriteDto incoming,
            MixingCycleMaster.CycleGroup existing) {
        assertCycleSectionsNotEmpty(incoming);
        List<MixingCycleMaster.Operation> existingPremix = existing == null || existing.getPremixOperations() == null
                ? List.of()
                : existing.getPremixOperations();
        List<MixingCycleMaster.Operation> existingFinal = existing == null || existing.getFinalMixOperations() == null
                ? List.of()
                : existing.getFinalMixOperations();
        List<QualityCheckParams> existingPremixQc = existing == null || existing.getPremixQualityChecks() == null
                ? List.of()
                : existing.getPremixQualityChecks();
        List<QualityCheckParams> existingFinalQc = existing == null || existing.getFinalMixQualityChecks() == null
                ? List.of()
                : existing.getFinalMixQualityChecks();
        return MixingCycleMaster.CycleGroup.builder()
                .premixOperations(mapOperationsForUpdate(
                        incoming.getPremixOperations(),
                        existingPremix,
                        nextOperationId(existingPremix, existingFinal)))
                .finalMixOperations(mapOperationsForUpdate(
                        incoming.getFinalMixOperations(),
                        existingFinal,
                        nextOperationId(existingPremix, existingFinal)))
                .premixQualityChecks(qualityCheckParameterMapper.mapChecksForUpdate(
                        incoming.getPremixQualityChecks(), existingPremixQc, true))
                .finalMixQualityChecks(qualityCheckParameterMapper.mapChecksForUpdate(
                        incoming.getFinalMixQualityChecks(), existingFinalQc, true))
                .build();
    }

    private List<MixingCycleMaster.Operation> mapOperations(
            List<MixingCycleMasterCreateRequest.OperationWriteDto> ops) {
        if (ops == null || ops.isEmpty()) {
            return new ArrayList<>();
        }
        long nextOpId = 1L;
        List<MixingCycleMaster.Operation> result = new ArrayList<>();
        int seq = 1;
        for (MixingCycleMasterCreateRequest.OperationWriteDto op : ops) {
            Long operationId = op.getOperationId();
            if (operationId == null) {
                operationId = nextOpId++;
            } else {
                nextOpId = Math.max(nextOpId, operationId + 1);
            }
            Integer sequenceNo = op.getSequenceNo() == null ? seq++ : op.getSequenceNo();
            result.add(MixingCycleMaster.Operation.builder()
                    .operationId(operationId)
                    .sequenceNo(sequenceNo)
                    .operationName(MasterDataValidation.validateName(op.getOperationName()))
                    .isActive(resolveNestedActive(op.getIsActive()))
                    .build());
        }
        return result;
    }

    private List<MixingCycleMaster.Operation> mapOperationsForUpdate(
            List<MixingCycleMasterCreateRequest.OperationWriteDto> incoming,
            List<MixingCycleMaster.Operation> existing,
            long nextOpIdStart) {
        if (incoming == null) {
            incoming = List.of();
        }
        Map<Long, MixingCycleMaster.Operation> existingById = indexOperationsById(existing);
        Set<Long> seenIds = new HashSet<>();
        List<MixingCycleMaster.Operation> result = new ArrayList<>();
        long nextOpId = nextOpIdStart;
        int seq = 1;
        for (MixingCycleMasterCreateRequest.OperationWriteDto op : incoming) {
            Long operationId = op.getOperationId();
            if (operationId != null) {
                if (!seenIds.add(operationId)) {
                    throw new AcemException(ErrorCode.DUPLICATE_MASTER_CODE,
                            "Duplicate operation id: " + operationId);
                }
                MixingCycleMaster.Operation existingOp = existingById.get(operationId);
                if (existingOp == null) {
                    throw new AcemException(ErrorCode.INVALID_REQUEST,
                            "Unknown operation id: " + operationId);
                }
                assertExistingOperationUnchanged(existingOp, op, operationId);
                result.add(MixingCycleMaster.Operation.builder()
                        .operationId(existingOp.getOperationId())
                        .sequenceNo(existingOp.getSequenceNo())
                        .operationName(existingOp.getOperationName())
                        .isActive(resolveNestedActive(op.getIsActive()))
                        .build());
                continue;
            }
            Integer sequenceNo = op.getSequenceNo() == null ? seq++ : op.getSequenceNo();
            result.add(MixingCycleMaster.Operation.builder()
                    .operationId(nextOpId++)
                    .sequenceNo(sequenceNo)
                    .operationName(MasterDataValidation.validateName(op.getOperationName()))
                    .isActive(resolveNestedActive(op.getIsActive()))
                    .build());
        }
        for (Long existingId : existingById.keySet()) {
            if (!seenIds.contains(existingId)) {
                throw new AcemException(ErrorCode.READONLY_FIELD,
                        "Existing operation cannot be removed: " + existingId);
            }
        }
        return result;
    }

    private static Map<Long, MixingCycleMaster.Operation> indexOperationsById(
            List<MixingCycleMaster.Operation> operations) {
        Map<Long, MixingCycleMaster.Operation> indexed = new HashMap<>();
        if (operations == null) {
            return indexed;
        }
        for (MixingCycleMaster.Operation operation : operations) {
            if (operation.getOperationId() == null) {
                continue;
            }
            indexed.put(operation.getOperationId(), operation);
        }
        return indexed;
    }

    private static void assertExistingOperationUnchanged(
            MixingCycleMaster.Operation existing,
            MixingCycleMasterCreateRequest.OperationWriteDto incoming,
            Long operationId) {
        String existingName = existing.getOperationName() == null ? "" : existing.getOperationName().trim();
        String incomingName = incoming.getOperationName() == null ? "" : incoming.getOperationName().trim();
        if (!existingName.equals(incomingName)) {
            throw new AcemException(ErrorCode.READONLY_FIELD,
                    "Existing operation name cannot be changed: " + operationId);
        }
        if (!Objects.equals(existing.getSequenceNo(), incoming.getSequenceNo())) {
            throw new AcemException(ErrorCode.READONLY_FIELD,
                    "Existing operation sequence cannot be changed: " + operationId);
        }
    }

    private static long nextOperationId(
            List<MixingCycleMaster.Operation> premix,
            List<MixingCycleMaster.Operation> finalMix) {
        long max = 0L;
        for (MixingCycleMaster.Operation op : premix) {
            if (op.getOperationId() != null) {
                max = Math.max(max, op.getOperationId());
            }
        }
        for (MixingCycleMaster.Operation op : finalMix) {
            if (op.getOperationId() != null) {
                max = Math.max(max, op.getOperationId());
            }
        }
        return max + 1L;
    }

    private MixingCycleMasterRecordDto toDto(MixingCycleMaster entity) {
        MixingCycleMasterRecordDto.CycleGroupDto cyclesDto = null;
        if (entity.getCycles() != null) {
            cyclesDto = MixingCycleMasterRecordDto.CycleGroupDto.builder()
                    .premixOperations(mapOpDtos(entity.getCycles().getPremixOperations()))
                    .finalMixOperations(mapOpDtos(entity.getCycles().getFinalMixOperations()))
                    .premixQualityChecks(mapQcDtos(entity.getCycles().getPremixQualityChecks()))
                    .finalMixQualityChecks(mapQcDtos(entity.getCycles().getFinalMixQualityChecks()))
                    .build();
        }
        return MixingCycleMasterRecordDto.builder()
                .id(entity.getId())
                .mixingCycleCode(entity.getMixingCycleCode())
                .mixingCycleName(entity.getMixingCycleName())
                .description(entity.getDescription())
                .projectId(entity.getProjectId())
                .motorStage(entity.getMotorStage())
                .motorStageName(entity.getMotorStageName())
                .isActive(isActive(entity))
                .cycles(cyclesDto)
                .createdBy(MasterDataValidation.auditCreatedBy(entity))
                .createdOn(MasterDataValidation.auditCreatedOn(entity))
                .updatedBy(MasterDataValidation.auditUpdatedBy(entity))
                .updatedOn(MasterDataValidation.auditUpdatedOn(entity))
                .build();
    }

    private List<MixingCycleMasterRecordDto.OperationDto> mapOpDtos(List<MixingCycleMaster.Operation> ops) {
        if (ops == null) return List.of();
        return ops.stream()
                .map(op -> MixingCycleMasterRecordDto.OperationDto.builder()
                        .operationId(op.getOperationId())
                        .sequenceNo(op.getSequenceNo())
                        .operationName(op.getOperationName())
                        .isActive(resolveNestedActive(op.getIsActive()))
                        .build())
                .toList();
    }

    private List<QualityCheckParams> mapQcDtos(List<QualityCheckParams> params) {
        if (params == null) return List.of();
        return params.stream()
                .map(QualityCheckParameterMapper::toParameterDto)
                .toList();
    }

    private static boolean resolveNestedActive(Boolean isActive) {
        return isActive == null || Boolean.TRUE.equals(isActive);
    }

    private boolean matchesActive(MixingCycleMaster e, Boolean filter) {
        if (filter == null) return true;
        return filter == isActive(e);
    }

    private boolean matchesProjectId(MixingCycleMaster e, String projectId) {
        if (projectId == null || projectId.isBlank()) return true;
        return Objects.equals(e.getProjectId(), projectId.trim());
    }

    private boolean matchesSearch(MixingCycleMaster e, String search) {
        if (search == null || search.isBlank()) return true;
        String q = search.trim().toLowerCase(Locale.ROOT);
        return (e.getMixingCycleCode() != null && e.getMixingCycleCode().toLowerCase(Locale.ROOT).contains(q))
                || (e.getMixingCycleName() != null && e.getMixingCycleName().toLowerCase(Locale.ROOT).contains(q))
                || (e.getProjectId() != null && e.getProjectId().toLowerCase(Locale.ROOT).contains(q))
                || (e.getMotorStageName() != null && e.getMotorStageName().toLowerCase(Locale.ROOT).contains(q))
                || (e.getMotorStage() != null && String.valueOf(e.getMotorStage()).contains(q));
    }

    private static String defaultMotorStageName(Integer motorStage) {
        return motorStage == null ? "" : "Stage " + motorStage;
    }

    private long nextId() {
        return repository.findAll().stream()
                .map(MixingCycleMaster::getId)
                .filter(Objects::nonNull)
                .max(Long::compareTo)
                .orElse(0L) + 1L;
    }
}
