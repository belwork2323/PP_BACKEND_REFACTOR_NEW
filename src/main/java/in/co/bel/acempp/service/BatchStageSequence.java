package in.co.bel.acempp.service;

import org.springframework.stereotype.Component;

import java.util.*;


@Component
public class BatchStageSequence {

    // Ordered stage sequence – starts at manufacturing
    private static final List<Long> STANDARD_STAGE_ORDER = List.of(
            3L, 4L, 5L, 6L, 7L,  8L, 9L,    // Manufacturing
            10L,  11L, 12L,                         // Quality Assurance
            13L                                  // Dispatch
    );
    private static final List<Long> SUBSCALE_STAGE_ORDER = List.of(
            8L,     // Subscale
            11L,    // Quality Assurance
            12L     // Static Test Facility (Last Stage)
    );
    // subDeptId → department name (for batch stage fields)
    private static final Map<Long, String[]> STAGE_META = Map.ofEntries(
            Map.entry(3L, new String[]{"Manufacturing",    "Raw Material Preparation"}),
            Map.entry(4L, new String[]{"Manufacturing",    "Case Preparation"}),
            Map.entry(5L, new String[]{"Manufacturing",    "Mixing"}),
            Map.entry(6L, new String[]{"Manufacturing",  "Casting and Curing"}),
            Map.entry(7L, new String[]{"Manufacturing",  "Post Cure Operations"}),
            Map.entry(8L, new String[]{"Manufacturing",    "Subscale"}),
            Map.entry(9L, new String[]{"Manufacturing",    "Trimming"}),
            Map.entry(10L, new String[]{"Quality Assurance",  "NDT"}),
            Map.entry(11L, new String[]{"Quality Assurance",  "Quality Assurance"}),
            Map.entry(12L, new String[]{"Quality Assurance",  "Static Test Facility"}),
            Map.entry(13L, new String[]{"Dispatch",         "Dispatch"})
    );

    // subDeptId → deptId mapping
    private static final Map<Long, Long> DEPT_MAP = Map.ofEntries(
            Map.entry(3L, 2L), Map.entry(4L, 2L), Map.entry(5L, 2L),
            Map.entry(6L, 2L), Map.entry(7L, 2L),
            Map.entry(8L, 2L), Map.entry(9L, 2L),
            Map.entry(10L, 3L), Map.entry(11L, 3L),
            Map.entry(12L, 3L),  Map.entry(13L, 4L)
    );
    private List<Long> getSequenceForBatchType(String batchType) {
        if ("SUBSCALE".equalsIgnoreCase(batchType)) {
            return SUBSCALE_STAGE_ORDER;
        }
        return STANDARD_STAGE_ORDER;
    }
    /** Returns next subDeptId after current, or null if this is the last stage */
    public Long getNextStage(Long currentSubDeptId, String batchType) {
        List<Long> sequence = getSequenceForBatchType(batchType);
        int idx = sequence.indexOf(currentSubDeptId);
        if (idx < 0 || idx >= sequence.size() - 1) return null;
        return sequence.get(idx + 1);
    }

    /** Returns first stage (now manufacturing) */
    public Long getFirstStage(String batchType) {
        return getSequenceForBatchType(batchType).get(0); // Returns 8L for SUBSCALE, 3L for standard
    }

    /** Initial stages unlocked when identification sheet is completed. */
    public List<Long> getInitialStages(String batchType, boolean parallelMainFlow) {
        if ("SUBSCALE".equalsIgnoreCase(batchType)) {
            // Subscale Processing + QC at create.
            return List.of(8L, 11L);
        }
        if (parallelMainFlow) {
            // RMP, Case Prep, QC after ID sheet; Subscale after Mixing complete.
            return List.of(3L, 4L, 11L);
        }
        return List.of(3L);
    }

    /** Stage IDs used for progress calculation. */
    public List<Long> getProgressStages(String batchType, boolean parallelMainFlow) {
        if ("SUBSCALE".equalsIgnoreCase(batchType)) {
            return SUBSCALE_STAGE_ORDER;
        }
        if (parallelMainFlow) {
            return List.of(3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L);
        }
        return STANDARD_STAGE_ORDER;
    }


    public Long getDepartmentId(Long subDeptId) {
        return DEPT_MAP.get(subDeptId);
    }

    /** True if this is the last stage */
    public boolean isLastStage(Long currentSubDeptId, String batchType) {
        List<Long> sequence = getSequenceForBatchType(batchType);
        return sequence.indexOf(currentSubDeptId) == sequence.size() - 1;
    }
    /** Returns dept name and subdept name for a given subDeptId */
    public String[] getMeta(Long subDeptId) {
        return STAGE_META.getOrDefault(subDeptId, new String[]{"Unknown", "Unknown"});
    }
    public boolean isBatchStage(Long subDeptId) {
        return STAGE_META.containsKey(subDeptId);
    }

    private static final Map<Long, String> DEPT_NAMES = Map.of(
            2L, "Manufacturing",
            3L, "Quality Assurance",
            4L, "Dispatch"
    );

    // Sub‑departments per department (display order, including sourcing)
    private static final Map<Long, List<Long>> DISPLAY_SUB_DEPTS = Map.of(
            2L, List.of(3L, 4L, 5L, 6L, 7L, 8L, 9L), // Manufacturing
            3L, List.of(10L, 11L, 12L),           // Quality Assurance
            4L, List.of(13L)                      // Dispatch
    );

    /** Ordered list of department IDs for the stages overview */
    public List<Long> getDisplayDepartmentIds() {
        return List.of(2L, 3L, 4L);
    }

    /** Sub‑department IDs under a given department (display order) */
    public List<Long> getDisplaySubDepartmentsForDepartment(Long deptId) {
        return DISPLAY_SUB_DEPTS.getOrDefault(deptId, List.of());
    }

    /** Department name by ID (for the overview) */
    public String getDepartmentName(Long deptId) {
        return DEPT_NAMES.getOrDefault(deptId, "Unknown");
    }

    public int getTotalStages(String batchType) {
        return getSequenceForBatchType(batchType).size();
    }

    public List<Long> getStages(String batchType) {
        return List.copyOf(getSequenceForBatchType(batchType));
    }
}
