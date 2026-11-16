package in.co.bel.acempp.service.masterdata;

import in.co.bel.acempp.dto.masterdata.MasterDataCreateRequest;
import in.co.bel.acempp.dto.masterdata.MasterDataRecordDto;
import in.co.bel.acempp.dto.masterdata.MasterDataTypeDescriptor;
import in.co.bel.acempp.dto.masterdata.MasterDataUpdateRequest;
import in.co.bel.acempp.entity.batch.UnitMaster;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.UnitMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class UnitMasterDataHandler implements MasterDataTypeHandler {

    private final UnitMasterRepository repository;

    @Override
    public String type() {
        return MasterDataCatalog.UNITS;
    }

    @Override
    public List<MasterDataRecordDto> listAll() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(UnitMaster::getId, Comparator.nullsLast(Long::compareTo)))
                .map(this::toDto)
                .toList();
    }

    @Override
    public MasterDataRecordDto create(MasterDataCreateRequest request) {
        MasterDataTypeDescriptor schema = MasterDataCatalog.get(type());
        String name = MasterDataValidation.validateName(request.getName());
        MasterDataValidation.assertUniqueName(name, null, listAll());
        Map<String, Object> attrs = MasterDataValidation.validateAttributes(schema, request.getAttributes());
        String unitCode = MasterDataValidation.validateCode((String) attrs.get("unitCode"));
        MasterDataValidation.assertUniqueUnitCode(unitCode, null, listAll());

        long id = nextId();
        String code;
        while (true) {
            code = MasterDataCatalog.generatedCode(MasterDataCodePrefixes.UNIT, id);
            if (repository.findByCode(code).isEmpty()) {
                break;
            }
            id++;
        }

        UnitMaster entity = UnitMaster.builder()
                .id(id)
                .code(code)
                .name(name)
                .unitCode(unitCode)
                .symbol((String) attrs.get("symbol"))
                .description((String) attrs.get("description"))
                .isActive(request.getIsActive() == null || request.getIsActive())
                .build();
        MasterDataAuditSupport.stampOnCreate(entity);
        return toDto(repository.save(entity));
    }

    @Override
    public MasterDataRecordDto update(MasterDataUpdateRequest request) {
        MasterDataTypeDescriptor schema = MasterDataCatalog.get(type());
        UnitMaster entity = repository.findFirstById(request.getId())
                .orElseThrow(() -> new AcemException(ErrorCode.MASTER_DATA_NOT_FOUND, "Unit not found"));
        if (request.getCode() != null && !request.getCode().isBlank()
                && !Objects.equals(entity.getCode(), request.getCode().trim())) {
            throw new AcemException(ErrorCode.READONLY_FIELD, "Code cannot be changed");
        }
        Map<String, Object> attrs = MasterDataValidation.validateAttributes(schema, request.getAttributes());
        String unitCode = MasterDataValidation.validateCode((String) attrs.get("unitCode"));
        String name = MasterDataValidation.validateName(request.getName());
        MasterDataValidation.assertUniqueName(name, request.getId(), listAll());
        MasterDataValidation.assertUniqueUnitCode(unitCode, request.getId(), listAll());
        entity.setName(name);
        entity.setUnitCode(unitCode);
        entity.setSymbol((String) attrs.get("symbol"));
        entity.setDescription((String) attrs.get("description"));
        entity.setIsActive(Boolean.TRUE.equals(request.getIsActive()));
        MasterDataAuditSupport.stampOnUpdate(entity);
        return toDto(repository.save(entity));
    }

    @Override
    public MasterDataRecordDto softDisable(Long id) {
        UnitMaster entity = repository.findFirstById(id)
                .orElseThrow(() -> new AcemException(ErrorCode.MASTER_DATA_NOT_FOUND, "Unit not found"));
        entity.setIsActive(false);
        MasterDataAuditSupport.stampOnUpdate(entity);
        return toDto(repository.save(entity));
    }

    @Override
    public MasterDataRecordDto enable(Long id) {
        UnitMaster entity = repository.findFirstById(id)
                .orElseThrow(() -> new AcemException(ErrorCode.MASTER_DATA_NOT_FOUND, "Unit not found"));
        if (MasterDataValidation.isActiveFlag(entity.getIsActive())) {
            return toDto(entity);
        }
        entity.setIsActive(true);
        MasterDataAuditSupport.stampOnUpdate(entity);
        return toDto(repository.save(entity));
    }

    private long nextId() {
        return repository.findAll().stream()
                .map(UnitMaster::getId)
                .filter(Objects::nonNull)
                .max(Long::compareTo)
                .orElse(0L) + 1L;
    }

    private MasterDataRecordDto toDto(UnitMaster entity) {
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("unitCode", entity.getUnitCode());
        attrs.put("symbol", entity.getSymbol());
        attrs.put("description", entity.getDescription());
        return MasterDataValidation.record(entity.getId(), entity.getCode(), entity.getName(),
                entity.getIsActive(), attrs, entity);
    }
}
