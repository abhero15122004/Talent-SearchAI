package com.talentmatch.workflow.entity;

public enum ApplicationStage {
    NEW,
    REVIEWING,
    SHORTLISTED,
    INTERVIEW,
    REJECTED,
    HIRED;

    /**
     * Enforces the documented workflow state-transition policy:
     * - NEW -> REVIEWING, SHORTLISTED, REJECTED
     * - REVIEWING -> SHORTLISTED, INTERVIEW, REJECTED
     * - SHORTLISTED -> INTERVIEW, REVIEWING, REJECTED
     * - INTERVIEW -> SHORTLISTED, HIRED, REJECTED
     * - REJECTED -> REVIEWING (requires deliberate recruiter action)
     * - HIRED -> REVIEWING (reopening terminal state requires confirmation)
     */
    public boolean canTransitionTo(ApplicationStage target) {
        if (this == target) {
            return true; // No-op transition allowed
        }
        return switch (this) {
            case NEW -> target == REVIEWING || target == SHORTLISTED || target == REJECTED;
            case REVIEWING -> target == SHORTLISTED || target == INTERVIEW || target == REJECTED;
            case SHORTLISTED -> target == INTERVIEW || target == REVIEWING || target == REJECTED;
            case INTERVIEW -> target == SHORTLISTED || target == HIRED || target == REJECTED;
            case REJECTED -> target == REVIEWING;
            case HIRED -> target == REVIEWING;
        };
    }

    public boolean isTerminal() {
        return this == REJECTED || this == HIRED;
    }
}
