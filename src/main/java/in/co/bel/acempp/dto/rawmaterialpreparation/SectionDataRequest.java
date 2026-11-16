package in.co.bel.acempp.dto.rawmaterialpreparation;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SectionDataRequest {
    private String sectionId;
    private List<Map<String, Object>> sectionData;   // flexible
}