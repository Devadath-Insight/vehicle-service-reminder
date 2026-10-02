package com.vehicleservice.util;

import com.vehicleservice.model.Reminder;
import com.vehicleservice.model.ReminderStatus;

import java.time.LocalDate;

/**
 * Business rules for reminder status. Kept in plain Java (not HTML or JavaScript)
 * so it can be unit-tested and reused.
 *
 * Date rules:     due date passed -> OVERDUE, within 30 days -> DUE_SOON, otherwise UPCOMING
 * Mileage rules:  current >= due -> OVERDUE, within 500 km -> DUE_SOON, otherwise UPCOMING
 * If a reminder has both, the MORE URGENT of the two results wins.
 */
public final class ReminderStatusUtil {

    public static final int DUE_SOON_DAYS = 30;
    public static final int DUE_SOON_KM = 500;

    private ReminderStatusUtil() {
        // utility class - no objects needed
    }

    public static ReminderStatus statusByDate(LocalDate dueDate, LocalDate today) {
        if (dueDate.isBefore(today)) {
            return ReminderStatus.OVERDUE;
        }
        if (!dueDate.isAfter(today.plusDays(DUE_SOON_DAYS))) {
            return ReminderStatus.DUE_SOON;
        }
        return ReminderStatus.UPCOMING;
    }

    public static ReminderStatus statusByMileage(int dueMileage, int currentMileage) {
        if (currentMileage >= dueMileage) {
            return ReminderStatus.OVERDUE;
        }
        if (dueMileage - currentMileage <= DUE_SOON_KM) {
            return ReminderStatus.DUE_SOON;
        }
        return ReminderStatus.UPCOMING;
    }

    /** Calculates the status from raw values. Never returns COMPLETED (that is set by the user). */
    public static ReminderStatus calculateStatus(LocalDate dueDate, Integer dueMileage,
                                                 Integer currentMileage, LocalDate today) {
        ReminderStatus result = ReminderStatus.UPCOMING;
        if (dueDate != null) {
            result = mostUrgent(result, statusByDate(dueDate, today));
        }
        if (dueMileage != null && currentMileage != null) {
            result = mostUrgent(result, statusByMileage(dueMileage, currentMileage));
        }
        return result;
    }

    /** Status for an existing reminder. Completed reminders stay completed. */
    public static ReminderStatus resolveStatus(Reminder reminder, LocalDate today) {
        if (reminder.getStatus() == ReminderStatus.COMPLETED) {
            return ReminderStatus.COMPLETED;
        }
        Integer currentMileage = reminder.getVehicle() != null ? reminder.getVehicle().getCurrentMileage() : null;
        return calculateStatus(reminder.getDueDate(), reminder.getDueMileage(), currentMileage, today);
    }

    private static ReminderStatus mostUrgent(ReminderStatus a, ReminderStatus b) {
        return a.getPriority() <= b.getPriority() ? a : b;
    }
}
