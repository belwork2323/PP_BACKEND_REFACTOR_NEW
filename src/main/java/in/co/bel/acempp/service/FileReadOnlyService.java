package in.co.bel.acempp.service;

import in.co.bel.acempp.entity.FileAttachments;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.exception.AcemException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileReadOnlyService {

    private final MongoTemplate mongoTemplate;
    private final FileAttachmentLookupService fileAttachmentLookupService;

    public void markReadOnly(Long subDepartmentId, Collection<String> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        List<String> ids = fileIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return;
        }

        Criteria criteria = Criteria.where("fileId").in(ids).and("isDeleted").ne(true);
        if (subDepartmentId != null && subDepartmentId != 11L) {
            criteria = criteria.and("subDepartmentId").is(subDepartmentId);
        }

        Update update = new Update().set("readOnly", true);
        mongoTemplate.updateMulti(new Query(criteria), update, FileAttachments.class, "file_attachments");
        log.info("Marked {} file(s) read-only for subDepartmentId={}", ids.size(), subDepartmentId);
    }

    public void markReadOnlyForForm(String formId, Long subDepartmentId) {
        if (formId == null || formId.isBlank()) {
            return;
        }
        Criteria criteria = Criteria.where("formId").is(formId)
                .and("isDeleted").ne(true)
                .and("readOnly").ne(true);
        if (subDepartmentId != null && subDepartmentId != 11L) {
            criteria = criteria.and("subDepartmentId").is(subDepartmentId);
        }
        Update update = new Update().set("readOnly", true);
        mongoTemplate.updateMulti(new Query(criteria), update, FileAttachments.class, "file_attachments");
        log.info("Marked all form files read-only for formId={}, subDepartmentId={}", formId, subDepartmentId);
    }

    public void assertWritable(Long subDepartmentId, String fileId) {
        if (fileId == null || fileId.isBlank()) {
            return;
        }
        if (isReadOnly(subDepartmentId, fileId)) {
            throw new AcemException(
                    ErrorCode.FILE_READ_ONLY,
                    "Approved file cannot be modified or deleted: " + fileId
            );
        }
    }


    public boolean isReadOnly(Long subDepartmentId, String fileId) {
        if (fileId == null || fileId.isBlank()) {
            return false;
        }
        Long lookupSubDept = subDepartmentId != null && subDepartmentId != 11L ? subDepartmentId : null;
        FileAttachments attachment = fileAttachmentLookupService
                .resolve(fileId, lookupSubDept, null)
                .orElse(null);
        return attachment != null && attachment.isReadOnly();
    }

    public List<String> excludeReadOnly(Long subDepartmentId, Collection<String> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return List.of();
        }
        return fileIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .filter(id -> !isReadOnly(subDepartmentId, id))
                .distinct()
                .toList();
    }
}
