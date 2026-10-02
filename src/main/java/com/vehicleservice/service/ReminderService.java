package com.vehicleservice.service;

import com.vehicleservice.dto.ReminderForm;
import com.vehicleservice.exception.ResourceNotFoundException;
import com.vehicleservice.model.Reminder;
import com.vehicleservice.model.ReminderStatus;
import com.vehicleservice.model.ReminderType;
import com.vehicleservice.model.ServiceRecord;
import com.vehicleservice.model.ServiceType;
import com.vehicleservice.model.Vehicle;
import com.vehicleservice.repository.ReminderRepository;
import com.vehicleservice.util.ReminderStatusUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Reminder management. Before reminders are shown, their status is recalculated
 * (time passes and mileage changes), so a reminder automatically moves from
 * UPCOMING to DUE SOON to OVERDUE without the user doing anything.
 */
@Service
@Transactional
public class ReminderService {

    /** Most urgent first, then earliest due date, then lowest due mileage. */
    public static final Comparator<Reminder> URGENCY_ORDER =
            Comparator.comparingInt((Reminder r) -> r.getStatus().getPriority())
                      .thenComparing(Reminder::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                      .thenComparing(Reminder::getDueMileage, Comparator.nullsLast(Comparator.naturalOrder()));

    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final ReminderRepository reminderRepository;
    private final VehicleService vehicleService;

    public ReminderService(ReminderRepository reminderRepository, VehicleService vehicleService) {
        this.reminderRepository = reminderRepository;
        this.vehicleService = vehicleService;
    }

    /** Recalculates and saves the status of all of this user's reminders. */
    public void refreshStatuses(Long userId) {
        applyCalculatedStatus(reminderRepository.findByVehicleUserId(userId));
    }

    public List<Reminder> search(Long userId, Long vehicleId, ReminderStatus status, ReminderType type) {
        refreshStatuses(userId);
        List<Reminder> reminders = new ArrayList<>(reminderRepository.search(userId, vehicleId, status, type));
        reminders.sort(URGENCY_ORDER);
        return reminders;
    }

    /** Reminders still waiting (UPCOMING or DUE SOON), most urgent first. */
    public List<Reminder> getUpcoming(Long userId, int limit) {
        refreshStatuses(userId);
        return reminderRepository.findByVehicleUserId(userId).stream()
                .filter(r -> r.getStatus() == ReminderStatus.UPCOMING || r.getStatus() == ReminderStatus.DUE_SOON)
                .sorted(URGENCY_ORDER)
                .limit(limit)
                .toList();
    }

    public List<Reminder> getOverdue(Long userId, int limit) {
        refreshStatuses(userId);
        return reminderRepository.findByVehicleUserId(userId).stream()
                .filter(r -> r.getStatus() == ReminderStatus.OVERDUE)
                .sorted(URGENCY_ORDER)
                .limit(limit)
                .toList();
    }

    /** Count per status, e.g. {OVERDUE=1, DUE_SOON=2, UPCOMING=3, COMPLETED=4}. */
    public Map<ReminderStatus, Long> countByStatus(Long userId) {
        refreshStatuses(userId);
        Map<ReminderStatus, Long> counts = new EnumMap<>(ReminderStatus.class);
        for (ReminderStatus status : ReminderStatus.values()) {
            counts.put(status, 0L);
        }
        for (Reminder reminder : reminderRepository.findByVehicleUserId(userId)) {
            counts.merge(reminder.getStatus(), 1L, Long::sum);
        }
        return counts;
    }

    public Reminder getReminder(Long reminderId, Long userId) {
        Reminder reminder = reminderRepository.findByIdAndVehicleUserId(reminderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder", reminderId));
        applyCalculatedStatus(List.of(reminder));
        return reminder;
    }

    public Reminder create(Long userId, ReminderForm form) {
        Vehicle vehicle = vehicleService.getVehicle(form.getVehicleId(), userId);
        Reminder reminder = new Reminder();
        reminder.setVehicle(vehicle);
        applyForm(reminder, form);
        return reminderRepository.save(reminder);
    }

    public Reminder update(Long reminderId, Long userId, ReminderForm form) {
        Reminder reminder = getReminder(reminderId, userId);
        reminder.setVehicle(vehicleService.getVehicle(form.getVehicleId(), userId));
        applyForm(reminder, form);
        return reminder;
    }

    public void markCompleted(Long reminderId, Long userId) {
        getReminder(reminderId, userId).setStatus(ReminderStatus.COMPLETED);
    }

    /** Re-opens a completed reminder; its status is recalculated from date/mileage. */
    public void reopen(Long reminderId, Long userId) {
        Reminder reminder = getReminder(reminderId, userId);
        reminder.setStatus(ReminderStatus.UPCOMING);
        reminder.setStatus(ReminderStatusUtil.resolveStatus(reminder, LocalDate.now()));
    }

    public void delete(Long reminderId, Long userId) {
        reminderRepository.delete(getReminder(reminderId, userId));
    }

    /**
     * Creates the "next service" reminder from a service record.
     * Returns null when the record has no next service date or mileage.
     */
    public Reminder createFromServiceRecord(ServiceRecord record) {
        if (record.getNextServiceDate() == null && record.getNextServiceMileage() == null) {
            return null;
        }
        Reminder reminder = new Reminder();
        reminder.setVehicle(record.getVehicle());
        reminder.setReminderType(toReminderType(record.getServiceType()));
        reminder.setTitle("Next " + record.getServiceType().getDisplayName().toLowerCase());
        reminder.setDescription("Created from the service on " + record.getServiceDate().format(DISPLAY_DATE)
                + (record.getServiceCenter() != null ? " at " + record.getServiceCenter() : "") + ".");
        reminder.setDueDate(record.getNextServiceDate());
        reminder.setDueMileage(record.getNextServiceMileage());
        reminder.setStatus(ReminderStatusUtil.resolveStatus(reminder, LocalDate.now()));
        return reminderRepository.save(reminder);
    }

    public static ReminderType toReminderType(ServiceType serviceType) {
        return switch (serviceType) {
            case GENERAL_SERVICE, ENGINE_SERVICE -> ReminderType.GENERAL_SERVICE;
            case OIL_CHANGE -> ReminderType.OIL_CHANGE;
            case BRAKE_SERVICE -> ReminderType.BRAKE_SERVICE;
            case TYRE_REPLACEMENT -> ReminderType.TYRE_REPLACEMENT;
            case BATTERY_REPLACEMENT -> ReminderType.BATTERY_REPLACEMENT;
            case AC_SERVICE, WHEEL_ALIGNMENT, OTHER -> ReminderType.OTHER;
        };
    }

    private void applyForm(Reminder reminder, ReminderForm form) {
        reminder.setReminderType(form.getReminderType());
        reminder.setTitle(form.getTitle().trim());
        reminder.setDescription(form.getDescription() == null || form.getDescription().isBlank()
                ? null : form.getDescription().trim());
        reminder.setDueDate(form.getDueDate());
        reminder.setDueMileage(form.getDueMileage());
        if (form.isCompleted()) {
            reminder.setStatus(ReminderStatus.COMPLETED);
        } else {
            reminder.setStatus(ReminderStatus.UPCOMING);   // clear COMPLETED so the rules apply
            reminder.setStatus(ReminderStatusUtil.resolveStatus(reminder, LocalDate.now()));
        }
    }

    private void applyCalculatedStatus(Collection<Reminder> reminders) {
        LocalDate today = LocalDate.now();
        for (Reminder reminder : reminders) {
            ReminderStatus calculated = ReminderStatusUtil.resolveStatus(reminder, today);
            if (calculated != reminder.getStatus()) {
                reminder.setStatus(calculated);   // saved automatically when the transaction commits
            }
        }
    }
}
