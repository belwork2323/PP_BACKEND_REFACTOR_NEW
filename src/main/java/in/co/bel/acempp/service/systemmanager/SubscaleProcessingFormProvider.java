package in.co.bel.acempp.service.systemmanager;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.entity.manufacturing.SubscaleProcessing;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.repository.manufacturing.SubscaleProcessingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
@Component
@RequiredArgsConstructor
public class SubscaleProcessingFormProvider implements SubDepartmentFormProvider {

    private final SubscaleProcessingRepository subscaleRepo;

    @Override
    public Map<String, Object> getFormData(String batchId) {
        SubscaleProcessing form = subscaleRepo.findByBatchIdAndIsDeletedFalse(batchId).orElse(null);
        if (form == null) {
            return null;
        }

        Map<String, Object> data = new LinkedHashMap<>();

        // Core metadata fields
        data.put("formId", form.getFormId());
        data.put("batchId", form.getBatchId());
        data.put("batchType", form.getBatchType());
        data.put("subDepartmentId", form.getSubDepartmentId());
        data.put("formSubmissionType", form.getFormSubmissionType());
        data.put("status", form.getStatus());

        // Individual Detail Sections
        data.put("subscaleDetails", form.getSubscaleDetails());
        data.put("hardwarePreparationDetails", form.getHardwarePreparationDetails());
        data.put("hardwarePreparationTable", form.getHardwarePreparationTable());
        data.put("castingDetails", form.getCastingDetails());
        data.put("curingDetails", form.getCuringDetails());
        data.put("ndtDetails", form.getNdtDetails());
        data.put("trimmingDetails", form.getTrimmingDetails());
        data.put("inhibitionDetails", form.getInhibitionDetails());
        data.put("staticTestingDetails", form.getStaticTestingDetails());
        data.put("mechanicalInterfaceProperties", form.getMechanicalInterfaceProperties());

        // Audit Info
        data.put("createdBy", form.getCreatedBy());
        data.put("createdAt", form.getCreatedAt());
        data.put("submittedBy", form.getSubmittedBy());
        data.put("submittedOn", form.getSubmittedOn());
        data.put("approvedBy", form.getApprovedBy());
        data.put("approvedOn", form.getApprovedOn());

        return data;
    }

    @Override
    public Status getSubmissionType(String batchId) {
        return subscaleRepo.findByBatchIdAndIsDeletedFalse(batchId)
                .map(SubscaleProcessing::getFormSubmissionType).orElse(null);
    }

    @Override
    public PersonInfo getSubmittedBy(String batchId) {
        return subscaleRepo.findByBatchIdAndIsDeletedFalse(batchId)
                .map(form -> form.getSubmittedBy() != null
                        ? form.getSubmittedBy()
                        : form.getFormSubmissionType() == Status.SUBMIT
                        ? form.getCreatedBy()
                        : null)
                .orElse(null);
    }

    @Override
    public LocalDateTime getSubmittedAt(String batchId) {
        return subscaleRepo.findByBatchIdAndIsDeletedFalse(batchId)
                .map(form -> form.getSubmittedOn() != null
                        ? form.getSubmittedOn()
                        : form.getFormSubmissionType() == Status.SUBMIT
                        ? form.getCreatedAt()
                        : null)
                .orElse(null);
    }
}
