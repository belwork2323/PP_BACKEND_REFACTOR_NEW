package in.co.bel.acempp.service.masterdata;

import in.co.bel.acempp.dto.masterdata.MasterDataCreateRequest;
import in.co.bel.acempp.dto.masterdata.MasterDataRecordDto;
import in.co.bel.acempp.dto.masterdata.MasterDataTypeDescriptor;
import in.co.bel.acempp.dto.masterdata.MasterDataUpdateRequest;
import in.co.bel.acempp.entity.batch.MixerMaster;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.MixerMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class MixerMasterDataHandler implements MasterDataTypeHandler {

    private final MixerMasterRepository repository;

    @Override
    public String type() {
        return MasterDataCatalog.MIXERS;
    }

    @Override
    public List<MasterDataRecordDto> listAll() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(MixerMaster::getId, Comparator.nullsLast(Long::compareTo)))
                .map(this::toDto)
                .toList();
    }

    @Override
    public MasterDataRecordDto create(MasterDataCreateRequest request) {
        MasterDataTypeDescriptor schema = MasterDataCatalog.get(type());
        String name = MasterDataValidation.validateName(request.getName());
        MasterDataValidation.assertUniqueName(name, null, listAll());
        MasterDataValidation.validateAttributes(schema, request.getAttributes());

        long id = nextId();
        String code;
        while (true) {
            code = MasterDataCatalog.generatedCode(MasterDataCodePrefixes.MIXER, id);
            if (repository.findByCode(code).isEmpty()) {
                break;
            }
            id++;
        }

        MixerMaster entity = MixerMaster.builder()
                .id(id)
                .code(code)
                .name(name)
                .isActive(request.getIsActive() == null || request.getIsActive())
                .build();
        MasterDataAuditSupport.stampOnCreate(entity);
        return toDto(repository.save(entity));
    }

    @Override
    public MasterDataRecordDto update(MasterDataUpdateRequest request) {
        MasterDataTypeDescriptor schema = MasterDataCatalog.get(type());
        MixerMaster entity = repository.findFirstById(request.getId())
                .orElseThrow(() -> new AcemException(ErrorCode.MASTER_DATA_NOT_FOUND, "Mixer not found"));
        if (request.getCode() != null && !request.getCode().isBlank()
                && !Objects.equals(entity.getCode(), request.getCode().trim())) {
            throw new AcemException(ErrorCode.READONLY_FIELD, "Code cannot be changed");
        }
        String name = MasterDataValidation.validateName(request.getName());
        MasterDataValidation.assertUniqueName(name, request.getId(), listAll());
        entity.setName(name);
        entity.setIsActive(Boolean.TRUE.equals(request.getIsActive()));
        MasterDataValidation.validateAttributes(schema, request.getAttributes());
        MasterDataAuditSupport.stampOnUpdate(entity);
        return toDto(repository.save(entity));
    }

    @Override
    public MasterDataRecordDto softDisable(Long id) {
        MixerMaster entity = repository.findFirstById(id)
                .orElseThrow(() -> new AcemException(ErrorCode.MASTER_DATA_NOT_FOUND, "Mixer not found"));
        entity.setIsActive(false);
        MasterDataAuditSupport.stampOnUpdate(entity);
        return toDto(repository.save(entity));
    }

    @Override
    public MasterDataRecordDto enable(Long id) {
        MixerMaster entity = repository.findFirstById(id)
                .orElseThrow(() -> new AcemException(ErrorCode.MASTER_DATA_NOT_FOUND, "Mixer not found"));
        if (MasterDataValidation.isActiveFlag(entity.getIsActive())) {
            return toDto(entity);
        }
        entity.setIsActive(true);
        MasterDataAuditSupport.stampOnUpdate(entity);
        return toDto(repository.save(entity));
    }

    private long nextId() {
        return repository.findAll().stream()
                .map(MixerMaster::getId)
                .filter(Objects::nonNull)
                .max(Long::compareTo)
                .orElse(0L) + 1L;
    }

    private MasterDataRecordDto toDto(MixerMaster entity) {
        return MasterDataValidation.record(entity.getId(), entity.getCode(), entity.getName(),
                entity.getIsActive(), Map.of(), entity);
    }
}
