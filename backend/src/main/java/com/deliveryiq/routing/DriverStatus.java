package com.deliveryiq.routing;

public enum DriverStatus {
    AVAILABLE,
    ON_ROUTE,
    OFF_DUTY,
    SUSPENDED;

    public boolean canAcceptAssignment() {
        return this == AVAILABLE;
    }
}
