package com.vehicleservice.model;

/**
 * Reminder lifecycle. "priority" is used to sort the most urgent reminders first,
 * "badgeClass" is the CSS class used to colour the status badge in the UI.
 */
public enum ReminderStatus implements DisplayableEnum {

    OVERDUE("Overdue", 0, "status-overdue"),
    DUE_SOON("Due Soon", 1, "status-due-soon"),
    UPCOMING("Upcoming", 2, "status-upcoming"),
    COMPLETED("Completed", 3, "status-completed");

    private final String displayName;
    private final int priority;
    private final String badgeClass;

    ReminderStatus(String displayName, int priority, String badgeClass) {
        this.displayName = displayName;
        this.priority = priority;
        this.badgeClass = badgeClass;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    public int getPriority() {
        return priority;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    /** True for reminders that still need action (not completed). */
    public boolean isActive() {
        return this != COMPLETED;
    }
}
