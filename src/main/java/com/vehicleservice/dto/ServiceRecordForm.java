package com.vehicleservice.dto;

import com.vehicleservice.model.ServiceRecord;
import com.vehicleservice.model.ServiceType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ServiceRecordForm {

    @NotNull(message = "Select a vehicle")
    private Long vehicleId;

    @NotNull(message = "Select a service type")
    private ServiceType serviceType;

    @NotNull(message = "Enter the service date")
    @PastOrPresent(message = "Service date cannot be in the future")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate serviceDate;

    @NotNull(message = "Enter the odometer reading at service")
    @PositiveOrZero(message = "Mileage cannot be negative")
    private Integer mileage;

    @NotNull(message = "Enter the cost (0 if free)")
    @PositiveOrZero(message = "Cost cannot be negative")
    @Digits(integer = 8, fraction = 2, message = "Enter an amount like 4500 or 4500.50")
    private BigDecimal cost;

    @Size(max = 150, message = "Service center must be at most 150 characters")
    private String serviceCenter;

    @Size(max = 1000, message = "Description must be at most 1000 characters")
    private String description;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate nextServiceDate;

    @Positive(message = "Next service mileage must be positive")
    private Integer nextServiceMileage;

    /** When true (new records only), a reminder is created from the next-service fields. */
    private boolean createReminder = true;

    public static ServiceRecordForm from(ServiceRecord record) {
        ServiceRecordForm form = new ServiceRecordForm();
        form.setVehicleId(record.getVehicle().getId());
        form.setServiceType(record.getServiceType());
        form.setServiceDate(record.getServiceDate());
        form.setMileage(record.getMileage());
        form.setCost(record.getCost());
        form.setServiceCenter(record.getServiceCenter());
        form.setDescription(record.getDescription());
        form.setNextServiceDate(record.getNextServiceDate());
        form.setNextServiceMileage(record.getNextServiceMileage());
        form.setCreateReminder(false);
        return form;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    public LocalDate getServiceDate() {
        return serviceDate;
    }

    public void setServiceDate(LocalDate serviceDate) {
        this.serviceDate = serviceDate;
    }

    public Integer getMileage() {
        return mileage;
    }

    public void setMileage(Integer mileage) {
        this.mileage = mileage;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public String getServiceCenter() {
        return serviceCenter;
    }

    public void setServiceCenter(String serviceCenter) {
        this.serviceCenter = serviceCenter;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getNextServiceDate() {
        return nextServiceDate;
    }

    public void setNextServiceDate(LocalDate nextServiceDate) {
        this.nextServiceDate = nextServiceDate;
    }

    public Integer getNextServiceMileage() {
        return nextServiceMileage;
    }

    public void setNextServiceMileage(Integer nextServiceMileage) {
        this.nextServiceMileage = nextServiceMileage;
    }

    public boolean isCreateReminder() {
        return createReminder;
    }

    public void setCreateReminder(boolean createReminder) {
        this.createReminder = createReminder;
    }
}
