package in.co.bel.acempp.service.masterdata;

import in.co.bel.acempp.dto.masterdata.MasterDataCreateRequest;
import in.co.bel.acempp.dto.masterdata.MasterDataRecordDto;
import in.co.bel.acempp.dto.masterdata.MasterDataTypeDescriptor;
import in.co.bel.acempp.dto.masterdata.MasterDataUpdateRequest;
import in.co.bel.acempp.entity.BeamEnergy;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.BeamEnergyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class BeamEnergyMasterDataHandler implements MasterDataTypeHandler {

    private final BeamEnergyRepository repository;

    @Override
    public String type() {
        return MasterDataCatalog.BEAM_ENERGY;
    }

    @Override
    public List<MasterDataRecordDto> listAll() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(BeamEnergy::getBeamEnergyId, Comparator.nullsLast(Long::compareTo)))
                .map(this::toDto)
                .toList();
    }

    @Override
    public MasterDataRecordDto create(MasterDataCreateRequest request) {
        MasterDataTypeDescriptor schema = MasterDataCatalog.get(type());
        String name = MasterDataValidation.validateName(request.getName());
        MasterDataValidation.assertUniqueName(name, null, listAll());
        Map<String, Object> attrs = MasterDataValidation.validateAttributes(schema, request.getAttributes());

        long id = nextId();
        String code;
        while (true) {
            code = MasterDataCatalog.generatedCode(MasterDataCodePrefixes.BEAM_ENERGY, id);
            if (repository.findByBeamEnergyCode(code).isEmpty()) {
                break;
            }
            id++;
        }

        BeamEnergy entity = BeamEnergy.builder()
                .beamEnergyId(id)
                .beamEnergyCode(code)
                .beamEnergyName(name)
                .energyUnit((String) attrs.get("energyUnit"))
                .isActive(request.getIsActive() == null || request.getIsActive())
                .build();
        MasterDataAuditSupport.stampOnCreate(entity);
        return toDto(repository.save(entity));
    }

    @Override
    public MasterDataRecordDto update(MasterDataUpdateRequest request) {
        MasterDataTypeDescriptor schema = MasterDataCatalog.get(type());
        BeamEnergy entity = repository.findByBeamEnergyId(request.getId())
                .orElseThrow(() -> new AcemException(ErrorCode.MASTER_DATA_NOT_FOUND, "Beam energy not found"));
        if (request.getCode() != null && !request.getCode().isBlank()
                && !Objects.equals(entity.getBeamEnergyCode(), request.getCode().trim())) {
            throw new AcemException(ErrorCode.READONLY_FIELD, "Code cannot be changed");
        }
        Map<String, Object> attrs = MasterDataValidation.validateAttributes(schema, request.getAttributes());
        String name = MasterDataValidation.validateName(request.getName());
        MasterDataValidation.assertUniqueName(name, request.getId(), listAll());
        entity.setBeamEnergyName(name);
        entity.setEnergyUnit((String) attrs.get("energyUnit"));
        entity.setIsActive(Boolean.TRUE.equals(request.getIsActive()));
        MasterDataAuditSupport.stampOnUpdate(entity);
        return toDto(repository.save(entity));
    }

    @Override
    public MasterDataRecordDto softDisable(Long id) {
        BeamEnergy entity = repository.findByBeamEnergyId(id)
                .orElseThrow(() -> new AcemException(ErrorCode.MASTER_DATA_NOT_FOUND, "Beam energy not found"));
        entity.setIsActive(false);
        MasterDataAuditSupport.stampOnUpdate(entity);
        return toDto(repository.save(entity));
    }

    @Override
    public MasterDataRecordDto enable(Long id) {
        BeamEnergy entity = repository.findByBeamEnergyId(id)
                .orElseThrow(() -> new AcemException(ErrorCode.MASTER_DATA_NOT_FOUND, "Beam energy not found"));
        if (MasterDataValidation.isActiveFlag(entity.getIsActive())) {
            return toDto(entity);
        }
        entity.setIsActive(true);
        MasterDataAuditSupport.stampOnUpdate(entity);
        return toDto(repository.save(entity));
    }


    private long nextId() {
        return repository.findAll().stream()
                .map(BeamEnergy::getBeamEnergyId)
                .filter(Objects::nonNull)
                .max(Long::compareTo)
                .orElse(0L) + 1L;
    }

    private MasterDataRecordDto toDto(BeamEnergy entity) {
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("energyUnit", entity.getEnergyUnit());
        return MasterDataValidation.record(entity.getBeamEnergyId(), entity.getBeamEnergyCode(),
                entity.getBeamEnergyName(), entity.getIsActive(), attrs, entity);
    }
}
