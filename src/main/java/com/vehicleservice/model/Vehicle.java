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
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;

/**
 * A vehicle owned by one user (many vehicles -> one user).
 * The relationship is unidirectional (Vehicle knows its User, User does not hold a list of vehicles)
 * which avoids lazy-loading and serialization problems.
 */
@Entity
@Table(name = "vehicles",
       uniqueConstraints = @UniqueConstraint(name = "uk_vehicles_user_registration",
                                             columnNames = {"user_id", "registration_number"}),
       indexes = {
           @Index(name = "idx_vehicles_user_id", columnList = "user_id"),
           @Index(name = "idx_vehicles_registration_number", columnList = "registration_number")
       })
public class Vehicle extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_vehicles_user"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(name = "registration_number", nullable = false, length = 20)
    private String registrationNumber;

    @Column(name = "make", nullable = false, length = 50)
    private String make;

    @Column(name = "model", nullable = false, length = 50)
    private String model;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", nullable = false, length = 20)
    private FuelType fuelType;

    @Column(name = "current_mileage", nullable = false)
    private Integer currentMileage;

    @Column(name = "color", length = 30)
    private String color;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    /** "Maruti Suzuki Swift" - convenient label for lists and dropdowns. */
    public String getMakeAndModel() {
        return make + " " + model;
    }

    /** "KL 01 AB 1234 (Maruti Suzuki Swift)" - label for dropdowns. */
    public String getDisplayName() {
        return registrationNumber + " (" + getMakeAndModel() + ")";
    }
}
