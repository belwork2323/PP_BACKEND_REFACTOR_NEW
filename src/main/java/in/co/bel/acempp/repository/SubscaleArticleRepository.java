package in.co.bel.acempp.repository;

import in.co.bel.acempp.entity.batch.SubscaleArticleMaster;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscaleArticleRepository extends MongoRepository<SubscaleArticleMaster, Long> {

    List<SubscaleArticleMaster> findByIsActiveTrueOrderByDisplayOrderAscSubscaleArticleNameAsc();

    List<SubscaleArticleMaster> findAllByOrderByDisplayOrderAscSubscaleArticleNameAsc();

    Optional<SubscaleArticleMaster> findBySubscaleArticleCode(String code);

    Optional<SubscaleArticleMaster> findBySubscaleArticleId(Long subscaleArticleId);

    List<SubscaleArticleMaster> findBySubscaleArticleCodeIn(List<String> codes);
}
