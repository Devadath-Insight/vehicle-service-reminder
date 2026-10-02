package com.vehicleservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * A maintenance reminder for a vehicle. It can be due by date, by mileage, or both.
 * The status is calculated by ReminderStatusUtil - never inside the HTML.
 */
@Entity
@Table(name = "reminders",
       indexes = {
           @Index(name = "idx_reminders_vehicle_id", columnList = "vehicle_id"),
           @Index(name = "idx_reminders_due_date", columnList = "due_date")
       })
public class Reminder extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reminders_vehicle"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_type", nullable = false, length = 30)
    private ReminderType reminderType;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "due_mileage")
    private Integer dueMileage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReminderStatus status = ReminderStatus.UPCOMING;

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
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

    public ReminderStatus getStatus() {
        return status;
    }

    public void setStatus(ReminderStatus status) {
        this.status = status;
    }

    /** "Date", "Mileage" or "Date & mileage" - what triggers this reminder. */
    public String getBasis() {
        if (dueDate != null && dueMileage != null) {
            return "Date & mileage";
        }
        return dueDate != null ? "Date" : "Mileage";
    }

    /** Days until the due date (negative when overdue), or null when there is no due date. */
    public Long getDaysUntilDue() {
        return dueDate == null ? null : ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
    }

    /** Kilometres left before the due mileage (negative when overdue), or null when not mileage-based. */
    public Integer getKmRemaining() {
        if (dueMileage == null || vehicle == null || vehicle.getCurrentMileage() == null) {
            return null;
        }
        return dueMileage - vehicle.getCurrentMileage();
    }
}
