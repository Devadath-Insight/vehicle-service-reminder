package com.vehicleservice.service;

import com.vehicleservice.TestData;
import com.vehicleservice.dto.ReminderForm;
import com.vehicleservice.exception.ResourceNotFoundException;
import com.vehicleservice.model.Reminder;
import com.vehicleservice.model.ReminderStatus;
import com.vehicleservice.model.ReminderType;
import com.vehicleservice.model.User;
import com.vehicleservice.model.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Reminder CRUD with automatic status calculation, completion and isolation. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReminderServiceTest {

    @Autowired private UserService userService;
    @Autowired private VehicleService vehicleService;
    @Autowired private ReminderService reminderService;

    private User owner;
    private User stranger;
    private Vehicle car;

    @BeforeEach
    void setUp() {
        owner = userService.register(TestData.registration("rem-owner@example.com"));
        stranger = userService.register(TestData.registration("rem-stranger@example.com"));
        car = vehicleService.create(owner, TestData.vehicle("KL01AB1234", 40_000));
    }

    @Test
    void statusIsCalculatedOnCreate() {
        LocalDate today = LocalDate.now();
        Reminder overdue = reminderService.create(owner.getId(),
                TestData.reminder(car.getId(), ReminderType.POLLUTION_CERTIFICATE, today.minusDays(2), null));
        Reminder dueSoon = reminderService.create(owner.getId(),
                TestData.reminder(car.getId(), ReminderType.INSURANCE, today.plusDays(10), null));
        Reminder upcoming = reminderService.create(owner.getId(),
                TestData.reminder(car.getId(), ReminderType.REGISTRATION, today.plusMonths(6), null));
        Reminder mileageOverdue = reminderService.create(owner.getId(),
                TestData.reminder(car.getId(), ReminderType.OIL_CHANGE, null, 39_000));
        Reminder mileageDueSoon = reminderService.create(owner.getId(),
                TestData.reminder(car.getId(), ReminderType.TYRE_REPLACEMENT, null, 40_300));

        assertThat(overdue.getStatus()).isEqualTo(ReminderStatus.OVERDUE);
        assertThat(dueSoon.getStatus()).isEqualTo(ReminderStatus.DUE_SOON);
        assertThat(upcoming.getStatus()).isEqualTo(ReminderStatus.UPCOMING);
        assertThat(mileageOverdue.getStatus()).isEqualTo(ReminderStatus.OVERDUE);
        assertThat(mileageDueSoon.getStatus()).isEqualTo(ReminderStatus.DUE_SOON);
    }

    @Test
    void listIsSortedMostUrgentFirstAndCountsAreCorrect() {
        LocalDate today = LocalDate.now();
        reminderService.create(owner.getId(), TestData.reminder(car.getId(), ReminderType.REGISTRATION, today.plusMonths(6), null));
        reminderService.create(owner.getId(), TestData.reminder(car.getId(), ReminderType.INSURANCE, today.plusDays(5), null));
        reminderService.create(owner.getId(), TestData.reminder(car.getId(), ReminderType.POLLUTION_CERTIFICATE, today.minusDays(1), null));

        List<Reminder> all = reminderService.search(owner.getId(), null, null, null);
        assertThat(all).extracting(Reminder::getStatus)
                .containsExactly(ReminderStatus.OVERDUE, ReminderStatus.DUE_SOON, ReminderStatus.UPCOMING);

        Map<ReminderStatus, Long> counts = reminderService.countByStatus(owner.getId());
        assertThat(counts.get(ReminderStatus.OVERDUE)).isEqualTo(1L);
        assertThat(counts.get(ReminderStatus.DUE_SOON)).isEqualTo(1L);
        assertThat(counts.get(ReminderStatus.UPCOMING)).isEqualTo(1L);
        assertThat(counts.get(ReminderStatus.COMPLETED)).isZero();

        assertThat(reminderService.getOverdue(owner.getId(), 5)).hasSize(1);
        assertThat(reminderService.getUpcoming(owner.getId(), 5)).hasSize(2);
        assertThat(reminderService.search(owner.getId(), null, ReminderStatus.OVERDUE, null)).hasSize(1);
        assertThat(reminderService.search(owner.getId(), null, null, ReminderType.INSURANCE)).hasSize(1);
    }

    @Test
    void updateRecalculatesStatus() {
        Reminder reminder = reminderService.create(owner.getId(),
                TestData.reminder(car.getId(), ReminderType.INSURANCE, LocalDate.now().plusMonths(3), null));
        assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.UPCOMING);

        ReminderForm form = ReminderForm.from(reminder);
        form.setDueDate(LocalDate.now().minusDays(3));
        reminderService.update(reminder.getId(), owner.getId(), form);

        assertThat(reminderService.getReminder(reminder.getId(), owner.getId()).getStatus())
                .isEqualTo(ReminderStatus.OVERDUE);
    }

    @Test
    void completeAndReopen() {
        Reminder reminder = reminderService.create(owner.getId(),
                TestData.reminder(car.getId(), ReminderType.POLLUTION_CERTIFICATE, LocalDate.now().minusDays(1), null));

        reminderService.markCompleted(reminder.getId(), owner.getId());
        assertThat(reminderService.getReminder(reminder.getId(), owner.getId()).getStatus())
                .isEqualTo(ReminderStatus.COMPLETED);

        reminderService.reopen(reminder.getId(), owner.getId());
        assertThat(reminderService.getReminder(reminder.getId(), owner.getId()).getStatus())
                .isEqualTo(ReminderStatus.OVERDUE);
    }

    @Test
    void deleteReminder() {
        Reminder reminder = reminderService.create(owner.getId(),
                TestData.reminder(car.getId(), ReminderType.OTHER, LocalDate.now().plusDays(90), null));

        reminderService.delete(reminder.getId(), owner.getId());

        assertThat(reminderService.search(owner.getId(), null, null, null)).isEmpty();
    }

    @Test
    void strangerCannotAccessReminder() {
        Reminder reminder = reminderService.create(owner.getId(),
                TestData.reminder(car.getId(), ReminderType.INSURANCE, LocalDate.now().plusDays(20), null));

        assertThatThrownBy(() -> reminderService.getReminder(reminder.getId(), stranger.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> reminderService.markCompleted(reminder.getId(), stranger.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(reminderService.search(stranger.getId(), null, null, null)).isEmpty();
    }
}
