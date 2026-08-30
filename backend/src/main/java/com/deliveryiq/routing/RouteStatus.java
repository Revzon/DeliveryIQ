package com.deliveryiq.routing;

public enum RouteStatus {
    PLANNED,
    ASSIGNED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    public boolean isActive() {
        return this == ASSIGNED || this == IN_PROGRESS;
    }
}
