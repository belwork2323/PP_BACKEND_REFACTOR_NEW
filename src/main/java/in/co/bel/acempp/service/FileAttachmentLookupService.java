package in.co.bel.acempp.service;

import in.co.bel.acempp.entity.FileAttachments;
import in.co.bel.acempp.repository.FileSystemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FileAttachmentLookupService {

    private final FileSystemRepository fileSystemRepository;
    private final MongoTemplate mongoTemplate;

    public Optional<FileAttachments> resolve(String fileId, Long subDepartmentId, String motorId) {
        if (fileId == null || fileId.isBlank()) {
            return Optional.empty();
        }
        String normalizedFileId = fileId.trim();

        if (subDepartmentId != null) {
            Optional<FileAttachments> bySubDept = fileSystemRepository
                    .findBySubDepartmentIdAndFileId(subDepartmentId, normalizedFileId);
            if (bySubDept.isPresent()) {
                return bySubDept;
            }
        }

        if (motorId != null && !motorId.isBlank()) {
            Query query = new Query(
                    Criteria.where("motorId").is(motorId.trim())
                            .and("fileId").is(normalizedFileId));
            FileAttachments byMotor = mongoTemplate.findOne(query, FileAttachments.class, "file_attachments");
            if (byMotor != null) {
                return Optional.of(byMotor);
            }
        }

        return fileSystemRepository.findByFileId(normalizedFileId);
    }
}
