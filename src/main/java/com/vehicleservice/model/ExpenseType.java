package com.vehicleservice.model;

/** Categories of vehicle expenses. */
public enum ExpenseType implements DisplayableEnum {

    SERVICE("Service"),
    FUEL("Fuel"),
    INSURANCE("Insurance"),
    REPAIR("Repair"),
    TYRES("Tyres"),
    BATTERY("Battery"),
    PARTS("Parts"),
    CLEANING("Cleaning"),
    OTHER("Other");

    private final String displayName;

    ExpenseType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }
}
