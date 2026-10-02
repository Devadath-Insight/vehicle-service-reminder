package com.vehicleservice.dto;

import com.vehicleservice.model.Reminder;
import com.vehicleservice.model.ReminderStatus;
import com.vehicleservice.model.ReminderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** Reminder form. "Due date or due mileage required" is checked in ReminderController. */
public class ReminderForm {

    @NotNull(message = "Select a vehicle")
    private Long vehicleId;

    @NotNull(message = "Select a reminder type")
    private ReminderType reminderType;

    @NotBlank(message = "Enter a title")
    @Size(max = 150, message = "Title must be at most 150 characters")
    private String title;

    @Size(max = 1000, message = "Description must be at most 1000 characters")
    private String description;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDate;

    @Positive(message = "Due mileage must be positive")
    private Integer dueMileage;

    private boolean completed;

    public static ReminderForm from(Reminder reminder) {
        ReminderForm form = new ReminderForm();
        form.setVehicleId(reminder.getVehicle().getId());
        form.setReminderType(reminder.getReminderType());
        form.setTitle(reminder.getTitle());
        form.setDescription(reminder.getDescription());
        form.setDueDate(reminder.getDueDate());
        form.setDueMileage(reminder.getDueMileage());
        form.setCompleted(reminder.getStatus() == ReminderStatus.COMPLETED);
        return form;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public ReminderType getReminderType() {
        return reminderType;
    }

    public void setReminderType(ReminderType reminderType) {
        this.reminderType = reminderType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Integer getDueMileage() {
        return dueMileage;
    }

    public void setDueMileage(Integer dueMileage) {
        this.dueMileage = dueMileage;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
