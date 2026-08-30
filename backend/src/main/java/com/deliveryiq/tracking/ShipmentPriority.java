package com.deliveryiq.tracking;

public enum ShipmentPriority {
    STANDARD,
    EXPRESS,
    CRITICAL;

    public int weightBonusMinutes() {
        return switch (this) {
            case STANDARD -> 0;
            case EXPRESS -> -30;
            case CRITICAL -> -60;
        };
    }
}
