package in.co.bel.acempp.service.masterdata;

import in.co.bel.acempp.dto.masterdata.*;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.exception.AcemException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MasterDataService {

    private final Map<String, MasterDataTypeHandler> handlers;

    public MasterDataService(List<MasterDataTypeHandler> handlerList) {
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(MasterDataTypeHandler::type, Function.identity()));
    }

    public List<MasterDataTypeDescriptor> getTypes() {
        return MasterDataCatalog.allTypes();
    }

    public MasterDataListResponse list(String type, MasterDataListRequest request) {
        MasterDataTypeHandler handler = requireHandler(type);
        MasterDataTypeDescriptor schema = MasterDataCatalog.get(type);
        MasterDataListRequest req = request == null ? new MasterDataListRequest() : request;

        List<MasterDataRecordDto> all = handler.listAll();
        long active = all.stream().filter(r -> Boolean.TRUE.equals(r.getIsActive())).count();
        long inactive = all.size() - active;

        List<MasterDataRecordDto> filtered = all.stream()
                .filter(r -> MasterDataValidation.matchesActiveFilter(r, req.getIsActive()))
                .filter(r -> MasterDataValidation.matchesSearch(r, req.getSearch()))
                .sorted(MasterDataListOrdering.byLongIdDesc(MasterDataRecordDto::getId))
                .toList();

        return MasterDataListResponse.builder()
                .type(type)
                .schema(schema)
                .items(filtered)
                .stats(MasterDataStatsDto.builder()
                        .total(all.size())
                        .active(active)
                        .inactive(inactive)
                        .build())
                .build();
    }

    public MasterDataRecordDto create(String type, MasterDataCreateRequest request) {
        return requireHandler(type).create(request);
    }

    public MasterDataRecordDto update(String type, MasterDataUpdateRequest request) {
        return requireHandler(type).update(request);
    }

    public MasterDataRecordDto delete(String type, MasterDataDeleteRequest request) {
        return requireHandler(type).softDisable(request.getId());
    }

    public MasterDataRecordDto enable(String type, MasterDataDeleteRequest request) {
        return requireHandler(type).enable(request.getId());
    }

    private MasterDataTypeHandler requireHandler(String type) {
        if (!MasterDataCatalog.isKnown(type)) {
            throw new AcemException(ErrorCode.INVALID_REQUEST, "Unknown master data type: " + type);
        }
        if (MasterDataCatalog.isNested(type)) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "Type '" + type + "' uses dedicated nested routes under /api/v1/admin/master-data/"
                            + type + "/*");
        }
        MasterDataTypeHandler handler = handlers.get(type);
        if (handler == null) {
            throw new AcemException(ErrorCode.INTERNAL_SERVER_ERROR, "No handler registered for type: " + type);
        }
        return handler;
    }
}
