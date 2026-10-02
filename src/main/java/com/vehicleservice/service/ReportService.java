package com.vehicleservice.service;

import com.vehicleservice.model.Expense;
import com.vehicleservice.model.ServiceRecord;
import com.vehicleservice.model.Vehicle;
import com.vehicleservice.repository.ExpenseRepository;
import com.vehicleservice.repository.ServiceRecordRepository;
import com.vehicleservice.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Statistics for the dashboard and reports page.
 *
 * "Maintenance expense" = cost of all service records + all entries on the Expenses page.
 * COLLECTIONS: uses List, Map, LinkedHashMap (keeps order for charts) and TreeMap (sorted months).
 */
@Service
@Transactional(readOnly = true)
public class ReportService {

    public static final String SERVICE_CATEGORY = "Service visits";
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM yyyy");

    private final ServiceRecordRepository serviceRecordRepository;
    private final ExpenseRepository expenseRepository;
    private final VehicleRepository vehicleRepository;

    public ReportService(ServiceRecordRepository serviceRecordRepository,
                         ExpenseRepository expenseRepository,
                         VehicleRepository vehicleRepository) {
        this.serviceRecordRepository = serviceRecordRepository;
        this.expenseRepository = expenseRepository;
        this.vehicleRepository = vehicleRepository;
    }

    public BigDecimal getTotalServiceCost(Long userId) {
        return serviceRecordRepository.sumCostByUserId(userId);
    }

    public BigDecimal getTotalOtherExpenses(Long userId) {
        return expenseRepository.sumAmountByUserId(userId);
    }

    public BigDecimal getTotalMaintenanceExpense(Long userId) {
        return getTotalServiceCost(userId).add(getTotalOtherExpenses(userId));
    }

    public long getServiceCount(Long userId) {
        return serviceRecordRepository.countByVehicleUserId(userId);
    }

    /** Registration number -> total spent (services + expenses), highest first. Includes vehicles with 0. */
    public Map<String, BigDecimal> getExpenseByVehicle(Long userId) {
        Map<Long, BigDecimal> totalsById = new HashMap<>();
        for (ServiceRecord record : serviceRecordRepository.findByVehicleUserId(userId)) {
            totalsById.merge(record.getVehicle().getId(), record.getCost(), BigDecimal::add);
        }
        for (Expense expense : expenseRepository.findByVehicleUserId(userId)) {
            totalsById.merge(expense.getVehicle().getId(), expense.getAmount(), BigDecimal::add);
        }
        Map<String, BigDecimal> byVehicle = new HashMap<>();
        for (Vehicle vehicle : vehicleRepository.findByUserIdOrderByRegistrationNumberAsc(userId)) {
            byVehicle.put(vehicle.getRegistrationNumber(), totalsById.getOrDefault(vehicle.getId(), BigDecimal.ZERO));
        }
        return sortByValueDescending(byVehicle);
    }

    /** Category -> total. Service records form one category; expenses are grouped by their type. */
    public Map<String, BigDecimal> getExpenseByCategory(Long userId) {
        Map<String, BigDecimal> byCategory = new HashMap<>();
        BigDecimal serviceTotal = getTotalServiceCost(userId);
        if (serviceTotal.signum() > 0) {
            byCategory.put(SERVICE_CATEGORY, serviceTotal);
        }
        for (Expense expense : expenseRepository.findByVehicleUserId(userId)) {
            byCategory.merge(expense.getExpenseType().getDisplayName(), expense.getAmount(), BigDecimal::add);
        }
        return sortByValueDescending(byCategory);
    }

    /** Month label ("Mar 2026") -> total for the last {@code months} months, oldest first, including empty months. */
    public Map<String, BigDecimal> getMonthlyExpense(Long userId, int months) {
        YearMonth current = YearMonth.now();
        YearMonth first = current.minusMonths(months - 1L);
        TreeMap<YearMonth, BigDecimal> totals = new TreeMap<>();
        for (int i = 0; i < months; i++) {
            totals.put(first.plusMonths(i), BigDecimal.ZERO);
        }
        for (ServiceRecord record : serviceRecordRepository.findByVehicleUserId(userId)) {
            addIfInRange(totals, record.getServiceDate(), record.getCost());
        }
        for (Expense expense : expenseRepository.findByVehicleUserId(userId)) {
            addIfInRange(totals, expense.getExpenseDate(), expense.getAmount());
        }
        Map<String, BigDecimal> labelled = new LinkedHashMap<>();
        totals.forEach((month, total) -> labelled.put(month.format(MONTH_LABEL), total));
        return labelled;
    }

    /** Service type -> number of services, most frequent first. */
    public Map<String, Long> getServicesByType(Long userId) {
        Map<String, Long> counts = serviceRecordRepository.findByVehicleUserId(userId).stream()
                .collect(Collectors.groupingBy(r -> r.getServiceType().getDisplayName(), Collectors.counting()));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    private static void addIfInRange(TreeMap<YearMonth, BigDecimal> totals, LocalDate date, BigDecimal amount) {
        if (date == null || amount == null) {
            return;
        }
        YearMonth month = YearMonth.from(date);
        if (totals.containsKey(month)) {
            totals.merge(month, amount, BigDecimal::add);
        }
    }

    private static Map<String, BigDecimal> sortByValueDescending(Map<String, BigDecimal> map) {
        List<Map.Entry<String, BigDecimal>> entries = map.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .toList();
        Map<String, BigDecimal> sorted = new LinkedHashMap<>();
        entries.forEach(e -> sorted.put(e.getKey(), e.getValue()));
        return sorted;
    }
}
