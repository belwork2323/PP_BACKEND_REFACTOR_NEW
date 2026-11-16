package in.co.bel.acempp.enums;

public enum Status {
    ACTIVE,
    INACTIVE,
    TO_BE_INITIATED,
    DISCARDED,
    IN_PROGRESS,
    WAITING_FOR_APPROVAL,
    APPROVED,
    REJECTED,
    UPDATE,
    DRAFT,
    COMPLETED,
       DELETED, SUBMIT,
    YET_TO_START,
//    WAITING_FOR_COMPLETE_APPROVAL,
    WAITING_FOR_PARTIAL_APPROVAL,
    COMPLETELY_APPROVED;

    public boolean isSubmit() {
        return this == SUBMIT;
    }

    public boolean isDraft() {
        return this == DRAFT;
    }

}
