package in.co.bel.acempp.service.batch;

import java.util.List;
import java.util.Set;

public final class MainBatchStageConstants {

    public static final long RMP = 3L;
    public static final long CASE_PREP = 4L;
    public static final long MIXING = 5L;
    public static final long CASTING_CURING = 6L;
    public static final long POST_CURE = 7L;
    public static final long SUBSCALE = 8L;
    public static final long TRIMMING = 9L;
    public static final long NDT = 10L;
    public static final long QC = 11L;
    public static final long STF = 12L;
    public static final long DISPATCH = 13L;

    public static final List<Long> MAIN_INITIAL_STAGES = List.of(RMP, CASE_PREP, QC);

    public static final Set<Long> MOTOR_STAGES = Set.of(
            CASE_PREP, CASTING_CURING, POST_CURE, TRIMMING, NDT, QC, STF, DISPATCH);

    public static final List<Long> MAIN_PROGRESS_STAGES = List.of(
            RMP, CASE_PREP, MIXING, CASTING_CURING, POST_CURE, SUBSCALE, TRIMMING,
            NDT, QC, STF, DISPATCH);

    private MainBatchStageConstants() {
    }

    public static boolean isMainBatch(String batchType) {
        return batchType != null && !"SUBSCALE".equalsIgnoreCase(batchType);
    }
}
