package in.co.bel.acempp.service.systemmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.entity.manufacturing.RawMaterialPreparation;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.repository.manufacturing.RawMaterialPreparationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RawMaterialPrepFormProvider implements SubDepartmentFormProvider {

    private final RawMaterialPreparationRepository repo;
    private final ObjectMapper objectMapper;   // inject via constructor or use new ObjectMapper()

    @Override
    public Map<String, Object> getFormData(String batchId) {
        RawMaterialPreparation form = repo.findByBatchIdAndIsDeletedFalse(batchId).orElse(null);
        if (form == null || form.getPreparationDetails() == null) return null;
        return objectMapper.convertValue(form.getPreparationDetails(), Map.class);
    }

    @Override
    public Status getSubmissionType(String batchId) {
        return repo.findByBatchIdAndIsDeletedFalse(batchId)
                .map(RawMaterialPreparation::getFormSubmissionType).orElse(null);
    }

    @Override
    public PersonInfo getSubmittedBy(String batchId) {
        return repo.findByBatchIdAndIsDeletedFalse(batchId)
                .map(RawMaterialPreparation::getSubmittedBy).orElse(null);
    }

    @Override
    public LocalDateTime getSubmittedAt(String batchId) {
        return repo.findByBatchIdAndIsDeletedFalse(batchId)
                .map(RawMaterialPreparation::getSubmittedAt).orElse(null);
    }
}