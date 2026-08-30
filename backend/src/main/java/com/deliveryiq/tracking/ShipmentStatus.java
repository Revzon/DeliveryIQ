package com.deliveryiq.tracking;

public enum ShipmentStatus {
    CREATED,
    PICKED_UP,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    DELAYED,
    FAILED,
    CANCELLED;

    public boolean isTerminal() {
        return this == DELIVERED || this == FAILED || this == CANCELLED;
    }

    public boolean canTransitionTo(ShipmentStatus next) {
        if (isTerminal() || next == null || this == next) {
            return false;
        }
        return switch (this) {
            case CREATED -> next == PICKED_UP || next == CANCELLED;
            case PICKED_UP -> next == IN_TRANSIT || next == DELAYED || next == FAILED;
            case IN_TRANSIT -> next == OUT_FOR_DELIVERY || next == DELAYED || next == FAILED;
            case OUT_FOR_DELIVERY -> next == DELIVERED || next == DELAYED || next == FAILED;
            case DELAYED -> next == IN_TRANSIT || next == OUT_FOR_DELIVERY || next == FAILED || next == DELIVERED;
            default -> false;
        };
    }
}
