package in.co.bel.acempp.service.masterdata;

import in.co.bel.acempp.dto.masterdata.MasterDataCreateRequest;
import in.co.bel.acempp.dto.masterdata.MasterDataRecordDto;
import in.co.bel.acempp.dto.masterdata.MasterDataTypeDescriptor;
import in.co.bel.acempp.dto.masterdata.MasterDataUpdateRequest;
import in.co.bel.acempp.entity.batch.SubscaleArticleMaster;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.SubscaleArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class SubscaleArticleMasterDataHandler implements MasterDataTypeHandler {

    public static final String DEFAULT_BEM_UNIT = "Kg";

    private final SubscaleArticleRepository repository;

    @Override
    public String type() {
        return MasterDataCatalog.SUBSCALE_ARTICLES;
    }

    @Override
    public List<MasterDataRecordDto> listAll() {
        return repository.findAllByOrderByDisplayOrderAscSubscaleArticleNameAsc().stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public MasterDataRecordDto create(MasterDataCreateRequest request) {
        MasterDataTypeDescriptor schema = MasterDataCatalog.get(type());
        Map<String, Object> attrs = MasterDataValidation.validateAttributes(schema, request.getAttributes());
        ResolvedArticle resolved = resolveArticleFields(request.getName(), attrs);

        MasterDataValidation.assertUniqueName(resolved.name(), null, listAll());

        long id = nextId();
        String code;
        while (true) {
            code = MasterDataCatalog.generatedCode(MasterDataCodePrefixes.SUBSCALE_ARTICLE, id);
            if (repository.findBySubscaleArticleCode(code).isEmpty()) {
                break;
            }
            id++;
        }

        SubscaleArticleMaster entity = SubscaleArticleMaster.builder()
                .subscaleArticleId(id)
                .subscaleArticleCode(code)
                .subscaleArticleName(resolved.name())
                .isBem(resolved.isBem())
                .bemSize(resolved.bemSize())
                .bemUnit(resolved.bemUnit())
                .displayOrder(nextDisplayOrder())
                .isActive(request.getIsActive() == null || request.getIsActive())
                .build();
        MasterDataAuditSupport.stampOnCreate(entity);
        return toDto(repository.save(entity));
    }

    @Override
    public MasterDataRecordDto update(MasterDataUpdateRequest request) {
        MasterDataTypeDescriptor schema = MasterDataCatalog.get(type());
        SubscaleArticleMaster entity = findByBusinessId(request.getId());
        if (request.getCode() != null && !request.getCode().isBlank()
                && !Objects.equals(entity.getSubscaleArticleCode(), request.getCode().trim())) {
            throw new AcemException(ErrorCode.READONLY_FIELD, "Code cannot be changed");
        }
        Map<String, Object> attrs = MasterDataValidation.validateAttributes(schema, request.getAttributes());
        ResolvedArticle resolved = resolveArticleFields(request.getName(), attrs);

        MasterDataValidation.assertUniqueName(resolved.name(), request.getId(), listAll());
        entity.setSubscaleArticleName(resolved.name());
        entity.setIsBem(resolved.isBem());
        entity.setBemSize(resolved.bemSize());
        entity.setBemUnit(resolved.bemUnit());
        entity.setIsActive(Boolean.TRUE.equals(request.getIsActive()));
        MasterDataAuditSupport.stampOnUpdate(entity);
        return toDto(repository.save(entity));
    }

    @Override
    public MasterDataRecordDto softDisable(Long id) {
        SubscaleArticleMaster entity = findByBusinessId(id);
        entity.setIsActive(false);
        MasterDataAuditSupport.stampOnUpdate(entity);
        return toDto(repository.save(entity));
    }

    @Override
    public MasterDataRecordDto enable(Long id) {
        SubscaleArticleMaster entity = findByBusinessId(id);
        if (MasterDataValidation.isActiveFlag(entity.getIsActive())) {
            return toDto(entity);
        }
        entity.setIsActive(true);
        MasterDataAuditSupport.stampOnUpdate(entity);
        return toDto(repository.save(entity));
    }

    private SubscaleArticleMaster findByBusinessId(Long id) {
        return repository.findBySubscaleArticleId(id)
                .or(() -> repository.findById(id))
                .orElseThrow(() -> new AcemException(ErrorCode.MASTER_DATA_NOT_FOUND, "Subscale article not found"));
    }

    private ResolvedArticle resolveArticleFields(String requestedName, Map<String, Object> attrs) {
        boolean isBem = Boolean.TRUE.equals(attrs.get("isBem"));
        if (isBem) {
            Object rawSize = attrs.get("bemSize");
            Integer bemSize = null;
            if (rawSize instanceof Integer i) {
                bemSize = i;
            } else if (rawSize instanceof Number n) {
                bemSize = n.intValue();
            }
            if (bemSize == null || bemSize < 1) {
                throw new AcemException(ErrorCode.VALIDATION_ERROR, "BEM size is required and must be a positive integer");
            }
            String unit = attrs.get("bemUnit") instanceof String s && !s.isBlank()
                    ? s.trim()
                    : DEFAULT_BEM_UNIT;
            String composed = bemSize + " " + unit + " BEM";
            String name = MasterDataValidation.validateName(composed);
            if (requestedName != null && !requestedName.isBlank()
                    && !name.equals(requestedName.trim())) {
                throw new AcemException(ErrorCode.VALIDATION_ERROR,
                        "BEM article name is derived from size and unit; expected '" + name + "'");
            }
            return new ResolvedArticle(name, true, bemSize, unit);
        }

        String name = MasterDataValidation.validateName(requestedName);
        return new ResolvedArticle(name, false, null, null);
    }

    private long nextId() {
        return repository.findAll().stream()
                .map(SubscaleArticleMaster::getSubscaleArticleId)
                .filter(Objects::nonNull)
                .max(Long::compareTo)
                .orElse(0L) + 1L;
    }

    private int nextDisplayOrder() {
        return repository.findAll().stream()
                .map(SubscaleArticleMaster::getDisplayOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    private MasterDataRecordDto toDto(SubscaleArticleMaster entity) {
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("isBem", Boolean.TRUE.equals(entity.getIsBem()));
        if (entity.getBemSize() != null) {
            attrs.put("bemSize", entity.getBemSize());
        }
        if (entity.getBemUnit() != null && !entity.getBemUnit().isBlank()) {
            attrs.put("bemUnit", entity.getBemUnit());
        }
        return MasterDataValidation.record(entity.getSubscaleArticleId(), entity.getSubscaleArticleCode(),
                entity.getSubscaleArticleName(), entity.getIsActive(), attrs, entity);
    }

    private record ResolvedArticle(String name, boolean isBem, Integer bemSize, String bemUnit) {
    }
}
