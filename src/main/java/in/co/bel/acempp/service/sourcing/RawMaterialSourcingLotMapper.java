package in.co.bel.acempp.service.sourcing;

import in.co.bel.acempp.dto.rawmaterialsourcing.SpecificationRequest;
import in.co.bel.acempp.entity.MaterialSpecification;
import in.co.bel.acempp.entity.sourcing.Lot;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.util.SpecResultFormatValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RawMaterialSourcingLotMapper {

    private final RawMaterialSourcingValidationSupport validationSupport;

    String normalizeOptionalText(String value) {
        return validationSupport.normalizeOptionalText(value);
    }

    List<Lot.LotSpec> buildLotSpecs(
            List<SpecificationRequest> specificationRequests,
            List<MaterialSpecification> masterSpecifications,
            boolean updatePath) {
        List<Lot.LotSpec> lotSpecs = new ArrayList<>();

        if (specificationRequests == null) {
            return lotSpecs;
        }

        for (SpecificationRequest specReq : specificationRequests) {

            MaterialSpecification masterSpec = masterSpecifications.stream()
                    .filter(s ->
                            s.getSpecificationCode()
                                    .equals(specReq.getSpecificationCode()))
                    .findFirst()
                    .orElseThrow(() ->
                            new AcemException(
                                    ErrorCode.SPECIFICATION_NOT_FOUND,
                                    updatePath
                                            ? "Specification code not found for material: "
                                            + specReq.getSpecificationCode()
                                            : "Specification not found: "
                                            + specReq.getSpecificationCode()));

            Double minValue = null;
            Double maxValue = null;
            Long unitId = null;
            String unit = null;
            if (masterSpec.getReferenceRange() != null) {
                minValue = masterSpec.getReferenceRange().getMinValue();
                maxValue = masterSpec.getReferenceRange().getMaxValue();
                unitId = masterSpec.getReferenceRange().getUnitId();
                unit = masterSpec.getReferenceRange().getUnit();
            }
            String persistedResult = SpecResultFormatValidator.normalizeForPersist(
                    specReq.getAnalysedResult(), minValue, maxValue);

            boolean outOfRange = SpecResultFormatValidator.isOutOfRange(
                    persistedResult,
                    minValue,
                    maxValue
            );

            String specStatus = outOfRange
                    ? "Failed"
                    : "Passed";

            lotSpecs.add(
                    Lot.LotSpec.builder()
                            .specificationCode(masterSpec.getSpecificationCode())
                            .specificationName(masterSpec.getSpecificationName())
                            .minValue(minValue)
                            .maxValue(maxValue)
                            .unitId(unitId)
                            .unit(unit)
                            .analysedResult(persistedResult)
                            .isOutOfRange(outOfRange)
                            .acemQcResult(SpecResultFormatValidator.normalizeForPersist(
                                    specReq.getAcemQcResult(), minValue, maxValue))
                            .status(specStatus)
                            .build()
            );
        }
        return lotSpecs;
    }
}