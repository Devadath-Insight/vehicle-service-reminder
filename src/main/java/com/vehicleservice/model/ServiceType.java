package com.vehicleservice.model;

/** Kinds of service work recorded against a vehicle. */
public enum ServiceType implements DisplayableEnum {

    GENERAL_SERVICE("General Service"),
    OIL_CHANGE("Oil Change"),
    BRAKE_SERVICE("Brake Service"),
    TYRE_REPLACEMENT("Tyre Replacement"),
    BATTERY_REPLACEMENT("Battery Replacement"),
    ENGINE_SERVICE("Engine Service"),
    AC_SERVICE("AC Service"),
    WHEEL_ALIGNMENT("Wheel Alignment"),
    OTHER("Other");

    private final String displayName;

    ServiceType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }
}
