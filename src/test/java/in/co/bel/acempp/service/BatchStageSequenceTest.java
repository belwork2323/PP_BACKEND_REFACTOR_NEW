//package in.co.bel.acempp.service;
//
//import org.junit.jupiter.api.Test;
//
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//class BatchStageSequenceTest {
//
//    private final BatchStageSequence sequence = new BatchStageSequence();
//
//    @Test
//    void mainParallelInitialStagesIncludeRmpCasePrepAndQc() {
//        assertEquals(List.of(3L, 4L, 11L), sequence.getInitialStages("MAIN", true));
//    }
//
//    @Test
//    void subscaleInitialStagesIncludeSubscaleAndQc() {
//        assertEquals(List.of(8L, 11L), sequence.getInitialStages("SUBSCALE", false));
//    }
//
//    @Test
//    void legacyMainInitialStageIsRmpOnly() {
//        assertEquals(List.of(3L), sequence.getInitialStages("MAIN", false));
//    }
//}
