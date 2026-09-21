package com.commercegrid.auth.enums;

public enum StatusReasonCode {
    // -> LOCKED
    SUSPICIOUS_ACTIVITY(AdminStatus.LOCKED),
    CREDENTIAL_COMPROMISE_SUSPECTED(AdminStatus.LOCKED),
    POLICY_VIOLATION_UNDER_REVIEW(AdminStatus.LOCKED),
    AUDIT_INVESTIGATION(AdminStatus.LOCKED),
    REPEATED_FAILED_LOGINS(AdminStatus.LOCKED),

    // -> INACTIVE
    EMPLOYEE_OFFBOARDED(AdminStatus.INACTIVE),
    CONTRACT_ENDED(AdminStatus.INACTIVE),
    ROLE_CHANGED(AdminStatus.INACTIVE),
    DORMANT_ACCOUNT(AdminStatus.INACTIVE),
    LONG_LEAVE(AdminStatus.INACTIVE),

    // -> ACTIVE
    REVIEW_CLEARED(AdminStatus.ACTIVE),
    RETURNED_FROM_LEAVE(AdminStatus.ACTIVE),
    REHIRED(AdminStatus.ACTIVE);

    private final AdminStatus targetStatus;

    StatusReasonCode(AdminStatus targetStatus) { this.targetStatus = targetStatus; }

    public AdminStatus getTargetStatus() { return targetStatus; }
}