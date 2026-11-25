package in.co.bel.acempp.service.batch;

import in.co.bel.acempp.entity.batch.Batch;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.enums.StageType;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.service.qualitycontrol.QcDivisionCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MainBatchGateValidator {

    public void assertPremixUnlocked(Batch batch, long subDeptId, int premixNo, StageType stageType) {
        if (!MainBatchStageProgressHelper.isParallelMainFlow(batch)) {
            return;
        }
        if (!MainBatchStageProgressHelper.isPremixUnlocked(batch, subDeptId, premixNo, stageType)) {
            throw new AcemException(
                    ErrorCode.INVALID_STATE,
                    "Premix " + premixNo + " is not yet unlocked in subdepartment " + subDeptId);
        }
    }

    public void assertMotorUnlocked(Batch batch, long subDeptId, String motorId) {
        BatchMotorDiscardHelper.assertNotDiscarded(batch, motorId);
        if (!MainBatchStageProgressHelper.isParallelMainFlow(batch)) {
            return;
        }
        if (!MainBatchStageProgressHelper.isMotorUnlocked(batch, subDeptId, motorId)) {
            throw new AcemException(
                    ErrorCode.INVALID_STATE,
                    "Motor " + motorId + " is not yet unlocked in subdepartment " + subDeptId);
        }
    }

    public void assertMixingPremixCanBeSubmitted(Batch batch, int premixNo) {
        if (!MainBatchStageProgressHelper.isParallelMainFlow(batch)) {
            return;
        }
        if (!MainBatchStageProgressHelper.isPremixApproved(batch, premixNo)) {
            throw new AcemException(
                    ErrorCode.INVALID_STATE,
                    "Premix " + premixNo + " cannot be submitted in Mixing until it is approved in RMP");
        }
        assertPremixUnlocked(batch, MainBatchStageConstants.MIXING, premixNo, StageType.PREMIX);
    }

    public void assertSubscaleCanBeStarted(Batch batch) {
        if (!MainBatchStageProgressHelper.isParallelMainFlow(batch)) {
            return;
        }
        if (!MainBatchStageProgressHelper.areAllPremixesApproved(batch, MainBatchStageConstants.MIXING)) {
            throw new AcemException(
                    ErrorCode.INVALID_STATE,
                    "All Mixing premixes must be approved before starting Subscale");
        }
        if (!MainBatchStageProgressHelper.areAllFinalMixesApproved(batch)) {
            throw new AcemException(
                    ErrorCode.INVALID_STATE,
                    "All Mixing final mixes must be approved before starting Subscale");
        }
    }

    public void assertQcDivisionUnlocked(Batch batch, String divisionName) {
        if (batch == null) {
            return;
        }
        boolean enforce = MainBatchStageProgressHelper.isParallelMainFlow(batch)
                || "SUBSCALE".equalsIgnoreCase(batch.getBatchType());
        if (!enforce) {
            return;
        }
        if (QcDivisionProgressHelper.isExperimentalSubscale(batch)
                && QcDivisionCatalog.isMotorScoped(divisionName)) {
            // STF remains available; other motor divisions stay N/A.
            if (!QcDivisionCatalog.isStaticTestFacility(divisionName)) {
                throw new AcemException(
                        ErrorCode.INVALID_STATE,
                        "QC motor divisions are not available for Subscale Experimental batches");
            }
        }
        if (!QcDivisionProgressHelper.isDivisionUnlocked(batch, divisionName)) {
            throw new AcemException(
                    ErrorCode.INVALID_STATE,
                    "QC division is not yet unlocked: " + divisionName);
        }
    }

    public void assertQcPremixUnitUnlocked(
            Batch batch, String divisionName, int premixNo, StageType stageType) {
        assertQcDivisionUnlocked(batch, divisionName);
        if (!MainBatchStageProgressHelper.isParallelMainFlow(batch)
                && !QcDivisionProgressHelper.isQualificationSubscale(batch)
                && !QcDivisionProgressHelper.isExperimentalSubscale(batch)) {
            return;
        }
        if (!QcDivisionProgressHelper.isPremixUnitUnlocked(batch, divisionName, premixNo, stageType)) {
            throw new AcemException(
                    ErrorCode.INVALID_STATE,
                    "Premix " + premixNo + " is not yet unlocked in QC division " + divisionName);
        }
    }

    public void assertQcMotorUnitUnlocked(Batch batch, String divisionName, String motorId) {
        BatchMotorDiscardHelper.assertNotDiscarded(batch, motorId);
        assertQcDivisionUnlocked(batch, divisionName);
        if (!MainBatchStageProgressHelper.isParallelMainFlow(batch)
                && !QcDivisionProgressHelper.isQualificationSubscale(batch)) {
            return;
        }
        if (!QcDivisionProgressHelper.isMotorUnitUnlocked(batch, divisionName, motorId)) {
            throw new AcemException(
                    ErrorCode.INVALID_STATE,
                    "Motor " + motorId + " is not yet unlocked in QC division " + divisionName);
        }
    }

    public void assertCastingCuringMotorUnlocked(Batch batch, String motorId) {
        assertMotorUnlocked(batch, MainBatchStageConstants.CASTING_CURING, motorId);
    }

    public void assertCastingCuringMotorCanBeSubmitted(Batch batch, String motorId) {
        if (!MainBatchStageProgressHelper.isParallelMainFlow(batch)) {
            return;
        }
        assertCastingCuringMotorUnlocked(batch, motorId);
        if (!MainBatchStageProgressHelper.areAllPremixesApproved(batch, MainBatchStageConstants.RMP)) {
            throw new AcemException(
                    ErrorCode.INVALID_STATE,
                    "All premixes must be approved in RMP before submitting a motor in Casting and Curing");
        }
        if (!MainBatchStageProgressHelper.areAllPremixesApproved(batch, MainBatchStageConstants.MIXING)) {
            throw new AcemException(
                    ErrorCode.INVALID_STATE,
                    "All premixes must be approved in Mixing before submitting a motor in Casting and Curing");
        }
        if (!MainBatchStageProgressHelper.areAllFinalMixesApproved(batch)) {
            throw new AcemException(
                    ErrorCode.INVALID_STATE,
                    "All final mixes must be approved in Mixing before submitting a motor in Casting and Curing");
        }
    }

    public void assertDispatchMotorCanBeSubmitted(Batch batch, String motorId) {
        if (!MainBatchStageProgressHelper.isParallelMainFlow(batch)) {
            return;
        }
        if (!QcDivisionProgressHelper.isMotorReadyForDispatch(batch, motorId)) {
            throw new AcemException(
                    ErrorCode.INVALID_STATE,
                    "Motor " + motorId
                            + " is not ready for dispatch: QC revalidation, all RMP/Mixing units, "
                            + "and the same motor across Hardware–Weighment/QC must be approved, "
                            + "and at least one STF BEM motor must be approved");
        }
        assertMotorUnlocked(batch, MainBatchStageConstants.DISPATCH, motorId);
    }
}
