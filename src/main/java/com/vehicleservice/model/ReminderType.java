package com.vehicleservice.model;

/** What a reminder is about. */
public enum ReminderType implements DisplayableEnum {

    GENERAL_SERVICE("General Service"),
    OIL_CHANGE("Oil Change"),
    BRAKE_SERVICE("Brake Service"),
    TYRE_REPLACEMENT("Tyre Replacement"),
    BATTERY_REPLACEMENT("Battery Replacement"),
    INSURANCE("Insurance Renewal"),
    POLLUTION_CERTIFICATE("Pollution Certificate (PUC)"),
    REGISTRATION("Registration Renewal"),
    OTHER("Other");

    private final String displayName;

    ReminderType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }
}
