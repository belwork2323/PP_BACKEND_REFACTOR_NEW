package in.co.bel.acempp.service.systemmanager;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.entity.dispatch.Dispatch;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.repository.dispatch.DispatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
@Component
@RequiredArgsConstructor
public class DispatchFormProvider implements SubDepartmentFormProvider {

    private final DispatchRepository dispatchRepo;

    @Override
    public Map<String, Object> getFormData(String batchId) {
        Dispatch form = dispatchRepo.findByBatchIdAndIsDeletedFalse(batchId).orElse(null);
        if (form == null || form.getMotors() == null) return null;

        Map<String, Object> result = new HashMap<>();
        result.put("formId", form.getFormId());
        result.put("status", form.getStatus());
        result.put("motors", form.getMotors());
        return result;
    }

    @Override
    public Status getSubmissionType(String batchId) {
        return dispatchRepo.findByBatchIdAndIsDeletedFalse(batchId)
                .map(Dispatch::getFormSubmissionType).orElse(null);
    }

    @Override
    public PersonInfo getSubmittedBy(String batchId) {
        return dispatchRepo.findByBatchIdAndIsDeletedFalse(batchId)
                .map(Dispatch::getCreatedBy).orElse(null);
    }

    @Override
    public LocalDateTime getSubmittedAt(String batchId) {
        return dispatchRepo.findByBatchIdAndIsDeletedFalse(batchId)
                .map(Dispatch::getCreatedAt).orElse(null);
    }
}
