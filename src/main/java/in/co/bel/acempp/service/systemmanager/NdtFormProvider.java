package in.co.bel.acempp.service.systemmanager;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.entity.qualitycontrol.Ndt;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.repository.qualitycontrol.NdtRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
@Component
@RequiredArgsConstructor
public class NdtFormProvider implements SubDepartmentFormProvider {

    private final NdtRepository ndtRepo;

    @Override
    public Map<String, Object> getFormData(String batchId) {
        Ndt form = ndtRepo.findByBatchIdAndIsDeletedFalse(batchId).orElse(null);
        if (form == null || form.getMotors() == null) return null;

        Map<String, Object> result = new HashMap<>();
        result.put("motors", form.getMotors());
        return result;
    }

    @Override
    public Status getSubmissionType(String batchId) {
        return ndtRepo.findByBatchIdAndIsDeletedFalse(batchId)
                .map(Ndt::getFormSubmissionType).orElse(null);
    }

    @Override
    public PersonInfo getSubmittedBy(String batchId) {
        return ndtRepo.findByBatchIdAndIsDeletedFalse(batchId)
                .map(Ndt::getCreatedBy).orElse(null);
    }

    @Override
    public LocalDateTime getSubmittedAt(String batchId) {
        return ndtRepo.findByBatchIdAndIsDeletedFalse(batchId)
                .map(Ndt::getCreatedAt).orElse(null);
    }
}
