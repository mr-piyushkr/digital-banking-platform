package com.bankflow.kafka;

/**
 * Topic and consumer-group names in one place, so a producer and its consumers
 * cannot drift apart over a typo.
 */
public final class Topics {

    public static final String USER_REGISTERED = "user.registered";
    public static final String TRANSACTION_CREATED = "transaction.created";
    public static final String TRANSACTION_FLAGGED = "transaction.flagged";
    public static final String ACCOUNT_STATUS_CHANGED = "account.status.changed";

    /**
     * Separate groups on purpose: each one gets its own copy of every message
     * and its own offset, so a slow notification consumer never delays the
     * audit trail, and one failing consumer does not block the others.
     */
    public static final String GROUP_AUDIT = "bankflow-audit";
    public static final String GROUP_FRAUD = "bankflow-fraud";
    public static final String GROUP_NOTIFICATION = "bankflow-notification";

    private Topics() {
    }
}
