package in.co.bel.acempp.service.qualitycontrol;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class QcDivisionCatalog {

    public static final long SUB_DEPARTMENT_ID = 11L;

    public static final long RAW_MATERIAL_REVALIDATION_ID = 101L;
    public static final long RAW_MATERIAL_PROCESSING_ID = 102L;
    public static final long MIXING_ID = 2L;
    public static final long HARDWARE_ID = 3L;
    public static final long CASTING_ID = 4L;
    public static final long CURING_ID = 5L;
    public static final long DE_CORING_ID = 6L;
    public static final long TRIMMING_ID = 7L;
    public static final long POST_CURE_ID = 8L;
    public static final long NDT_ID = 9L;
    public static final long QC_ID = 10L;
    public static final long WEIGHTMENT_ID = 11L;
    /**
     * QC Static Test Facility. Seed/enable in Mongo {@code qc_division_master}:
     * {@code db.qc_division_master.updateOne({divisionId:12},{$set:{divisionName:"STATIC_TEST_FACILITY",disabled:false,displayOrder:10}},{upsert:true})}
     * then set QC/Weighment displayOrder after NDT (e.g. 11/12).
     */
    public static final long STATIC_TEST_FACILITY_ID = 12L;

    public static final String RAW_MATERIAL_REVALIDATION = "RAW_MATERIAL_REVALIDATION";
    public static final String RAW_MATERIAL_PROCESSING = "RAW_MATERIAL_PROCESSING";
    public static final String MIXING = "MIXING";
    public static final String HARDWARE = "HARDWARE";
    public static final String CASTING = "CASTING";
    public static final String CURING = "CURING";
    public static final String DE_CORING = "DE_CORING";
    public static final String TRIMMING = "TRIMMING";
    public static final String POST_CURE = "POST_CURE";
    public static final String NDT = "NDT";
    public static final String QC = "QC";
    public static final String WEIGHTMENT = "WEIGHTMENT";
    public static final String STATIC_TEST_FACILITY = "STATIC_TEST_FACILITY";

    private static final Map<Long, String> API_DIVISIONS = Map.ofEntries(
            Map.entry(RAW_MATERIAL_REVALIDATION_ID, RAW_MATERIAL_REVALIDATION),
            Map.entry(RAW_MATERIAL_PROCESSING_ID, RAW_MATERIAL_PROCESSING),
            Map.entry(MIXING_ID, MIXING),
            Map.entry(HARDWARE_ID, HARDWARE),
            Map.entry(CASTING_ID, CASTING),
            Map.entry(CURING_ID, CURING),
            Map.entry(DE_CORING_ID, DE_CORING),
            Map.entry(TRIMMING_ID, TRIMMING),
            Map.entry(POST_CURE_ID, POST_CURE),
            Map.entry(NDT_ID, NDT),
            Map.entry(QC_ID, QC),
            Map.entry(WEIGHTMENT_ID, WEIGHTMENT),
            Map.entry(STATIC_TEST_FACILITY_ID, STATIC_TEST_FACILITY));

    private static final Set<String> PREMIX_DIVISIONS = Set.of(
            RAW_MATERIAL_PROCESSING, MIXING);

    private static final Set<String> DIVISION_SCOPED_DIVISIONS = Set.of(
            RAW_MATERIAL_REVALIDATION);

    private static final Set<String> MOTOR_DIVISIONS = Set.of(
            HARDWARE, CASTING, CURING, DE_CORING, TRIMMING, POST_CURE, NDT, QC, WEIGHTMENT,
            STATIC_TEST_FACILITY);

    /** Motor-scoped divisions that must approve the same motor before STF (MAIN / Qualification). */
    public static final List<String> STF_MOTOR_DIVISIONS = List.of(
            HARDWARE, CASTING, CURING, DE_CORING, POST_CURE, TRIMMING, NDT, WEIGHTMENT, QC);

    public static final Set<String> EXPERIMENTAL_UNLOCKED_DIVISIONS = Set.of(
            RAW_MATERIAL_REVALIDATION, RAW_MATERIAL_PROCESSING, MIXING);

    private QcDivisionCatalog() {
    }

    public static String canonicalName(Long divisionId) {
        return API_DIVISIONS.get(divisionId);
    }

    /** Accepts legacy PROPELLANT_PROPERTIES as alias for QC. */
    public static String normalizeDivisionName(String division) {
        if (division == null) {
            return null;
        }
        if ("PROPELLANT_PROPERTIES".equals(division)) {
            return QC;
        }
        return division;
    }

    public static boolean isPremixScoped(String division) {
        return PREMIX_DIVISIONS.contains(normalizeDivisionName(division));
    }

    public static boolean isDivisionScoped(String division) {
        return DIVISION_SCOPED_DIVISIONS.contains(normalizeDivisionName(division));
    }

    public static boolean isMotorScoped(String division) {
        return MOTOR_DIVISIONS.contains(normalizeDivisionName(division));
    }

    public static boolean isExperimentalUnlocked(String division) {
        return EXPERIMENTAL_UNLOCKED_DIVISIONS.contains(normalizeDivisionName(division));
    }
}
