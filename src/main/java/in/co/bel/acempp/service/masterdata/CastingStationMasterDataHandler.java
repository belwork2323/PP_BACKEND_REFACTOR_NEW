package in.co.bel.acempp.service.masterdata;

import in.co.bel.acempp.dto.masterdata.MasterDataCreateRequest;
import in.co.bel.acempp.dto.masterdata.MasterDataRecordDto;
import in.co.bel.acempp.dto.masterdata.MasterDataTypeDescriptor;
import in.co.bel.acempp.dto.masterdata.MasterDataUpdateRequest;
import in.co.bel.acempp.entity.CastingStation;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.CastingStationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class CastingStationMasterDataHandler implements MasterDataTypeHandler {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";

    private final CastingStationRepository repository;

    @Override
    public String type() {
        return MasterDataCatalog.CASTING_STATIONS;
    }

    @Override
    public List<MasterDataRecordDto> listAll() {
        return repository.findAllByOrderByStationIdAsc().stream()
                .sorted(Comparator.comparing(CastingStation::getStationId, Comparator.nullsLast(Long::compareTo)))
                .map(this::toDto)
                .toList();
    }

    @Override
    public MasterDataRecordDto create(MasterDataCreateRequest request) {
        MasterDataTypeDescriptor schema = MasterDataCatalog.get(type());
        String name = MasterDataValidation.validateName(request.getName());
        MasterDataValidation.assertUniqueName(name, null, listAll());
        Map<String, Object> attrs = MasterDataValidation.validateAttributes(schema, request.getAttributes());
        boolean active = request.getIsActive() == null || request.getIsActive();

        long id = nextId();
        String code;
        while (true) {
            code = MasterDataCatalog.generatedCode(MasterDataCodePrefixes.CASTING_STATION, id);
            if (repository.findByStationCode(code).isEmpty()) {
                break;
            }
            id++;
        }

        CastingStation entity = CastingStation.builder()
                .stationId(id)
                .stationCode(code)
                .stationName(name)
                .stationType((String) attrs.get("stationType"))
                .status(active ? STATUS_ACTIVE : STATUS_INACTIVE)
                .build();
        MasterDataAuditSupport.stampOnCreate(entity);
        return toDto(repository.save(entity));
    }

    @Override
    public MasterDataRecordDto update(MasterDataUpdateRequest request) {
        MasterDataTypeDescriptor schema = MasterDataCatalog.get(type());
        CastingStation entity = repository.findByStationId(request.getId())
                .orElseThrow(() -> new AcemException(ErrorCode.MASTER_DATA_NOT_FOUND, "Casting station not found"));
        if (request.getCode() != null && !request.getCode().isBlank()
                && !Objects.equals(entity.getStationCode(), request.getCode().trim())) {
            throw new AcemException(ErrorCode.READONLY_FIELD, "Code cannot be changed");
        }
        Map<String, Object> attrs = MasterDataValidation.validateAttributes(schema, request.getAttributes());
        String name = MasterDataValidation.validateName(request.getName());
        MasterDataValidation.assertUniqueName(name, request.getId(), listAll());
        entity.setStationName(name);
        entity.setStationType((String) attrs.get("stationType"));
        entity.setStatus(Boolean.TRUE.equals(request.getIsActive()) ? STATUS_ACTIVE : STATUS_INACTIVE);
        MasterDataAuditSupport.stampOnUpdate(entity);
        return toDto(repository.save(entity));
    }

    @Override
    public MasterDataRecordDto softDisable(Long id) {
        CastingStation entity = repository.findByStationId(id)
                .orElseThrow(() -> new AcemException(ErrorCode.MASTER_DATA_NOT_FOUND, "Casting station not found"));
        entity.setStatus(STATUS_INACTIVE);
        MasterDataAuditSupport.stampOnUpdate(entity);
        return toDto(repository.save(entity));
    }

    @Override
    public MasterDataRecordDto enable(Long id) {
        CastingStation entity = repository.findByStationId(id)
                .orElseThrow(() -> new AcemException(ErrorCode.MASTER_DATA_NOT_FOUND, "Casting station not found"));
        if (STATUS_ACTIVE.equalsIgnoreCase(entity.getStatus())) {
            return toDto(entity);
        }
        entity.setStatus(STATUS_ACTIVE);
        MasterDataAuditSupport.stampOnUpdate(entity);
        return toDto(repository.save(entity));
    }


    private long nextId() {
        return repository.findAll().stream()
                .map(CastingStation::getStationId)
                .filter(Objects::nonNull)
                .max(Long::compareTo)
                .orElse(0L) + 1L;
    }

    private MasterDataRecordDto toDto(CastingStation entity) {
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("stationType", entity.getStationType());
        boolean active = STATUS_ACTIVE.equalsIgnoreCase(entity.getStatus());
        return MasterDataValidation.record(entity.getStationId(), entity.getStationCode(),
                entity.getStationName(), active, attrs, entity);
    }
}
