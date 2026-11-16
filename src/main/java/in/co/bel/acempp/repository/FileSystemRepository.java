package in.co.bel.acempp.repository;

import in.co.bel.acempp.entity.FileAttachments;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FileSystemRepository extends MongoRepository<FileAttachments, String> {
    Optional<FileAttachments> findBySubDepartmentIdAndFileId(Long subDepartmentId, String fileId);
    Optional<FileAttachments> findByFileId(String fileId);
}