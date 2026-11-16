package in.co.bel.acempp.dto.rawmaterialsourcing.preparation;

public interface BlendingStylePreparationDetails {

    String getMfgBatchLotNo();

    Double getTotalQty();

    String getEquipmentId();

    Double getAgitatorRpm();

    Double getProcessTemp();

    Double getJacketTemp();

    String getProcessStartTime();

    String getProcessEndTime();

    String getOtherObservations();
}
