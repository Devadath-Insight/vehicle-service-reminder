package com.vehicleservice.service;

import com.vehicleservice.TestData;
import com.vehicleservice.dto.ExpenseForm;
import com.vehicleservice.exception.ResourceNotFoundException;
import com.vehicleservice.model.Expense;
import com.vehicleservice.model.ExpenseType;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Expense CRUD plus the calculations shown on the dashboard and reports page. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ExpenseAndReportServiceTest {

    @Autowired private UserService userService;
    @Autowired private VehicleService vehicleService;
    @Autowired private ServiceRecordService serviceRecordService;
    @Autowired private ExpenseService expenseService;
    @Autowired private ReportService reportService;

    private User owner;
    private User stranger;
    private Vehicle car;
    private Vehicle scooter;

    @BeforeEach
    void setUp() {
        owner = userService.register(TestData.registration("exp-owner@example.com"));
        stranger = userService.register(TestData.registration("exp-stranger@example.com"));
        car = vehicleService.create(owner, TestData.vehicle("KL01AB1234", 30_000));
        scooter = vehicleService.create(owner, TestData.vehicle("KL07CD4521", 10_000));
    }

    @Test
    void expenseCrud() {
        Expense expense = expenseService.create(owner.getId(),
                TestData.expense(car.getId(), ExpenseType.FUEL, "3200.00", LocalDate.now()));
        assertThat(expenseService.getExpense(expense.getId(), owner.getId()).getAmount()).isEqualByComparingTo("3200");

        ExpenseForm form = ExpenseForm.from(expense);
        form.setAmount(new BigDecimal("3350.75"));
        form.setExpenseType(ExpenseType.REPAIR);
        expenseService.update(expense.getId(), owner.getId(), form);

        Expense updated = expenseService.getExpense(expense.getId(), owner.getId());
        assertThat(updated.getAmount()).isEqualByComparingTo("3350.75");
        assertThat(updated.getExpenseType()).isEqualTo(ExpenseType.REPAIR);

        expenseService.delete(expense.getId(), owner.getId());
        assertThat(expenseService.search(owner.getId(), null, null, null, null, null)).isEmpty();
    }

    @Test
    void filtersAndTotals() {
        LocalDate today = LocalDate.now();
        expenseService.create(owner.getId(), TestData.expense(car.getId(), ExpenseType.FUEL, "1000.50", today.minusDays(40)));
        expenseService.create(owner.getId(), TestData.expense(car.getId(), ExpenseType.FUEL, "2000.25", today.minusDays(5)));
        expenseService.create(owner.getId(), TestData.expense(scooter.getId(), ExpenseType.INSURANCE, "1850", today.minusDays(3)));

        List<Expense> fuel = expenseService.search(owner.getId(), null, ExpenseType.FUEL, null, null, null);
        assertThat(fuel).hasSize(2);
        assertThat(ExpenseService.sumAmount(fuel)).isEqualByComparingTo("3000.75");

        List<Expense> recentForCar = expenseService.search(owner.getId(), car.getId(), null, today.minusDays(10), today, null);
        assertThat(recentForCar).hasSize(1);

        assertThat(expenseService.getTotalExpenses(owner.getId())).isEqualByComparingTo("4850.75");
    }

    @Test
    void reportTotalsCombineServicesAndExpenses() {
        LocalDate today = LocalDate.now();
        serviceRecordService.create(owner.getId(),
                TestData.service(car.getId(), ServiceType.GENERAL_SERVICE, today.minusDays(2), 30_100, "6000"));
        serviceRecordService.create(owner.getId(),
                TestData.service(scooter.getId(), ServiceType.OIL_CHANGE, today.minusDays(1), 10_050, "500"));
        expenseService.create(owner.getId(), TestData.expense(car.getId(), ExpenseType.FUEL, "3000", today));
        expenseService.create(owner.getId(), TestData.expense(scooter.getId(), ExpenseType.INSURANCE, "1500", today));

        assertThat(reportService.getTotalServiceCost(owner.getId())).isEqualByComparingTo("6500");
        assertThat(reportService.getTotalOtherExpenses(owner.getId())).isEqualByComparingTo("4500");
        assertThat(reportService.getTotalMaintenanceExpense(owner.getId())).isEqualByComparingTo("11000");
        assertThat(reportService.getServiceCount(owner.getId())).isEqualTo(2);

        Map<String, BigDecimal> byVehicle = reportService.getExpenseByVehicle(owner.getId());
        assertThat(byVehicle.get("KL01AB1234")).isEqualByComparingTo("9000");
        assertThat(byVehicle.get("KL07CD4521")).isEqualByComparingTo("2000");
        assertThat(byVehicle.keySet()).first().isEqualTo("KL01AB1234");   // highest first

        Map<String, BigDecimal> byCategory = reportService.getExpenseByCategory(owner.getId());
        assertThat(byCategory.get(ReportService.SERVICE_CATEGORY)).isEqualByComparingTo("6500");
        assertThat(byCategory.get("Fuel")).isEqualByComparingTo("3000");
        assertThat(byCategory.get("Insurance")).isEqualByComparingTo("1500");

        Map<String, BigDecimal> monthly = reportService.getMonthlyExpense(owner.getId(), 6);
        assertThat(monthly).hasSize(6);
        BigDecimal sixMonthTotal = monthly.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sixMonthTotal).isEqualByComparingTo("11000");

        Map<String, Long> byType = reportService.getServicesByType(owner.getId());
        assertThat(byType).containsEntry("General Service", 1L).containsEntry("Oil Change", 1L);
    }

    @Test
    void emptyAccountHasZeroTotals() {
        assertThat(reportService.getTotalMaintenanceExpense(stranger.getId())).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(reportService.getExpenseByCategory(stranger.getId())).isEmpty();
        assertThat(reportService.getExpenseByVehicle(stranger.getId())).isEmpty();
    }

    @Test
    void strangerCannotAccessOrAddExpenses() {
        Expense expense = expenseService.create(owner.getId(),
                TestData.expense(car.getId(), ExpenseType.FUEL, "500", LocalDate.now()));

        assertThatThrownBy(() -> expenseService.getExpense(expense.getId(), stranger.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> expenseService.create(stranger.getId(),
                TestData.expense(car.getId(), ExpenseType.FUEL, "1", LocalDate.now())))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(reportService.getTotalOtherExpenses(stranger.getId())).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
