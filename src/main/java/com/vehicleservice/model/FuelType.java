package com.vehicleservice.model;

/** Fuel types a vehicle can use. */
public enum FuelType implements DisplayableEnum {

    PETROL("Petrol"),
    DIESEL("Diesel"),
    ELECTRIC("Electric"),
    HYBRID("Hybrid"),
    CNG("CNG"),
    OTHER("Other");

    private final String displayName;

    FuelType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }
}
