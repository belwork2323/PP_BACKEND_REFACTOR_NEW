package in.co.bel.acempp.util;

import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.exception.AcemException;

import java.time.LocalDateTime;
import java.util.function.Consumer;

public final class ApprovalStatusUtil {

    private ApprovalStatusUtil() {}

    public static void requireLegacyFinalApprovalState(Status currentStatus) {
        if (currentStatus != Status.WAITING_FOR_PARTIAL_APPROVAL) {
            throw new AcemException(
                    ErrorCode.FINAL_APPROVAL_NOT_READY,
                    "Form is not waiting for legacy final approval");
        }
    }

    public static void requireDraftFormWorkflow(Status submissionType) {
        if (submissionType != Status.DRAFT) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "formSubmissionType must remain DRAFT; completion is automatic");
        }
    }


    public static void applyStatusChange(Status actionType, Long subDeptId, String remarks, String rejectionReason,
            String approverName, Consumer<Status> statusSetter, Consumer<String> remarksSetter,
            Consumer<String> rejectionReasonSetter, Consumer<PersonInfo> updatedBySetter,
            Consumer<LocalDateTime> updatedAtSetter) {

        validateStatusChange(actionType, remarks, rejectionReason);
        if (actionType == Status.APPROVED) {
            remarksSetter.accept(remarks);
            rejectionReasonSetter.accept(null);

        } else {
            rejectionReasonSetter.accept(rejectionReason);
            remarksSetter.accept(null);
        }
        Status statusToSave = actionType;

        if (actionType == Status.APPROVED) {
            if (subDeptId == 1L || subDeptId == 2L) {
                statusToSave = Status.APPROVED;
            } else {
                statusToSave = Status.COMPLETELY_APPROVED;
            }
        }

        statusSetter.accept(statusToSave);

        updatedBySetter.accept(
                PersonInfo.builder()
                        .fullName(approverName)
                        .build()
        );
        updatedAtSetter.accept(LocalDateTime.now());
    }

    private static void validateStatusChange(Status actionType, String remarks, String rejectionReason) {

        if (actionType != Status.APPROVED &&
                actionType != Status.REJECTED) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "Invalid request",
                    "Only APPROVED or REJECTED actions are allowed"
            );
        }

        if (actionType == Status.APPROVED && (remarks == null || remarks.isBlank())) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "Invalid request",
                    "Remarks are required for APPROVED"
            );
        }

        if (actionType == Status.REJECTED && (rejectionReason == null || rejectionReason.isBlank())) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "Invalid request",
                    "Rejection reason is required for REJECTED"
            );
        }
    }
}
