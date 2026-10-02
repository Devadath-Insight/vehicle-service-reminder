package com.vehicleservice.exception;

/**
 * Thrown when a record does not exist OR belongs to another user.
 * Both cases produce the same "not found" page, so an attacker cannot tell
 * whether another user's record with that ID exists.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, Long id) {
        super(resourceName + " not found, or you do not have access to it.");
    }
}
