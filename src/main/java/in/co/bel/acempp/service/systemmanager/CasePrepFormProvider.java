package in.co.bel.acempp.service.systemmanager;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.entity.manufacturing.CasePreparation;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.repository.manufacturing.CasePreparationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CasePrepFormProvider implements SubDepartmentFormProvider {

    private final CasePreparationRepository repo;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> getFormData(String batchId) {

        CasePreparation form = repo.findByBatchIdAndIsDeletedFalse(batchId)
                .orElse(null);

        if (form == null) {
            return null;
        }

        return objectMapper.convertValue(
                form.getCasePreparationDetails(),
                new TypeReference<Map<String, Object>>() {}
        );
    }

    @Override
    public Status getSubmissionType(String batchId) {
        return repo.findByBatchIdAndIsDeletedFalse(batchId)
                .map(CasePreparation::getFormSubmissionType).orElse(null);
    }

    @Override
    public PersonInfo getSubmittedBy(String batchId) {
        return repo.findByBatchIdAndIsDeletedFalse(batchId)
                .map(CasePreparation::getSubmittedBy).orElse(null);
    }

    @Override
    public LocalDateTime getSubmittedAt(String batchId) {
        return repo.findByBatchIdAndIsDeletedFalse(batchId)
                .map(CasePreparation::getSubmittedAt).orElse(null);
    }
}