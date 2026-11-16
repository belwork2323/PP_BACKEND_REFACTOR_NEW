package in.co.bel.acempp.dto.user.operations;

import lombok.*;

import java.util.List;

@Data
@Builder
public class MaterialLotDto {
    private String materialCode;
    private MaterialGrade grade;
    private String materialName;
    private List<String> lotId;
    private String make;
}