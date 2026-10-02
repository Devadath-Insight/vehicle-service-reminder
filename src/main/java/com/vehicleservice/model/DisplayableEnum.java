package com.vehicleservice.model;

/**
 * ABSTRACTION + POLYMORPHISM: every enum shown in a dropdown implements this interface,
 * so templates can call getDisplayName() on any of them without knowing the concrete type.
 */
public interface DisplayableEnum {

    String getDisplayName();
}
