// ExplorerRequest.java
package in.co.bel.acempp.dto.blockchain;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

public class ExplorerRequest {

    @Data
    public static class GenesisHashRequest {
        @NotBlank
        private String genesisHash;
    }

    @Data
    public static class ChartRequest {
        @NotBlank
        private String hash;          // channel genesis hash

        @NotBlank
        private String type;          // "BLOCKS" or "TX"

        @NotBlank
        private String interval;      // "HOUR" or "MIN"
    }
}