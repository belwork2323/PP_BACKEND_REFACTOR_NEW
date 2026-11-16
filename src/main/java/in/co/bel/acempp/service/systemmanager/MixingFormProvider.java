package in.co.bel.acempp.service.systemmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.entity.manufacturing.Mixing;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.repository.manufacturing.MixingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
@Component
@RequiredArgsConstructor
public class MixingFormProvider implements SubDepartmentFormProvider {

    private final MixingRepository mixingRepo;
    private final ObjectMapper objectMapper;
    @Override
    public Map<String, Object> getFormData(String batchId) {
        Mixing form = mixingRepo.findByBatchIdAndIsDeletedFalse(batchId).orElse(null);
        if (form == null || form.getMixingDetails() == null) return null;
        return objectMapper.convertValue(form.getMixingDetails(), Map.class);
    }

    @Override
    public Status getSubmissionType(String batchId) {
        return mixingRepo.findByBatchIdAndIsDeletedFalse(batchId)
                .map(Mixing::getFormSubmissionType).orElse(null);
    }

    @Override
    public PersonInfo getSubmittedBy(String batchId) {
        return mixingRepo.findByBatchIdAndIsDeletedFalse(batchId)
                .map(Mixing::getSubmittedBy).orElse(null);
    }

    @Override
    public LocalDateTime getSubmittedAt(String batchId) {
        return mixingRepo.findByBatchIdAndIsDeletedFalse(batchId)
                .map(Mixing::getSubmittedAt).orElse(null);
    }
}