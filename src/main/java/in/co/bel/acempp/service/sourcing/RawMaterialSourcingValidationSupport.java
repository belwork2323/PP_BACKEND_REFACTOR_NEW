package in.co.bel.acempp.service.sourcing;

import in.co.bel.acempp.dto.rawmaterialsourcing.CertificateRequest;
import in.co.bel.acempp.dto.rawmaterialsourcing.LotRequest;
import in.co.bel.acempp.dto.rawmaterialsourcing.MaterialRequest;
import in.co.bel.acempp.dto.rawmaterialsourcing.SpecificationRequest;
import in.co.bel.acempp.dto.rawmaterialsourcing.UpdateLotRequest;
import in.co.bel.acempp.dto.rawmaterialsourcing.preparation.AdductPreparationDetails;
import in.co.bel.acempp.dto.rawmaterialsourcing.preparation.BlendingStylePreparationDetails;
import in.co.bel.acempp.dto.rawmaterialsourcing.preparation.HtpbBlendingPreparationDetails;
import in.co.bel.acempp.entity.MaterialMaster;
import in.co.bel.acempp.entity.MaterialSpecification;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.enums.PreparationType;
import in.co.bel.acempp.enums.RawMaterialCategory;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.service.masterdata.MaterialMasterDataService;
import in.co.bel.acempp.util.SpecResultFormatValidator;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RawMaterialSourcingValidationSupport {

    boolean isSubmitIntent(Status submissionType) {
        return submissionType == Status.SUBMIT || submissionType == Status.UPDATE;
    }

    void validateMaterialForTier(MaterialRequest material, Status submissionType) {
        Map<String, String> errors = new LinkedHashMap<>();
        collectMaterialForTierErrors(material, submissionType, "", errors);
        throwValidationErrors(errors);
    }

    void collectMaterialForTierErrors(
            MaterialRequest material,
            Status submissionType,
            String prefix,
            Map<String, String> errors) {
        String p = prefix == null || prefix.isBlank() ? "" : prefix + ".";
        if (material == null) {
            errors.put(prefix.isBlank() ? "materials" : prefix, "material is required");
            return;
        }

        SpecResultFormatValidator.validateAlphaNumField(
                material.getManufacturerName(), p + "manufacturerName", true, errors);
        SpecResultFormatValidator.validateAlphaNumField(
                material.getSupplyOrderNo(), p + "supplyOrderNo", false, errors);
        if (material.getLots() == null || material.getLots().isEmpty()) {
            errors.put(p + "lots", "lots list cannot be empty");
        } else {
            for (int i = 0; i < material.getLots().size(); i++) {
                LotRequest lot = material.getLots().get(i);
                validateLotForTier(lot, submissionType, p + "lots[" + i + "]", errors);
            }
        }
    }

    void collectUpdateLotForTierErrors(UpdateLotRequest request, Map<String, String> errors) {
        SpecResultFormatValidator.validateAlphaNumField(
                request.getManufacturerName(), "manufacturerName", true, errors);
        SpecResultFormatValidator.validateAlphaNumField(
                request.getSupplyOrderNo(), "supplyOrderNo", false, errors);
        LotRequest lotShape = LotRequest.builder()
                .lotId(request.getLotId())
                .specifications(request.getSpecifications())
                .certificates(request.getCertificates())
                .build();
        validateLotForTier(lotShape, request.getSubmissionType(), "lot", errors);
    }

    void validatePreparationDetails(
            MaterialMaster material,
            AdductPreparationDetails adductPreparation,
            HtpbBlendingPreparationDetails htpbBlendingPreparation,
            Status submissionType,
            String prefix,
            Map<String, String> errors) {
        RawMaterialCategory category = MaterialMasterDataService.resolveRawMaterialCategory(
                material.getRawMaterialType());

        if (category == RawMaterialCategory.NORMAL) {
            rejectPreparationFieldIfPresent(
                    adductPreparation, "adductPreparation", prefix, errors,
                    "adductPreparation is not allowed for normal raw materials");
            rejectPreparationFieldIfPresent(
                    htpbBlendingPreparation, "htpbBlendingPreparation", prefix, errors,
                    "htpbBlendingPreparation is not allowed for normal raw materials");
            return;
        }

        PreparationType preparationType = material.getPreparationType();
        if (preparationType == PreparationType.ADDUCT) {
            rejectUnsupportedPreparationFields(
                    preparationType,
                    prefix,
                    errors,
                    adductPreparation,
                    htpbBlendingPreparation);
            validateAdductPreparation(
                    material, adductPreparation, submissionType, prefix, errors);
        } else if (preparationType == PreparationType.HTPB_BLENDING) {
            rejectUnsupportedPreparationFields(
                    preparationType,
                    prefix,
                    errors,
                    adductPreparation,
                    htpbBlendingPreparation);
            validateBlendingStylePreparation(
                    htpbBlendingPreparation,
                    "htpbBlendingPreparation",
                    submissionType,
                    prefix,
                    errors);
        } else {
            rejectUnsupportedPreparationFields(
                    preparationType,
                    prefix,
                    errors,
                    adductPreparation,
                    htpbBlendingPreparation);
            if (isSubmitIntent(submissionType)) {
                errors.put(prefix + ".preparationDetails",
                        "Preparation details are not yet supported for preparation type " + preparationType);
            }
        }
    }

    void rejectPreparationFieldIfPresent(
            Object value,
            String fieldName,
            String prefix,
            Map<String, String> errors,
            String message) {
        if (value != null) {
            errors.put(prefix + "." + fieldName, message);
        }
    }

    void rejectUnsupportedPreparationFields(
            PreparationType preparationType,
            String prefix,
            Map<String, String> errors,
            AdductPreparationDetails adductPreparation,
            HtpbBlendingPreparationDetails htpbBlendingPreparation) {
        if (preparationType != PreparationType.ADDUCT && adductPreparation != null) {
            errors.put(prefix + ".adductPreparation",
                    "adductPreparation is not supported for preparation type " + preparationType);
        }
        if (preparationType != PreparationType.HTPB_BLENDING && htpbBlendingPreparation != null) {
            errors.put(prefix + ".htpbBlendingPreparation",
                    "htpbBlendingPreparation is not supported for preparation type " + preparationType);
        }
    }

    void validateAdductPreparation(
            MaterialMaster material,
            AdductPreparationDetails adductPreparation,
            Status submissionType,
            String prefix,
            Map<String, String> errors) {
        if (!isSubmitIntent(submissionType)) {
            return;
        }
        if (adductPreparation == null) {
            errors.put(prefix + ".adductPreparation", "adductPreparation is required");
            return;
        }
        validateAdductPreparationFields(adductPreparation, prefix + ".adductPreparation", errors);
    }

    void validateBlendingStylePreparation(
            BlendingStylePreparationDetails preparationDetails,
            String fieldName,
            Status submissionType,
            String prefix,
            Map<String, String> errors) {
        if (!isSubmitIntent(submissionType)) {
            return;
        }
        if (preparationDetails == null) {
            errors.put(prefix + "." + fieldName, fieldName + " is required");
            return;
        }
        validateBlendingStylePreparationFields(
                preparationDetails, prefix + "." + fieldName, errors);
    }

    void validateAdductPreparationFields(
            AdductPreparationDetails details,
            String prefix,
            Map<String, String> errors) {
        if (isBlank(details.getAdductBatchPrepDate())) {
            errors.put(prefix + ".adductBatchPrepDate", "adductBatchPrepDate is required");
        }
        SpecResultFormatValidator.validateAlphaNumField(
                details.getTmpMfgLotNo(), prefix + ".tmpMfgLotNo", true, errors);
        if (details.getTmpTotalQtyGm() == null) {
            errors.put(prefix + ".tmpTotalQtyGm", "tmpTotalQtyGm is required");
        }
        SpecResultFormatValidator.validateAlphaNumField(
                details.getNbdMfgLotNo(), prefix + ".nbdMfgLotNo", true, errors);
        if (details.getNbdTotalQtyGm() == null) {
            errors.put(prefix + ".nbdTotalQtyGm", "nbdTotalQtyGm is required");
        }
        if (details.getRpm() == null) {
            errors.put(prefix + ".rpm", "rpm is required");
        }
        if (details.getProcessTemp() == null) {
            errors.put(prefix + ".processTemp", "processTemp is required");
        }
        if (details.getJacketTemp() == null) {
            errors.put(prefix + ".jacketTemp", "jacketTemp is required");
        }
        if (isBlank(details.getProcessStartTime())) {
            errors.put(prefix + ".processStartTime", "processStartTime is required");
        }
        if (isBlank(details.getProcessEndTime())) {
            errors.put(prefix + ".processEndTime", "processEndTime is required");
        }
        if (details.getFinalAdductQty() == null) {
            errors.put(prefix + ".finalAdductQty", "finalAdductQty is required");
        }
        if (isBlank(details.getDispatchDateTime())) {
            errors.put(prefix + ".dispatchDateTime", "dispatchDateTime is required");
        }
    }

    void validateBlendingStylePreparationFields(
            BlendingStylePreparationDetails details,
            String prefix,
            Map<String, String> errors) {
        SpecResultFormatValidator.validateAlphaNumField(
                details.getMfgBatchLotNo(), prefix + ".mfgBatchLotNo", true, errors);
        if (details.getTotalQty() == null) {
            errors.put(prefix + ".totalQty", "totalQty is required");
        }
        SpecResultFormatValidator.validateAlphaNumField(
                details.getEquipmentId(), prefix + ".equipmentId", true, errors);
        if (details.getAgitatorRpm() == null) {
            errors.put(prefix + ".agitatorRpm", "agitatorRpm is required");
        }
        if (details.getProcessTemp() == null) {
            errors.put(prefix + ".processTemp", "processTemp is required");
        }
        if (details.getJacketTemp() == null) {
            errors.put(prefix + ".jacketTemp", "jacketTemp is required");
        }
        if (isBlank(details.getProcessStartTime())) {
            errors.put(prefix + ".processStartTime", "processStartTime is required");
        }
        if (isBlank(details.getProcessEndTime())) {
            errors.put(prefix + ".processEndTime", "processEndTime is required");
        }
    }

    void validateLotForTier(
            LotRequest lot,
            Status submissionType,
            String prefix,
            Map<String, String> errors) {
        if (lot == null) {
            errors.put(prefix, "lot is required");
            return;
        }

        SpecResultFormatValidator.validateAlphaNumField(
                lot.getLotId(), prefix + ".lotId", true, errors);

        if (!isSubmitIntent(submissionType)) {
            return;
        }

        List<SpecificationRequest> specifications = lot.getSpecifications();
        if (specifications == null || specifications.isEmpty()) {
            errors.put(prefix + ".specifications", "specifications are required");
        }
        // analysedResult required + type: validateLotSpecificationFormats (single source)

        List<CertificateRequest> certificates = lot.getCertificates();
        if (certificates == null || certificates.isEmpty()) {
            errors.put(prefix + ".certificates", "certificates are required");
        } else {
            for (int i = 0; i < certificates.size(); i++) {
                CertificateRequest cert = certificates.get(i);
                if (cert == null || isBlank(cert.getFileId())) {
                    errors.put(prefix + ".certificates[" + i + "].fileId", "fileId is required");
                }
                SpecResultFormatValidator.validateAlphaNumField(
                        cert == null ? null : cert.getCertificateType(),
                        prefix + ".certificates[" + i + "].certificateType",
                        true,
                        errors);
            }
        }
    }

    String normalizeOptionalText(String value) {
        return SpecResultFormatValidator.normalizeOptionalText(value);
    }

    boolean isBlank(String value) {
        return SpecResultFormatValidator.isBlank(value);
    }

    void throwValidationErrors(Map<String, String> errors) {
        if (errors == null || errors.isEmpty()) {
            return;
        }
        throw new AcemException(ErrorCode.VALIDATION_ERROR, "Validation failed", errors);
    }

    void validateSubmissionType(Status submissionType) {
        if (submissionType == null ||
                (submissionType != Status.DRAFT
                        && submissionType != Status.SUBMIT)) {

            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "submissionType must be DRAFT or SUBMIT"
            );
        }
    }

    void validateLotSpecificationFormats(
            List<SpecificationRequest> specifications,
            List<MaterialSpecification> masterSpecifications,
            Status submissionType,
            String specificationsPrefix,
            Map<String, String> errors) {
        if (specifications == null || specifications.isEmpty()) {
            return;
        }

        boolean required = isSubmitIntent(submissionType);
        for (int i = 0; i < specifications.size(); i++) {
            SpecificationRequest specReq = specifications.get(i);
            if (specReq == null) {
                continue;
            }

            String fieldPath = specificationsPrefix + "[" + i + "]";
            MaterialSpecification masterSpec = masterSpecifications.stream()
                    .filter(spec -> spec.getSpecificationCode().equals(specReq.getSpecificationCode()))
                    .findFirst()
                    .orElse(null);
            if (masterSpec == null) {
                errors.put(fieldPath + ".specificationCode", "specificationCode is invalid");
                continue;
            }

            SpecResultFormatValidator.validateSpecResultFormat(
                    specReq.getAnalysedResult(), masterSpec, fieldPath, "analysedResult", required, errors);
            SpecResultFormatValidator.validateSpecResultFormat(
                    specReq.getAcemQcResult(), masterSpec, fieldPath, "acemQcResult", false, errors);
        }
    }

    static List<MaterialSpecification> activeSpecifications(List<MaterialSpecification> specifications) {
        if (specifications == null || specifications.isEmpty()) {
            return Collections.emptyList();
        }
        return specifications.stream()
                .filter(MaterialMasterDataService::isSpecificationActive)
                .toList();
    }
}