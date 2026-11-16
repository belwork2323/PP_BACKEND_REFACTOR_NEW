package in.co.bel.acempp.service.masterdata;

import java.util.Comparator;
import java.util.function.Function;

public final class MasterDataListOrdering {

    private MasterDataListOrdering() {
    }

    public static <T> Comparator<T> byLongIdDesc(Function<T, Long> idExtractor) {
        return Comparator.comparing(idExtractor, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    public static <T> Comparator<T> byMongoIdDesc(Function<T, String> idExtractor) {
        return (left, right) -> {
            String leftId = idExtractor.apply(left);
            String rightId = idExtractor.apply(right);
            if (leftId == null && rightId == null) {
                return 0;
            }
            if (leftId == null) {
                return 1;
            }
            if (rightId == null) {
                return -1;
            }
            return rightId.compareTo(leftId);
        };
    }
}
