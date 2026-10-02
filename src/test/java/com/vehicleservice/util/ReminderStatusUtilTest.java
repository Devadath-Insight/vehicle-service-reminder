package com.vehicleservice.util;

import com.vehicleservice.model.Reminder;
import com.vehicleservice.model.ReminderStatus;
import com.vehicleservice.model.Vehicle;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plain unit tests (no Spring, no database) for the automatic reminder status rules.
 * A fixed "today" is used so results never depend on the day the tests run.
 */
class ReminderStatusUtilTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);

    // ---------- date based ----------

    @Test
    void dateInThePastIsOverdue() {
        assertThat(ReminderStatusUtil.statusByDate(TODAY.minusDays(1), TODAY)).isEqualTo(ReminderStatus.OVERDUE);
    }

    @Test
    void dateTodayIsDueSoon() {
        assertThat(ReminderStatusUtil.statusByDate(TODAY, TODAY)).isEqualTo(ReminderStatus.DUE_SOON);
    }

    @Test
    void dateWithin30DaysIsDueSoon() {
        assertThat(ReminderStatusUtil.statusByDate(TODAY.plusDays(30), TODAY)).isEqualTo(ReminderStatus.DUE_SOON);
    }

    @Test
    void dateMoreThan30DaysAwayIsUpcoming() {
        assertThat(ReminderStatusUtil.statusByDate(TODAY.plusDays(31), TODAY)).isEqualTo(ReminderStatus.UPCOMING);
    }

    // ---------- mileage based ----------

    @Test
    void mileageReachedIsOverdue() {
        assertThat(ReminderStatusUtil.statusByMileage(40_000, 40_000)).isEqualTo(ReminderStatus.OVERDUE);
        assertThat(ReminderStatusUtil.statusByMileage(40_000, 41_250)).isEqualTo(ReminderStatus.OVERDUE);
    }

    @Test
    void mileageWithin500KmIsDueSoon() {
        assertThat(ReminderStatusUtil.statusByMileage(40_000, 39_500)).isEqualTo(ReminderStatus.DUE_SOON);
        assertThat(ReminderStatusUtil.statusByMileage(40_000, 39_999)).isEqualTo(ReminderStatus.DUE_SOON);
    }

    @Test
    void mileageFarAwayIsUpcoming() {
        assertThat(ReminderStatusUtil.statusByMileage(40_000, 39_499)).isEqualTo(ReminderStatus.UPCOMING);
    }

    // ---------- both ----------

    @Test
    void whenBothAreSetTheMostUrgentWins() {
        // date is far away (UPCOMING) but mileage already passed (OVERDUE)
        assertThat(ReminderStatusUtil.calculateStatus(TODAY.plusMonths(6), 10_000, 10_200, TODAY))
                .isEqualTo(ReminderStatus.OVERDUE);
        // date due soon, mileage far away
        assertThat(ReminderStatusUtil.calculateStatus(TODAY.plusDays(10), 50_000, 10_000, TODAY))
                .isEqualTo(ReminderStatus.DUE_SOON);
    }

    @Test
    void mileageIsIgnoredWhenCurrentMileageUnknown() {
        assertThat(ReminderStatusUtil.calculateStatus(null, 10_000, null, TODAY)).isEqualTo(ReminderStatus.UPCOMING);
    }

    @Test
    void completedReminderStaysCompleted() {
        Vehicle vehicle = new Vehicle();
        vehicle.setCurrentMileage(99_000);
        Reminder reminder = new Reminder();
        reminder.setVehicle(vehicle);
        reminder.setDueDate(TODAY.minusYears(1));
        reminder.setDueMileage(10_000);
        reminder.setStatus(ReminderStatus.COMPLETED);

        assertThat(ReminderStatusUtil.resolveStatus(reminder, TODAY)).isEqualTo(ReminderStatus.COMPLETED);
    }

    @Test
    void resolveStatusUsesTheVehiclesCurrentMileage() {
        Vehicle vehicle = new Vehicle();
        vehicle.setCurrentMileage(19_800);
        Reminder reminder = new Reminder();
        reminder.setVehicle(vehicle);
        reminder.setDueMileage(20_000);

        assertThat(ReminderStatusUtil.resolveStatus(reminder, TODAY)).isEqualTo(ReminderStatus.DUE_SOON);
    }
}
