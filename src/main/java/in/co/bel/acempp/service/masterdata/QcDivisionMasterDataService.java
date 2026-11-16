package in.co.bel.acempp.service.masterdata;

import in.co.bel.acempp.dto.masterdata.MasterDataStatsDto;
import in.co.bel.acempp.dto.masterdata.qcdivisions.*;
import in.co.bel.acempp.entity.qualitycontrol.QcDivisionMaster;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.qualitycontrol.QcDivisionMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class QcDivisionMasterDataService {

    private final QcDivisionMasterRepository repository;

    public static boolean isActive(QcDivisionMaster entity) {
        return !Boolean.TRUE.equals(entity.getDisabled());
    }

    public static boolean isTypeActive(QcDivisionMaster.DivisionType type) {
        return type == null || !Boolean.TRUE.equals(type.getDisabled());
    }

    public QcDivisionMasterListResponse list(QcDivisionMasterListRequest request) {
        QcDivisionMasterListRequest req = request == null ? new QcDivisionMasterListRequest() : request;
        List<QcDivisionMaster> all = repository.findAll().stream()
                .sorted(MasterDataListOrdering.byLongIdDesc(QcDivisionMaster::getDivisionId))
                .toList();

        long active = all.stream().filter(QcDivisionMasterDataService::isActive).count();
        long inactive = all.size() - active;

        List<QcDivisionMasterRecordDto> items = all.stream()
                .filter(e -> matchesActive(e, req.getIsActive()))
                .filter(e -> matchesSearch(e, req.getSearch()))
                .map(this::toDto)
                .toList();

        return QcDivisionMasterListResponse.builder()
                .items(items)
                .stats(MasterDataStatsDto.builder()
                        .total(all.size())
                        .active(active)
                        .inactive(inactive)
                        .build())
                .build();
    }

    public QcDivisionMasterRecordDto create(QcDivisionMasterCreateRequest request) {
        long divisionId = request.getDivisionId() == null ? nextDivisionId() : request.getDivisionId();
        if (repository.findByDivisionId(divisionId).isPresent()) {
            throw new AcemException(ErrorCode.DUPLICATE_MASTER_CODE,
                    "QC division already exists: " + divisionId);
        }
        QcDivisionMaster entity = QcDivisionMaster.builder()
                .divisionId(divisionId)
                .divisionName(MasterDataValidation.validateName(request.getDivisionName()))
                .displayOrder(request.getDisplayOrder() == null ? (int) divisionId : request.getDisplayOrder())
                .disabled(!(request.getIsActive() == null || request.getIsActive()))
                .types(mapTypes(request.getTypes(), divisionId))
                .build();
        MasterDataAuditSupport.stampOnCreate(entity);
        return toDto(repository.save(entity));
    }

    public QcDivisionMasterRecordDto update(QcDivisionMasterUpdateRequest request) {
        QcDivisionMaster entity = repository.findById(request.getId())
                .orElseThrow(() -> new AcemException(ErrorCode.MASTER_DATA_NOT_FOUND, "QC division not found"));
        if (request.getDivisionId() != null && !Objects.equals(entity.getDivisionId(), request.getDivisionId())) {
            throw new AcemException(ErrorCode.READONLY_FIELD, "Division ID cannot be changed");
        }
        entity.setDivisionName(MasterDataValidation.validateName(request.getDivisionName()));
        if (request.getDisplayOrder() != null) {
            entity.setDisplayOrder(request.getDisplayOrder());
        }
        entity.setDisabled(!Boolean.TRUE.equals(request.getIsActive()));
        entity.setTypes(mapTypes(request.getTypes(), entity.getDivisionId()));
        MasterDataAuditSupport.stampOnUpdate(entity);
        return toDto(repository.save(entity));
    }

    public QcDivisionMasterRecordDto softDisable(QcDivisionMasterDeleteRequest request) {
        QcDivisionMaster entity = repository.findById(request.getId())
                .orElseThrow(() -> new AcemException(ErrorCode.MASTER_DATA_NOT_FOUND, "QC division not found"));
        entity.setDisabled(true);
        if (entity.getTypes() != null) {
            entity.getTypes().forEach(t -> t.setDisabled(true));
        }
        MasterDataAuditSupport.stampOnUpdate(entity);
        return toDto(repository.save(entity));
    }

    private List<QcDivisionMaster.DivisionType> mapTypes(
            List<QcDivisionMasterCreateRequest.DivisionTypeWriteDto> types,
            long parentDivisionId) {
        if (types == null || types.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> ids = new HashSet<>();
        ids.add(parentDivisionId);
        long nextId = nextDivisionId();
        List<QcDivisionMaster.DivisionType> result = new ArrayList<>();
        int order = 1;
        for (QcDivisionMasterCreateRequest.DivisionTypeWriteDto t : types) {
            Long typeId = t.getDivisionId();
            if (typeId == null) {
                while (ids.contains(nextId) || repository.findByDivisionOrTypeId(nextId).isPresent()) {
                    nextId++;
                }
                typeId = nextId++;
            }
            if (!ids.add(typeId)) {
                throw new AcemException(ErrorCode.DUPLICATE_MASTER_CODE, "Duplicate divisionId: " + typeId);
            }
            result.add(QcDivisionMaster.DivisionType.builder()
                    .divisionId(typeId)
                    .divisionName(MasterDataValidation.validateName(t.getDivisionName()))
                    .displayOrder(t.getDisplayOrder() == null ? order++ : t.getDisplayOrder())
                    .disabled(!(t.getIsActive() == null || t.getIsActive()))
                    .build());
        }
        return result;
    }

    private QcDivisionMasterRecordDto toDto(QcDivisionMaster entity) {
        List<QcDivisionMasterRecordDto.DivisionTypeDto> types = entity.getTypes() == null
                ? List.of()
                : entity.getTypes().stream()
                .map(t -> QcDivisionMasterRecordDto.DivisionTypeDto.builder()
                        .divisionId(t.getDivisionId())
                        .divisionName(t.getDivisionName())
                        .displayOrder(t.getDisplayOrder())
                        .isActive(isTypeActive(t))
                        .build())
                .toList();
        return QcDivisionMasterRecordDto.builder()
                .id(entity.getId())
                .divisionId(entity.getDivisionId())
                .divisionName(entity.getDivisionName())
                .displayOrder(entity.getDisplayOrder())
                .isActive(isActive(entity))
                .types(types)
                .createdBy(MasterDataValidation.auditCreatedBy(entity))
                .createdOn(MasterDataValidation.auditCreatedOn(entity))
                .updatedBy(MasterDataValidation.auditUpdatedBy(entity))
                .updatedOn(MasterDataValidation.auditUpdatedOn(entity))
                .build();
    }

    private boolean matchesActive(QcDivisionMaster e, Boolean filter) {
        if (filter == null) return true;
        return filter == isActive(e);
    }

    private boolean matchesSearch(QcDivisionMaster e, String search) {
        if (search == null || search.isBlank()) return true;
        String q = search.trim().toLowerCase(Locale.ROOT);
        if (e.getDivisionName() != null && e.getDivisionName().toLowerCase(Locale.ROOT).contains(q)) {
            return true;
        }
        if (e.getDivisionId() != null && String.valueOf(e.getDivisionId()).contains(q)) {
            return true;
        }
        return e.getTypes() != null && e.getTypes().stream()
                .anyMatch(t -> t.getDivisionName() != null
                        && t.getDivisionName().toLowerCase(Locale.ROOT).contains(q));
    }

    private long nextDivisionId() {
        long max = 0L;
        for (QcDivisionMaster d : repository.findAll()) {
            if (d.getDivisionId() != null) {
                max = Math.max(max, d.getDivisionId());
            }
            if (d.getTypes() != null) {
                for (QcDivisionMaster.DivisionType t : d.getTypes()) {
                    if (t.getDivisionId() != null) {
                        max = Math.max(max, t.getDivisionId());
                    }
                }
            }
        }
        return max + 1L;
    }
}
