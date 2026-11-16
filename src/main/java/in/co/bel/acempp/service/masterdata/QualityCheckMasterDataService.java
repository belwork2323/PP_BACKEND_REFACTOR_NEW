package in.co.bel.acempp.service.masterdata;

import in.co.bel.acempp.dto.masterdata.MasterDataStatsDto;
import in.co.bel.acempp.dto.masterdata.qualitychecks.*;
import in.co.bel.acempp.dto.mixing.qualitychecks.QualityCheckParams;
import in.co.bel.acempp.entity.manufacturing.QualityCheckMaster;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.manufacturing.QualityCheckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class QualityCheckMasterDataService {

    private static final Set<String> ALLOWED_MIX_TYPES = Set.of("PREMIX", "FINAL_MIX");

    private final QualityCheckRepository repository;
    private final QualityCheckParameterMapper qualityCheckParameterMapper;

    public static boolean isActive(QualityCheckMaster entity) {
        return MasterDataValidation.isActiveFlag(entity.getIsActive());
    }

    public static boolean isParameterActive(QualityCheckParams parameter) {
        return QualityCheckParameterMapper.isParameterActive(parameter);
    }

    public static List<QualityCheckParams> activeParameters(QualityCheckMaster entity) {
        if (entity == null || entity.getQualityChecks() == null) {
            return List.of();
        }
        return QualityCheckParameterMapper.activeParameters(entity.getQualityChecks());
    }

    public QualityCheckMasterListResponse list(QualityCheckListRequest request) {
        QualityCheckListRequest req = request == null ? new QualityCheckListRequest() : request;
        List<QualityCheckMaster> all = repository.findAll().stream()
                .sorted(MasterDataListOrdering.byLongIdDesc(QualityCheckMaster::getId))
                .toList();

        long active = all.stream().filter(QualityCheckMasterDataService::isActive).count();
        long inactive = all.size() - active;

        List<QualityCheckMasterRecordDto> items = all.stream()
                .filter(e -> matchesActive(e, req.getIsActive()))
                .filter(e -> req.getMixType() == null || req.getMixType().isBlank()
                        || Objects.equals(e.getMixType(), req.getMixType()))
                .filter(e -> req.getMotorStage() == null || Objects.equals(e.getMotorStage(), req.getMotorStage()))
                .filter(e -> matchesSearch(e, req.getSearch()))
                .map(this::toDto)
                .toList();

        return QualityCheckMasterListResponse.builder()
                .items(items)
                .stats(MasterDataStatsDto.builder()
                        .total(all.size())
                        .active(active)
                        .inactive(inactive)
                        .build())
                .build();
    }

    public QualityCheckMasterRecordDto create(QualityCheckMasterCreateRequest request) {
        String mixType = validateMixType(request.getMixType());
        if (repository.findByMixTypeAndMotorStage(mixType, request.getMotorStage()).isPresent()) {
            throw new AcemException(ErrorCode.DUPLICATE_MASTER_CODE,
                    "Quality checks already exist for mixType " + mixType
                            + " and motorStage " + request.getMotorStage());
        }

        long id = nextId();
        String code;
        while (true) {
            code = MasterDataCatalog.generatedCode(MasterDataCodePrefixes.QUALITY_CHECK, id);
            if (repository.findByQualityCheckCode(code).isEmpty()) {
                break;
            }
            id++;
        }

        QualityCheckMaster entity = QualityCheckMaster.builder()
                .id(id)
                .qualityCheckCode(code)
                .mixType(mixType)
                .motorStage(request.getMotorStage())
                .qualityChecks(qualityCheckParameterMapper.mapChecks(request.getQualityChecks(), true))
                .isActive(request.getIsActive() == null || request.getIsActive())
                .build();
        MasterDataAuditSupport.stampOnCreate(entity);
        return toDto(repository.save(entity));
    }

    public QualityCheckMasterRecordDto update(QualityCheckMasterUpdateRequest request) {
        QualityCheckMaster entity = repository.findById(request.getId())
                .orElseThrow(() -> new AcemException(ErrorCode.MASTER_DATA_NOT_FOUND, "Quality check master not found"));
        if (request.getMixType() != null && !request.getMixType().isBlank()
                && !Objects.equals(entity.getMixType(), validateMixType(request.getMixType()))) {
            throw new AcemException(ErrorCode.READONLY_FIELD, "Mix type cannot be changed");
        }
        if (request.getMotorStage() != null && !Objects.equals(entity.getMotorStage(), request.getMotorStage())) {
            throw new AcemException(ErrorCode.READONLY_FIELD, "Motor stage cannot be changed");
        }
        entity.setQualityChecks(qualityCheckParameterMapper.mapChecksForUpdate(
                request.getQualityChecks(), entity.getQualityChecks(), true));
        entity.setIsActive(Boolean.TRUE.equals(request.getIsActive()));
        MasterDataAuditSupport.stampOnUpdate(entity);
        return toDto(repository.save(entity));
    }

    public QualityCheckMasterRecordDto softDisable(QualityCheckMasterDeleteRequest request) {
        QualityCheckMaster entity = repository.findById(request.getId())
                .orElseThrow(() -> new AcemException(ErrorCode.MASTER_DATA_NOT_FOUND, "Quality check master not found"));
        entity.setIsActive(false);
        MasterDataAuditSupport.stampOnUpdate(entity);
        return toDto(repository.save(entity));
    }

    public QualityCheckMasterRecordDto enable(QualityCheckMasterDeleteRequest request) {
        QualityCheckMaster entity = repository.findById(request.getId())
                .orElseThrow(() -> new AcemException(ErrorCode.MASTER_DATA_NOT_FOUND, "Quality check master not found"));
        entity.setIsActive(true);
        MasterDataAuditSupport.stampOnUpdate(entity);
        return toDto(repository.save(entity));
    }

    private static String validateMixType(String mixType) {
        String normalized = MasterDataValidation.requireTrimmed(mixType, "Mix type").toUpperCase(Locale.ROOT);
        if (!ALLOWED_MIX_TYPES.contains(normalized)) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "Mix type must be one of: PREMIX, FINAL_MIX");
        }
        return normalized;
    }

    private QualityCheckMasterRecordDto toDto(QualityCheckMaster entity) {
        List<QualityCheckParams> checks = entity.getQualityChecks() == null
                ? List.of()
                : entity.getQualityChecks().stream()
                .map(QualityCheckParameterMapper::toParameterDto)
                .toList();
        return QualityCheckMasterRecordDto.builder()
                .id(entity.getId())
                .qualityCheckCode(entity.getQualityCheckCode())
                .mixType(entity.getMixType())
                .motorStage(entity.getMotorStage())
                .isActive(isActive(entity))
                .qualityChecks(checks)
                .createdBy(MasterDataValidation.auditCreatedBy(entity))
                .createdOn(MasterDataValidation.auditCreatedOn(entity))
                .updatedBy(MasterDataValidation.auditUpdatedBy(entity))
                .updatedOn(MasterDataValidation.auditUpdatedOn(entity))
                .build();
    }

    private boolean matchesActive(QualityCheckMaster e, Boolean filter) {
        if (filter == null) return true;
        return filter == isActive(e);
    }

    private boolean matchesSearch(QualityCheckMaster e, String search) {
        if (search == null || search.isBlank()) return true;
        String q = search.trim().toLowerCase(Locale.ROOT);
        return (e.getQualityCheckCode() != null && e.getQualityCheckCode().toLowerCase(Locale.ROOT).contains(q))
                || (e.getMixType() != null && e.getMixType().toLowerCase(Locale.ROOT).contains(q))
                || (e.getMotorStage() != null && String.valueOf(e.getMotorStage()).contains(q));
    }

    private long nextId() {
        return repository.findAll().stream()
                .map(QualityCheckMaster::getId)
                .filter(Objects::nonNull)
                .max(Long::compareTo)
                .orElse(0L) + 1L;
    }
}
