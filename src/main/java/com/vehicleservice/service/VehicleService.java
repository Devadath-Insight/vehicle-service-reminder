package com.vehicleservice.service;

import com.vehicleservice.dto.VehicleForm;
import com.vehicleservice.exception.ResourceNotFoundException;
import com.vehicleservice.model.User;
import com.vehicleservice.model.Vehicle;
import com.vehicleservice.repository.ExpenseRepository;
import com.vehicleservice.repository.ReminderRepository;
import com.vehicleservice.repository.ServiceRecordRepository;
import com.vehicleservice.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Vehicle management. Every method takes the logged-in user's id and only ever
 * touches that user's vehicles - this is where "users can't see each other's data" is enforced.
 */
@Service
@Transactional
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final ServiceRecordRepository serviceRecordRepository;
    private final ReminderRepository reminderRepository;
    private final ExpenseRepository expenseRepository;

    public VehicleService(VehicleRepository vehicleRepository,
                          ServiceRecordRepository serviceRecordRepository,
                          ReminderRepository reminderRepository,
                          ExpenseRepository expenseRepository) {
        this.vehicleRepository = vehicleRepository;
        this.serviceRecordRepository = serviceRecordRepository;
        this.reminderRepository = reminderRepository;
        this.expenseRepository = expenseRepository;
    }

    @Transactional(readOnly = true)
    public List<Vehicle> getVehicles(Long userId) {
        return vehicleRepository.findByUserIdOrderByRegistrationNumberAsc(userId);
    }

    @Transactional(readOnly = true)
    public List<Vehicle> searchVehicles(Long userId, String query) {
        String q = (query == null || query.isBlank()) ? null : query.trim();
        return vehicleRepository.search(userId, q);
    }

    /**
     * Ownership check: finds the vehicle only if it belongs to this user.
     * Changing the ID in the URL to someone else's vehicle gives "not found".
     */
    @Transactional(readOnly = true)
    public Vehicle getVehicle(Long vehicleId, Long userId) {
        if (vehicleId == null) {
            throw new ResourceNotFoundException("Vehicle", null);
        }
        return vehicleRepository.findByIdAndUserId(vehicleId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", vehicleId));
    }

    @Transactional(readOnly = true)
    public long countVehicles(Long userId) {
        return vehicleRepository.countByUserId(userId);
    }

    /** True if this user already has another vehicle with the same registration number. */
    @Transactional(readOnly = true)
    public boolean isRegistrationTaken(Long userId, String registrationNumber, Long excludeVehicleId) {
        if (registrationNumber == null || registrationNumber.isBlank()) {
            return false;
        }
        return vehicleRepository
                .findByUserIdAndRegistrationNumberIgnoreCase(userId, normalizeRegistration(registrationNumber))
                .filter(existing -> !Objects.equals(existing.getId(), excludeVehicleId))
                .isPresent();
    }

    public Vehicle create(User owner, VehicleForm form) {
        Vehicle vehicle = new Vehicle();
        vehicle.setUser(owner);
        applyForm(vehicle, form);
        return vehicleRepository.save(vehicle);
    }

    public Vehicle update(Long vehicleId, Long userId, VehicleForm form) {
        Vehicle vehicle = getVehicle(vehicleId, userId);
        applyForm(vehicle, form);
        return vehicle;
    }

    /** Deletes the vehicle together with its service records, reminders and expenses. */
    public void delete(Long vehicleId, Long userId) {
        getVehicle(vehicleId, userId);   // ownership check first
        serviceRecordRepository.deleteAllByVehicleId(vehicleId);
        reminderRepository.deleteAllByVehicleId(vehicleId);
        expenseRepository.deleteAllByVehicleId(vehicleId);
        vehicleRepository.deleteById(vehicleId);
    }

    /** Odometers only go up: a newer service reading updates the vehicle's current mileage. */
    public void updateMileageIfHigher(Vehicle vehicle, Integer mileage) {
        if (mileage != null && (vehicle.getCurrentMileage() == null || mileage > vehicle.getCurrentMileage())) {
            vehicle.setCurrentMileage(mileage);
        }
    }

    /** "kl 01  ab 1234 " -> "KL 01 AB 1234" */
    public static String normalizeRegistration(String registrationNumber) {
        return registrationNumber.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    private void applyForm(Vehicle vehicle, VehicleForm form) {
        vehicle.setRegistrationNumber(normalizeRegistration(form.getRegistrationNumber()));
        vehicle.setMake(form.getMake().trim());
        vehicle.setModel(form.getModel().trim());
        vehicle.setYear(form.getYear());
        vehicle.setFuelType(form.getFuelType());
        vehicle.setCurrentMileage(form.getCurrentMileage());
        vehicle.setColor(blankToNull(form.getColor()));
        vehicle.setPurchaseDate(form.getPurchaseDate());
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
