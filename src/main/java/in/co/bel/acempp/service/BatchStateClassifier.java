package in.co.bel.acempp.service;

import in.co.bel.acempp.entity.batch.Batch;
import in.co.bel.acempp.enums.Status;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class BatchStateClassifier {

    public enum BatchState { REJECTED, COMPLETED, PENDING, IN_PROGRESS }

    public BatchState classify(Batch batch) {
        List<Batch.StageProgress> stages = batch.getStageProgress() == null
                ? List.of()
                : batch.getStageProgress();

        if (stages.stream().anyMatch(s -> s.getStatus() == Status.REJECTED)) {
            return BatchState.REJECTED;
        }

        Set<Long> approved = stages.stream()
                .filter(s -> s.getStatus() == Status.COMPLETELY_APPROVED)
                .map(Batch.StageProgress::getSubDepartmentId)
                .collect(Collectors.toSet());

        boolean completed = "SUBSCALE".equals(batch.getBatchType())
                ? approved.containsAll(Set.of(11L, 12L))
                : approved.contains(13L);
        if (completed) {
            return BatchState.COMPLETED;
        }

        if (stages.stream().anyMatch(s ->
                s.getStatus() == Status.WAITING_FOR_APPROVAL
                        || s.getStatus() == Status.WAITING_FOR_PARTIAL_APPROVAL)) {
            return BatchState.PENDING;
        }

        return BatchState.IN_PROGRESS;
    }

    public Map<BatchState, Long> countByState(List<Batch> batches) {
        return batches.stream()
                .collect(Collectors.groupingBy(this::classify, Collectors.counting()));
    }
}
