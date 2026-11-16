package in.co.bel.acempp.repository;

import in.co.bel.acempp.entity.FileUploadConfig;
import in.co.bel.acempp.enums.FileCategory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileUploadConfigRepository extends MongoRepository<FileUploadConfig, String> {

    Optional<FileUploadConfig> findByCategoryAndIsActiveTrue(FileCategory category);
}