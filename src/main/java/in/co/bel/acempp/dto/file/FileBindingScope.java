package in.co.bel.acempp.dto.file;

import in.co.bel.acempp.enums.StageType;
import lombok.Builder;

@Builder
public record FileBindingScope(
        String lotId,
        String motorCasingId,
        String motorId,
        Integer premixNo,
        StageType stageType,
        Long divisionId,
        String division,
        String subType,
        String stfTestNo,
        String bemNo
) {
    public static FileBindingScope forMotor(String motorId) {
        return FileBindingScope.builder().motorId(motorId).build();
    }

    public static FileBindingScope forLot(String lotId) {
        return FileBindingScope.builder().lotId(lotId).build();
    }

    public static FileBindingScope forMotorCasing(String motorCasingId) {
        return FileBindingScope.builder().motorCasingId(motorCasingId).build();
    }

    public static FileBindingScope forPremix(Integer premixNo, StageType stageType) {
        return FileBindingScope.builder().premixNo(premixNo).stageType(stageType).build();
    }

    public static FileBindingScope forStf(String motorId, String stfTestNo, String subType, String bemNo) {
        return FileBindingScope.builder()
                .motorId(motorId)
                .stfTestNo(stfTestNo)
                .subType(subType)
                .bemNo(bemNo)
                .build();
    }

    public static FileBindingScope forBemMotor(String motorId, String stfTestNo) {
        return FileBindingScope.builder()
                .motorId(motorId)
                .stfTestNo(stfTestNo)
                .subType("BEM")
                .bemNo(motorId)
                .build();
    }
}
