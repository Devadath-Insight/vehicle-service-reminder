package com.vehicleservice;

import com.vehicleservice.dto.ExpenseForm;
import com.vehicleservice.dto.RegistrationRequest;
import com.vehicleservice.dto.ReminderForm;
import com.vehicleservice.dto.ServiceRecordForm;
import com.vehicleservice.dto.VehicleForm;
import com.vehicleservice.model.ExpenseType;
import com.vehicleservice.model.FuelType;
import com.vehicleservice.model.ReminderType;
import com.vehicleservice.model.ServiceType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Small factory of ready-to-use form objects so every test does not repeat the same setup code.
 * (Test helper only - not part of the application.)
 */
public final class TestData {

    public static final String PASSWORD = "secret1234";

    private TestData() {
    }

    public static RegistrationRequest registration(String email) {
        RegistrationRequest request = new RegistrationRequest();
        request.setFullName("Test User");
        request.setEmail(email);
        request.setPhone("9876543210");
        request.setPassword(PASSWORD);
        request.setConfirmPassword(PASSWORD);
        return request;
    }

    public static VehicleForm vehicle(String registrationNumber, int currentMileage) {
        VehicleForm form = new VehicleForm();
        form.setRegistrationNumber(registrationNumber);
        form.setMake("Maruti Suzuki");
        form.setModel("Swift");
        form.setYear(2020);
        form.setFuelType(FuelType.PETROL);
        form.setCurrentMileage(currentMileage);
        form.setColor("White");
        form.setPurchaseDate(LocalDate.of(2020, 1, 15));
        return form;
    }

    public static ServiceRecordForm service(Long vehicleId, ServiceType type, LocalDate date,
                                            int mileage, String cost) {
        ServiceRecordForm form = new ServiceRecordForm();
        form.setVehicleId(vehicleId);
        form.setServiceType(type);
        form.setServiceDate(date);
        form.setMileage(mileage);
        form.setCost(new BigDecimal(cost));
        form.setServiceCenter("Test Motors");
        form.setDescription("Test service");
        form.setCreateReminder(false);
        return form;
    }

    public static ReminderForm reminder(Long vehicleId, ReminderType type, LocalDate dueDate, Integer dueMileage) {
        ReminderForm form = new ReminderForm();
        form.setVehicleId(vehicleId);
        form.setReminderType(type);
        form.setTitle(type.getDisplayName());
        form.setDueDate(dueDate);
        form.setDueMileage(dueMileage);
        return form;
    }

    public static ExpenseForm expense(Long vehicleId, ExpenseType type, String amount, LocalDate date) {
        ExpenseForm form = new ExpenseForm();
        form.setVehicleId(vehicleId);
        form.setExpenseType(type);
        form.setAmount(new BigDecimal(amount));
        form.setExpenseDate(date);
        form.setDescription("Test expense");
        return form;
    }
}
