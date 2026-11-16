package in.co.bel.acempp.service.masterdata;

import in.co.bel.acempp.dto.masterdata.MasterDataCreateRequest;
import in.co.bel.acempp.dto.masterdata.MasterDataRecordDto;
import in.co.bel.acempp.dto.masterdata.MasterDataTypeDescriptor;
import in.co.bel.acempp.dto.masterdata.MasterDataUpdateRequest;
import in.co.bel.acempp.entity.MotorStage;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.MotorStageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class MotorStageMasterDataHandler implements MasterDataTypeHandler {

    private final MotorStageRepository repository;

    @Override
    public String type() {
        return MasterDataCatalog.MOTOR_STAGES;
    }

    @Override
    public List<MasterDataRecordDto> listAll() {
        return repository.findAll().stream()
                .sorted(Comparator
                        .comparing(MotorStage::getProjectId, Comparator.nullsLast(String::compareTo))
                        .thenComparing(MotorStage::getMotorStage, Comparator.nullsLast(Integer::compareTo)))
                .map(this::toDto)
                .toList();
    }

    @Override
    public MasterDataRecordDto create(MasterDataCreateRequest request) {
        MasterDataTypeDescriptor schema = MasterDataCatalog.get(type());
        Map<String, Object> attrs = MasterDataValidation.validateAttributes(schema, request.getAttributes());
        String projectId = requireProjectId(attrs.get("projectId"));
        Integer motorStage = (Integer) attrs.get("motorStage");
        boolean willBeActive = request.getIsActive() == null || Boolean.TRUE.equals(request.getIsActive());
        if (willBeActive) {
            assertNoActiveProjectStage(projectId, motorStage, null);
        }

        String name = defaultMotorStageName(motorStage);

        MotorStage entity = MotorStage.builder()
                .stageId(nextId())
                .projectId(projectId)
                .motorStage(motorStage)
                .motorStageName(name)
                .isActive(willBeActive)
                .build();
        MasterDataAuditSupport.stampOnCreate(entity);
        return toDto(repository.save(entity));
    }

    @Override
    public MasterDataRecordDto update(MasterDataUpdateRequest request) {
        MasterDataTypeDescriptor schema = MasterDataCatalog.get(type());
        MotorStage entity = repository.findFirstByStageId(request.getId())
                .orElseThrow(() -> new AcemException(ErrorCode.MASTER_DATA_NOT_FOUND, "Motor stage not found"));
        String expectedCode = MasterDataCatalog.generatedCode(
                MasterDataCodePrefixes.MOTOR_STAGE, entity.getMotorStage());
        if (request.getCode() != null && !request.getCode().isBlank()
                && !Objects.equals(expectedCode, request.getCode().trim())) {
            throw new AcemException(ErrorCode.READONLY_FIELD, "Code cannot be changed");
        }
        Map<String, Object> attrs = MasterDataValidation.validateAttributes(schema, request.getAttributes());
        Integer motorStage = (Integer) attrs.get("motorStage");
        if (!Objects.equals(entity.getMotorStage(), motorStage)) {
            throw new AcemException(ErrorCode.READONLY_FIELD, "Motor stage number cannot be changed");
        }

        String requestedProjectId = asTrimmedString(attrs.get("projectId"));
        String existingProjectId = asTrimmedString(entity.getProjectId());
        if (existingProjectId != null) {
            if (requestedProjectId != null && !Objects.equals(existingProjectId, requestedProjectId)) {
                throw new AcemException(ErrorCode.READONLY_FIELD, "Project cannot be changed");
            }
        } else if (requestedProjectId != null) {
            if (Boolean.TRUE.equals(request.getIsActive())) {
                assertNoActiveProjectStage(requestedProjectId, entity.getMotorStage(), entity.getStageId());
            }
            entity.setProjectId(requestedProjectId);
        }

        boolean enabling = Boolean.TRUE.equals(request.getIsActive()) && !isActive(entity);
        if (enabling) {
            assertNoActiveProjectStage(entity.getProjectId(), entity.getMotorStage(), entity.getStageId());
        }

        entity.setMotorStageName(defaultMotorStageName(entity.getMotorStage()));
        entity.setIsActive(Boolean.TRUE.equals(request.getIsActive()));
        MasterDataAuditSupport.stampOnUpdate(entity);
        return toDto(repository.save(entity));
    }

    @Override
    public MasterDataRecordDto softDisable(Long id) {
        MotorStage entity = repository.findFirstByStageId(id)
                .orElseThrow(() -> new AcemException(ErrorCode.MASTER_DATA_NOT_FOUND, "Motor stage not found"));
        entity.setIsActive(false);
        MasterDataAuditSupport.stampOnUpdate(entity);
        return toDto(repository.save(entity));
    }

    @Override
    public MasterDataRecordDto enable(Long id) {
        MotorStage entity = repository.findFirstByStageId(id)
                .orElseThrow(() -> new AcemException(ErrorCode.MASTER_DATA_NOT_FOUND, "Motor stage not found"));
        if (isActive(entity)) {
            return toDto(entity);
        }
        assertNoActiveProjectStage(entity.getProjectId(), entity.getMotorStage(), entity.getStageId());
        entity.setIsActive(true);
        MasterDataAuditSupport.stampOnUpdate(entity);
        return toDto(repository.save(entity));
    }

    public static boolean isActive(MotorStage stage) {
        return MasterDataValidation.isActiveFlag(stage.getIsActive());
    }

    static String defaultMotorStageName(Integer motorStage) {
        if (motorStage == null) {
            return null;
        }
        return "Stage " + motorStage;
    }

    private void assertNoActiveProjectStage(String projectId, Integer motorStage, Long excludeStageId) {
        String pid = asTrimmedString(projectId);
        if (pid == null || motorStage == null) {
            return;
        }
        boolean conflict = repository.findAllByProjectIdAndMotorStage(pid, motorStage).stream()
                .filter(MotorStageMasterDataHandler::isActive)
                .anyMatch(existing -> excludeStageId == null
                        || !Objects.equals(existing.getStageId(), excludeStageId));
        if (conflict) {
            throw new AcemException(ErrorCode.DUPLICATE_MASTER_CODE,
                    "Motor stage already exists and is active for this project: " + motorStage);
        }
    }

    private static String requireProjectId(Object raw) {
        String projectId = asTrimmedString(raw);
        if (projectId == null) {
            throw new AcemException(ErrorCode.VALIDATION_ERROR, "Project is required");
        }
        return projectId;
    }

    private static String asTrimmedString(Object raw) {
        if (raw == null) {
            return null;
        }
        String value = String.valueOf(raw).trim();
        return value.isEmpty() ? null : value;
    }

    private long nextId() {
        return repository.findAll().stream()
                .map(MotorStage::getStageId)
                .filter(Objects::nonNull)
                .max(Long::compareTo)
                .orElse(0L) + 1L;
    }

    private MasterDataRecordDto toDto(MotorStage entity) {
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("projectId", entity.getProjectId());
        attrs.put("motorStage", entity.getMotorStage());
        String code = entity.getMotorStage() == null
                ? null
                : MasterDataCatalog.generatedCode(MasterDataCodePrefixes.MOTOR_STAGE, entity.getMotorStage());
        String name = entity.getMotorStageName() != null
                ? entity.getMotorStageName()
                : (entity.getMotorStage() == null ? null : defaultMotorStageName(entity.getMotorStage()));
        Long id = entity.getStageId();
        return MasterDataValidation.record(id, code, name, entity.getIsActive(), attrs, entity);
    }
}
