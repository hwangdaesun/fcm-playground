package com.example.fcmretryplayground.domain.notification;

public enum FcmTokenStatus {
    ACTIVE(true),
    INVALID(false),
    BLOCKED(false);

    private final boolean deliverable;

    FcmTokenStatus(boolean deliverable) {
        this.deliverable = deliverable;
    }

    public boolean isDeliverable() {
        return deliverable;
    }

}
