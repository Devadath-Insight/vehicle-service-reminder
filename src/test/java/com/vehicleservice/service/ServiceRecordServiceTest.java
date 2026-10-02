package com.vehicleservice.service;

import com.vehicleservice.TestData;
import com.vehicleservice.dto.ServiceRecordForm;
import com.vehicleservice.exception.ResourceNotFoundException;
import com.vehicleservice.model.Reminder;
import com.vehicleservice.model.ReminderType;
import com.vehicleservice.model.ServiceRecord;
import com.vehicleservice.model.ServiceType;
import com.vehicleservice.model.User;
import com.vehicleservice.model.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Service record CRUD, filters, totals, automatic reminder creation and isolation. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ServiceRecordServiceTest {

    @Autowired private UserService userService;
    @Autowired private VehicleService vehicleService;
    @Autowired private ServiceRecordService serviceRecordService;
    @Autowired private ReminderService reminderService;

    private User owner;
    private User stranger;
    private Vehicle car;

    @BeforeEach
    void setUp() {
        owner = userService.register(TestData.registration("owner@example.com"));
        stranger = userService.register(TestData.registration("stranger@example.com"));
        car = vehicleService.create(owner, TestData.vehicle("KL01AB1234", 30_000));
    }

    @Test
    void createRecordAndRaiseVehicleMileage() {
        ServiceRecord record = serviceRecordService.create(owner.getId(),
                TestData.service(car.getId(), ServiceType.GENERAL_SERVICE, LocalDate.now().minusDays(1), 31_500, "5850.50"));

        ServiceRecord found = serviceRecordService.getRecord(record.getId(), owner.getId());
        assertThat(found.getCost()).isEqualByComparingTo("5850.50");
        assertThat(found.getServiceType()).isEqualTo(ServiceType.GENERAL_SERVICE);
        assertThat(vehicleService.getVehicle(car.getId(), owner.getId()).getCurrentMileage()).isEqualTo(31_500);
    }

    @Test
    void createWithNextServiceCreatesReminder() {
        ServiceRecordForm form = TestData.service(car.getId(), ServiceType.OIL_CHANGE, LocalDate.now(), 30_100, "2400");
        form.setNextServiceDate(LocalDate.now().plusMonths(6));
        form.setNextServiceMileage(35_100);
        form.setCreateReminder(true);

        serviceRecordService.create(owner.getId(), form);

        List<Reminder> reminders = reminderService.search(owner.getId(), car.getId(), null, null);
        assertThat(reminders).hasSize(1);
        assertThat(reminders.get(0).getReminderType()).isEqualTo(ReminderType.OIL_CHANGE);
        assertThat(reminders.get(0).getDueMileage()).isEqualTo(35_100);
    }

    @Test
    void updateRecord() {
        ServiceRecord record = serviceRecordService.create(owner.getId(),
                TestData.service(car.getId(), ServiceType.BRAKE_SERVICE, LocalDate.now().minusDays(10), 30_000, "3000"));
        ServiceRecordForm form = ServiceRecordForm.from(record);
        form.setCost(new BigDecimal("3200.00"));
        form.setServiceCenter("Brake Masters");

        serviceRecordService.update(record.getId(), owner.getId(), form);

        ServiceRecord updated = serviceRecordService.getRecord(record.getId(), owner.getId());
        assertThat(updated.getCost()).isEqualByComparingTo("3200");
        assertThat(updated.getServiceCenter()).isEqualTo("Brake Masters");
    }

    @Test
    void deleteRecord() {
        ServiceRecord record = serviceRecordService.create(owner.getId(),
                TestData.service(car.getId(), ServiceType.AC_SERVICE, LocalDate.now(), 30_000, "1800"));

        serviceRecordService.delete(record.getId(), owner.getId());

        assertThat(serviceRecordService.countRecords(owner.getId())).isZero();
    }

    @Test
    void filterByTypeAndDateAndSumCosts() {
        LocalDate today = LocalDate.now();
        serviceRecordService.create(owner.getId(), TestData.service(car.getId(), ServiceType.OIL_CHANGE, today.minusMonths(8), 30_000, "2000"));
        serviceRecordService.create(owner.getId(), TestData.service(car.getId(), ServiceType.OIL_CHANGE, today.minusMonths(1), 31_000, "2500"));
        serviceRecordService.create(owner.getId(), TestData.service(car.getId(), ServiceType.GENERAL_SERVICE, today.minusMonths(2), 30_500, "6000"));

        List<ServiceRecord> oilChanges = serviceRecordService.search(owner.getId(), null, ServiceType.OIL_CHANGE,
                null, null, null, "date_desc");
        assertThat(oilChanges).hasSize(2);
        assertThat(oilChanges.get(0).getServiceDate()).isEqualTo(today.minusMonths(1));   // newest first
        assertThat(ServiceRecordService.sumCost(oilChanges)).isEqualByComparingTo("4500");

        List<ServiceRecord> lastQuarter = serviceRecordService.search(owner.getId(), car.getId(), null,
                today.minusMonths(3), today, null, "cost_desc");
        assertThat(lastQuarter).extracting(ServiceRecord::getCost)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("6000"), new BigDecimal("2500"));

        assertThat(serviceRecordService.getTotalServiceCost(owner.getId())).isEqualByComparingTo("10500");
    }

    @Test
    void strangerCannotSeeOrChangeRecords() {
        ServiceRecord record = serviceRecordService.create(owner.getId(),
                TestData.service(car.getId(), ServiceType.OIL_CHANGE, LocalDate.now(), 30_000, "2000"));

        assertThatThrownBy(() -> serviceRecordService.getRecord(record.getId(), stranger.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> serviceRecordService.delete(record.getId(), stranger.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
        // a stranger cannot attach a record to someone else's vehicle either
        assertThatThrownBy(() -> serviceRecordService.create(stranger.getId(),
                TestData.service(car.getId(), ServiceType.OIL_CHANGE, LocalDate.now(), 30_000, "1")))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(serviceRecordService.search(stranger.getId(), null, null, null, null, null, null)).isEmpty();
    }
}
