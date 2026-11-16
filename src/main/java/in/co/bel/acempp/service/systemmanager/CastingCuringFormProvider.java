package in.co.bel.acempp.service.systemmanager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.entity.manufacturing.CastingCuring;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.repository.manufacturing.CastingCuringRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CastingCuringFormProvider implements SubDepartmentFormProvider {

    private final CastingCuringRepository castingCuringRepo;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> getFormData(String batchId) {
        CastingCuring form = castingCuringRepo.findByBatchIdAndIsDeletedFalse(batchId).orElse(null);
        if (form == null || form.getMotors() == null) return null;

        // Wrap the list of motor detail maps into a top-level map with key "motors"
        List<Map<String, Object>> motorMaps = form.getMotors().stream()
                .map(motor -> objectMapper.convertValue(
                        motor,
                        new TypeReference<Map<String, Object>>() {}))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("motors", motorMaps);
        return result;
    }

    @Override
    public Status getSubmissionType(String batchId) {
        return castingCuringRepo.findByBatchIdAndIsDeletedFalse(batchId)
                .map(CastingCuring::getFormSubmissionType).orElse(null);
    }

    @Override
    public PersonInfo getSubmittedBy(String batchId) {
        return castingCuringRepo.findByBatchIdAndIsDeletedFalse(batchId)
                .map(CastingCuring::getSubmittedBy).orElse(null);
    }

    @Override
    public LocalDateTime getSubmittedAt(String batchId) {
        return castingCuringRepo.findByBatchIdAndIsDeletedFalse(batchId)
                .map(CastingCuring::getSubmittedAt).orElse(null);
    }
}
