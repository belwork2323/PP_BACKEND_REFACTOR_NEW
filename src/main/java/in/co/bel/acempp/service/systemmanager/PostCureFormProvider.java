package in.co.bel.acempp.service.systemmanager;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.entity.manufacturing.PostCureOperations;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.repository.manufacturing.PostCureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

@Component
@RequiredArgsConstructor
public class PostCureFormProvider implements SubDepartmentFormProvider {

    private final PostCureRepository postCureRepo;

    @Override
    public Map<String, Object> getFormData(String batchId) {
        PostCureOperations form = postCureRepo.findByBatchIdAndIsDeletedFalse(batchId).orElse(null);
        if (form == null) return null;

        Map<String, Object> result = new HashMap<>();
        result.put("operationType", form.getOperationType());
        if (form.getInhibitorType() != null) {
            result.put("inhibitorType", form.getInhibitorType());
        }

        if (form.getMotors() != null) {
            result.put("motors", form.getMotors());
        }
        return result;
    }

    @Override
    public Status getSubmissionType(String batchId) {
        return postCureRepo.findByBatchIdAndIsDeletedFalse(batchId)
                .map(PostCureOperations::getFormSubmissionType).orElse(null);
    }

    @Override
    public PersonInfo getSubmittedBy(String batchId) {
        return postCureRepo.findByBatchIdAndIsDeletedFalse(batchId)
                .map(PostCureOperations::getSubmittedBy).orElse(null);
    }

    @Override
    public LocalDateTime getSubmittedAt(String batchId) {
        return postCureRepo.findByBatchIdAndIsDeletedFalse(batchId)
                .map(PostCureOperations::getSubmittedAt).orElse(null);
    }
}
