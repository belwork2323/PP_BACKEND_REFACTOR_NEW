package in.co.bel.acempp.entity.manufacturing;

import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@Builder @NoArgsConstructor @AllArgsConstructor
public class SectionData {
    private String sectionId;
    private List<Map<String, Object>> sectionData;   // dynamic JSON objects
}