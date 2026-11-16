package in.co.bel.acempp.dto.masterdata;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasterDataCreateRequest {

    /** Ignored on create when the type uses server-generated codes. */
    @Size(max = 64, message = "Code must not exceed 64 characters")
    private String code;

    /** Optional when the type uses server-generated names (e.g. motor-stages). */
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    private Boolean isActive;

    @Builder.Default
    private Map<String, Object> attributes = new LinkedHashMap<>();
}
