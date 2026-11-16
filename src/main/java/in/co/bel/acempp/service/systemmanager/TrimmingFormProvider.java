package in.co.bel.acempp.service.systemmanager;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.trimming.trimmingcreation.MotorDTO;
import in.co.bel.acempp.dto.trimming.trimmingcreation.SectionDTO;
import in.co.bel.acempp.entity.manufacturing.Trimming;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.repository.manufacturing.TrimmingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TrimmingFormProvider implements SubDepartmentFormProvider {

    private final TrimmingRepository trimmingRepo;

    @Override
    public Map<String, Object> getFormData(String batchId) {
        Trimming form = trimmingRepo.findByBatchIdAndIsDeletedFalse(batchId).orElse(null);
        if (form == null || form.getMotors() == null) return null;

        List<List<SectionDTO>> motorMaps = form.getMotors().stream()
                .map(MotorDTO::getSections)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("motors", motorMaps);
        return result;
    }

    @Override
    public Status getSubmissionType(String batchId) {
        return trimmingRepo.findByBatchIdAndIsDeletedFalse(batchId)
                .map(Trimming::getFormSubmissionType).orElse(null);
    }

    @Override
    public PersonInfo getSubmittedBy(String batchId) {
        return trimmingRepo.findByBatchIdAndIsDeletedFalse(batchId)
                .map(Trimming::getCreatedBy).orElse(null);
    }

    @Override
    public LocalDateTime getSubmittedAt(String batchId) {
        return trimmingRepo.findByBatchIdAndIsDeletedFalse(batchId)
                .map(Trimming::getCreatedAt).orElse(null);
    }
}