package com.vehicleservice.dto;

import com.vehicleservice.model.FuelType;
import com.vehicleservice.model.Vehicle;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Form object for adding/editing a vehicle. Using a separate form class (instead of binding
 * directly to the entity) means a user can never set fields like "user" through the form.
 */
public class VehicleForm {

    @NotBlank(message = "Enter the registration number")
    @Size(max = 20, message = "Registration number must be at most 20 characters")
    @Pattern(regexp = "^[A-Za-z0-9 -]+$", message = "Use only letters, numbers, spaces and hyphens")
    private String registrationNumber;

    @NotBlank(message = "Enter the make (manufacturer)")
    @Size(max = 50, message = "Make must be at most 50 characters")
    private String make;

    @NotBlank(message = "Enter the model")
    @Size(max = 50, message = "Model must be at most 50 characters")
    private String model;

    @NotNull(message = "Enter the manufacturing year")
    @Min(value = 1950, message = "Year must be 1950 or later")
    @Max(value = 2100, message = "Enter a valid year")
    private Integer year;

    @NotNull(message = "Select a fuel type")
    private FuelType fuelType;

    @NotNull(message = "Enter the current odometer reading")
    @PositiveOrZero(message = "Mileage cannot be negative")
    @Max(value = 2_000_000, message = "Mileage looks too high")
    private Integer currentMileage;

    @Size(max = 30, message = "Colour must be at most 30 characters")
    private String color;

    @PastOrPresent(message = "Purchase date cannot be in the future")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate purchaseDate;

    public static VehicleForm from(Vehicle vehicle) {
        VehicleForm form = new VehicleForm();
        form.setRegistrationNumber(vehicle.getRegistrationNumber());
        form.setMake(vehicle.getMake());
        form.setModel(vehicle.getModel());
        form.setYear(vehicle.getYear());
        form.setFuelType(vehicle.getFuelType());
        form.setCurrentMileage(vehicle.getCurrentMileage());
        form.setColor(vehicle.getColor());
        form.setPurchaseDate(vehicle.getPurchaseDate());
        return form;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public FuelType getFuelType() {
        return fuelType;
    }

    public void setFuelType(FuelType fuelType) {
        this.fuelType = fuelType;
    }

    public Integer getCurrentMileage() {
        return currentMileage;
    }

    public void setCurrentMileage(Integer currentMileage) {
        this.currentMileage = currentMileage;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }
}
