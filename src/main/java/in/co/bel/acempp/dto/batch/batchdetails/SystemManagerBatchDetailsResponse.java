package in.co.bel.acempp.dto.batch.batchdetails;

import lombok.*;

import java.util.*;

@Data
@Builder
public class SystemManagerBatchDetailsResponse {
    private Long subDepartmentId;
    private String subDepartmentName;
    private DepartmentInfo department;
    private BatchDetailsInfo batchDetails;


    private Object rawMaterialProcessingData;
    private Object casePreparationProcessingData;
    private Object mixingProcessingData;
    private Object castingCuringProcessingData;
    private Object postCureProcessingData;
    private Object subscaleProcessingData;
    private Object trimmingProcessingData;
//    private String batchStatus;                     // overall batch status at this stage
    private Map<String, Object> qcDetails;          // division → { status, data }
    private Object ndtProcessingData;
    private List<Map<String, Object>> forms;   // for static testing
    private Map<String, Object> dispatchBatchDetails;
    private List<Map<String, Object>> motors;
    private ProgressInsights progressInsights;
    private QualityInsights qualityInsights;
    private WorkflowInsights workflowInsights;
    private RiskInsights riskInsights;
    private DelayInsights delayInsights;
    private List<Alert> alerts;
    private AuditDetails auditDetails;
    private List<TimelineEvent> timeline;
    private List<Object> documents;
}