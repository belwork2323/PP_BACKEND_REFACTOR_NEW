package in.co.bel.acempp.service.systemmanager;

import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.enums.Status;

import java.time.LocalDateTime;
import java.util.Map;

public interface SubDepartmentFormProvider {

    /** Returns the form data as a flat Map, or null if the form hasn't been started. */
    Map<String, Object> getFormData(String batchId);

    /** @return DRAFT / SUBMIT / null */
    Status getSubmissionType(String batchId);

    PersonInfo getSubmittedBy(String batchId);
    LocalDateTime getSubmittedAt(String batchId);
}