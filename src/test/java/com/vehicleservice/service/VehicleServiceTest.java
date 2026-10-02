package com.vehicleservice.service;

import com.vehicleservice.TestData;
import com.vehicleservice.dto.VehicleForm;
import com.vehicleservice.exception.ResourceNotFoundException;
import com.vehicleservice.model.ExpenseType;
import com.vehicleservice.model.FuelType;
import com.vehicleservice.model.ReminderType;
import com.vehicleservice.model.ServiceType;
import com.vehicleservice.model.User;
import com.vehicleservice.model.Vehicle;
import com.vehicleservice.repository.ExpenseRepository;
import com.vehicleservice.repository.ReminderRepository;
import com.vehicleservice.repository.ServiceRecordRepository;
import com.vehicleservice.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Vehicle CRUD, search, unique registration per user and user data isolation. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VehicleServiceTest {

    @Autowired private UserService userService;
    @Autowired private VehicleService vehicleService;
    @Autowired private ServiceRecordService serviceRecordService;
    @Autowired private ReminderService reminderService;
    @Autowired private ExpenseService expenseService;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private ServiceRecordRepository serviceRecordRepository;
    @Autowired private ReminderRepository reminderRepository;
    @Autowired private ExpenseRepository expenseRepository;

    private User alice;
    private User bob;

    @BeforeEach
    void createUsers() {
        alice = userService.register(TestData.registration("alice@example.com"));
        bob = userService.register(TestData.registration("bob@example.com"));
    }

    @Test
    void createAndReadVehicle() {
        Vehicle created = vehicleService.create(alice, TestData.vehicle("  kl 01   ab 1234 ", 25_000));

        Vehicle found = vehicleService.getVehicle(created.getId(), alice.getId());
        assertThat(found.getRegistrationNumber()).isEqualTo("KL 01 AB 1234");   // normalised
        assertThat(found.getMakeAndModel()).isEqualTo("Maruti Suzuki Swift");
        assertThat(found.getCurrentMileage()).isEqualTo(25_000);
        assertThat(vehicleService.countVehicles(alice.getId())).isEqualTo(1);
    }

    @Test
    void updateVehicle() {
        Vehicle created = vehicleService.create(alice, TestData.vehicle("KL01AB1111", 10_000));
        VehicleForm form = VehicleForm.from(created);
        form.setModel("Dzire");
        form.setFuelType(FuelType.CNG);
        form.setCurrentMileage(12_345);

        vehicleService.update(created.getId(), alice.getId(), form);

        Vehicle updated = vehicleService.getVehicle(created.getId(), alice.getId());
        assertThat(updated.getModel()).isEqualTo("Dzire");
        assertThat(updated.getFuelType()).isEqualTo(FuelType.CNG);
        assertThat(updated.getCurrentMileage()).isEqualTo(12_345);
    }

    @Test
    void deleteVehicleAlsoDeletesItsRecords() {
        Vehicle vehicle = vehicleService.create(alice, TestData.vehicle("KL01AB2222", 10_000));
        Long id = vehicle.getId();
        serviceRecordService.create(alice.getId(),
                TestData.service(id, ServiceType.OIL_CHANGE, LocalDate.now().minusDays(3), 10_100, "1500"));
        reminderService.create(alice.getId(),
                TestData.reminder(id, ReminderType.INSURANCE, LocalDate.now().plusDays(60), null));
        expenseService.create(alice.getId(),
                TestData.expense(id, ExpenseType.FUEL, "2000", LocalDate.now()));

        vehicleService.delete(id, alice.getId());

        assertThat(vehicleRepository.findById(id)).isEmpty();
        assertThat(serviceRecordRepository.findByVehicleUserId(alice.getId())).isEmpty();
        assertThat(reminderRepository.findByVehicleUserId(alice.getId())).isEmpty();
        assertThat(expenseRepository.findByVehicleUserId(alice.getId())).isEmpty();
    }

    @Test
    void searchByRegistrationMakeOrModel() {
        vehicleService.create(alice, TestData.vehicle("KL01AB1234", 1_000));
        VehicleForm honda = TestData.vehicle("KL07CD4521", 2_000);
        honda.setMake("Honda");
        honda.setModel("City");
        vehicleService.create(alice, honda);

        assertThat(vehicleService.searchVehicles(alice.getId(), "cd45")).hasSize(1);
        assertThat(vehicleService.searchVehicles(alice.getId(), "honda")).extracting(Vehicle::getModel).containsExactly("City");
        assertThat(vehicleService.searchVehicles(alice.getId(), "Honda City")).hasSize(1);
        assertThat(vehicleService.searchVehicles(alice.getId(), "")).hasSize(2);
        assertThat(vehicleService.searchVehicles(alice.getId(), "tesla")).isEmpty();
    }

    @Test
    void registrationMustBeUniqueForTheSameUserOnly() {
        Vehicle first = vehicleService.create(alice, TestData.vehicle("KL01AB1234", 1_000));

        assertThat(vehicleService.isRegistrationTaken(alice.getId(), "kl01ab1234", null)).isTrue();
        // editing the same vehicle keeps its own number
        assertThat(vehicleService.isRegistrationTaken(alice.getId(), "KL01AB1234", first.getId())).isFalse();
        // a different user may register the same number
        assertThat(vehicleService.isRegistrationTaken(bob.getId(), "KL01AB1234", null)).isFalse();
    }

    @Test
    void userCannotAccessAnotherUsersVehicle() {
        Vehicle alicesCar = vehicleService.create(alice, TestData.vehicle("KL01AB9999", 1_000));

        assertThatThrownBy(() -> vehicleService.getVehicle(alicesCar.getId(), bob.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> vehicleService.update(alicesCar.getId(), bob.getId(), TestData.vehicle("HACK", 1)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> vehicleService.delete(alicesCar.getId(), bob.getId()))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(vehicleService.getVehicles(bob.getId())).isEmpty();
        List<Vehicle> alicesList = vehicleService.getVehicles(alice.getId());
        assertThat(alicesList).hasSize(1);
    }

    @Test
    void mileageOnlyGoesUp() {
        Vehicle vehicle = vehicleService.create(alice, TestData.vehicle("KL01AB5555", 20_000));

        vehicleService.updateMileageIfHigher(vehicle, 19_000);
        assertThat(vehicle.getCurrentMileage()).isEqualTo(20_000);

        vehicleService.updateMileageIfHigher(vehicle, 21_000);
        assertThat(vehicle.getCurrentMileage()).isEqualTo(21_000);
    }
}
