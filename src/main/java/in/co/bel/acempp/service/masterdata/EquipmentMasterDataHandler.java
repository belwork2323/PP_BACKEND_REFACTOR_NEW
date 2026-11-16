package in.co.bel.acempp.service.masterdata;

import in.co.bel.acempp.dto.masterdata.MasterDataCreateRequest;
import in.co.bel.acempp.dto.masterdata.MasterDataRecordDto;
import in.co.bel.acempp.dto.masterdata.MasterDataTypeDescriptor;
import in.co.bel.acempp.dto.masterdata.MasterDataUpdateRequest;
import in.co.bel.acempp.entity.EquipmentMaster;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.EquipmentRepository;
import in.co.bel.acempp.repository.EquipmentTypeMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class EquipmentMasterDataHandler implements MasterDataTypeHandler {

    private final EquipmentRepository repository;
    private final EquipmentTypeMasterRepository equipmentTypeRepository;

    @Override
    public String type() {
        return MasterDataCatalog.EQUIPMENT;
    }

    @Override
    public List<MasterDataRecordDto> listAll() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(EquipmentMaster::getEquipmentId, Comparator.nullsLast(Long::compareTo)))
                .map(this::toDto)
                .toList();
    }

    @Override
    public MasterDataRecordDto create(MasterDataCreateRequest request) {
        MasterDataTypeDescriptor schema = MasterDataCatalog.get(type());
        String name = MasterDataValidation.validateName(request.getName());
        MasterDataValidation.assertUniqueName(name, null, listAll());
        Map<String, Object> attrs = MasterDataValidation.validateAttributes(schema, request.getAttributes());
        String equipmentType = MasterDataValidation.validateEquipmentType(
                (String) attrs.get("equipmentType"), equipmentTypeRepository);

        long id = nextId();
        String code;
        while (true) {
            code = MasterDataCatalog.generatedCode(MasterDataCodePrefixes.EQUIPMENT, id);
            if (repository.findByEquipmentCode(code).isEmpty()) {
                break;
            }
            id++;
        }

        EquipmentMaster entity = EquipmentMaster.builder()
                .equipmentId(id)
                .equipmentCode(code)
                .equipmentName(name)
                .equipmentType(equipmentType)
                .isActive(request.getIsActive() == null || request.getIsActive())
                .build();
        MasterDataAuditSupport.stampOnCreate(entity);
        return toDto(repository.save(entity));
    }

    @Override
    public MasterDataRecordDto update(MasterDataUpdateRequest request) {
        MasterDataTypeDescriptor schema = MasterDataCatalog.get(type());
        EquipmentMaster entity = repository.findByEquipmentId(request.getId())
                .orElseThrow(() -> new AcemException(ErrorCode.MASTER_DATA_NOT_FOUND, "Equipment not found"));
        if (request.getCode() != null && !request.getCode().isBlank()
                && !Objects.equals(entity.getEquipmentCode(), request.getCode().trim())) {
            throw new AcemException(ErrorCode.READONLY_FIELD, "Code cannot be changed");
        }
        Map<String, Object> attrs = MasterDataValidation.validateAttributes(schema, request.getAttributes());
        String equipmentType = MasterDataValidation.validateEquipmentType(
                (String) attrs.get("equipmentType"), equipmentTypeRepository);
        String name = MasterDataValidation.validateName(request.getName());
        MasterDataValidation.assertUniqueName(name, request.getId(), listAll());
        entity.setEquipmentName(name);
        entity.setEquipmentType(equipmentType);
        entity.setIsActive(Boolean.TRUE.equals(request.getIsActive()));
        MasterDataAuditSupport.stampOnUpdate(entity);
        return toDto(repository.save(entity));
    }

    @Override
    public MasterDataRecordDto softDisable(Long id) {
        EquipmentMaster entity = repository.findByEquipmentId(id)
                .orElseThrow(() -> new AcemException(ErrorCode.MASTER_DATA_NOT_FOUND, "Equipment not found"));
        entity.setIsActive(false);
        MasterDataAuditSupport.stampOnUpdate(entity);
        return toDto(repository.save(entity));
    }

    @Override
    public MasterDataRecordDto enable(Long id) {
        EquipmentMaster entity = repository.findByEquipmentId(id)
                .orElseThrow(() -> new AcemException(ErrorCode.MASTER_DATA_NOT_FOUND, "Equipment not found"));
        if (MasterDataValidation.isActiveFlag(entity.getIsActive())) {
            return toDto(entity);
        }
        entity.setIsActive(true);
        MasterDataAuditSupport.stampOnUpdate(entity);
        return toDto(repository.save(entity));
    }


    private long nextId() {
        return repository.findAll().stream()
                .map(EquipmentMaster::getEquipmentId)
                .filter(Objects::nonNull)
                .max(Long::compareTo)
                .orElse(0L) + 1L;
    }

    private MasterDataRecordDto toDto(EquipmentMaster entity) {
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("equipmentType", entity.getEquipmentType());
        return MasterDataValidation.record(entity.getEquipmentId(), entity.getEquipmentCode(),
                entity.getEquipmentName(), entity.getIsActive(), attrs, entity);
    }
}
