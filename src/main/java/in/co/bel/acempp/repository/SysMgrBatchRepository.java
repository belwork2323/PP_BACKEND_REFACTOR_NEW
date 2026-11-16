package in.co.bel.acempp.repository;
import in.co.bel.acempp.entity.batch.Batch;

import java.util.List;

public interface SysMgrBatchRepository {

    List<Batch> getBatchStatusList(
            String managerId,
            String search,
            int page,
            int limit
    );

    Long getBatchStatusCount(
            String managerId,
            String search
    );
}
