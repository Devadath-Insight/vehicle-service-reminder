package com.vehicleservice.controller;

import com.vehicleservice.model.ReminderStatus;
import com.vehicleservice.model.User;
import com.vehicleservice.service.ExpenseService;
import com.vehicleservice.service.ReminderService;
import com.vehicleservice.service.ReportService;
import com.vehicleservice.service.ServiceRecordService;
import com.vehicleservice.service.UserService;
import com.vehicleservice.service.VehicleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Dashboard (home page after login) and the Reports page. */
@Controller
public class DashboardController {

    private final UserService userService;
    private final VehicleService vehicleService;
    private final ServiceRecordService serviceRecordService;
    private final ReminderService reminderService;
    private final ExpenseService expenseService;
    private final ReportService reportService;

    public DashboardController(UserService userService, VehicleService vehicleService,
                               ServiceRecordService serviceRecordService, ReminderService reminderService,
                               ExpenseService expenseService, ReportService reportService) {
        this.userService = userService;
        this.vehicleService = vehicleService;
        this.serviceRecordService = serviceRecordService;
        this.reminderService = reminderService;
        this.expenseService = expenseService;
        this.reportService = reportService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        User user = userService.getCurrentUser(principal);
        Long userId = user.getId();

        Map<ReminderStatus, Long> statusCounts = reminderService.countByStatus(userId);
        Map<String, BigDecimal> monthly = reportService.getMonthlyExpense(userId, 6);

        model.addAttribute("vehicles", vehicleService.getVehicles(userId));
        model.addAttribute("totalVehicles", vehicleService.countVehicles(userId));
        model.addAttribute("upcomingCount",
                statusCounts.get(ReminderStatus.UPCOMING) + statusCounts.get(ReminderStatus.DUE_SOON));
        model.addAttribute("dueSoonCount", statusCounts.get(ReminderStatus.DUE_SOON));
        model.addAttribute("overdueCount", statusCounts.get(ReminderStatus.OVERDUE));
        model.addAttribute("totalExpense", reportService.getTotalMaintenanceExpense(userId));
        model.addAttribute("serviceCount", reportService.getServiceCount(userId));
        model.addAttribute("upcomingReminders", reminderService.getUpcoming(userId, 5));
        model.addAttribute("overdueReminders", reminderService.getOverdue(userId, 5));
        model.addAttribute("recentServices", serviceRecordService.getRecentRecords(userId));
        model.addAttribute("recentExpenses", expenseService.getRecentExpenses(userId));
        model.addAttribute("monthlyLabels", new ArrayList<>(monthly.keySet()));
        model.addAttribute("monthlyValues", new ArrayList<>(monthly.values()));
        return "dashboard";
    }

    @GetMapping("/reports")
    public String reports(Principal principal, Model model) {
        User user = userService.getCurrentUser(principal);
        Long userId = user.getId();

        Map<ReminderStatus, Long> statusCounts = reminderService.countByStatus(userId);
        Map<String, BigDecimal> monthly = reportService.getMonthlyExpense(userId, 12);
        Map<String, BigDecimal> byCategory = reportService.getExpenseByCategory(userId);
        Map<String, BigDecimal> byVehicle = reportService.getExpenseByVehicle(userId);
        Map<String, Long> byServiceType = reportService.getServicesByType(userId);

        model.addAttribute("totalMaintenance", reportService.getTotalMaintenanceExpense(userId));
        model.addAttribute("totalService", reportService.getTotalServiceCost(userId));
        model.addAttribute("totalOther", reportService.getTotalOtherExpenses(userId));
        model.addAttribute("serviceCount", reportService.getServiceCount(userId));
        model.addAttribute("upcomingCount",
                statusCounts.get(ReminderStatus.UPCOMING) + statusCounts.get(ReminderStatus.DUE_SOON));
        model.addAttribute("overdueCount", statusCounts.get(ReminderStatus.OVERDUE));

        model.addAttribute("expenseByVehicle", byVehicle);
        model.addAttribute("expenseByCategory", byCategory);
        model.addAttribute("servicesByType", byServiceType);

        model.addAttribute("monthlyLabels", new ArrayList<>(monthly.keySet()));
        model.addAttribute("monthlyValues", new ArrayList<>(monthly.values()));
        model.addAttribute("categoryLabels", new ArrayList<>(byCategory.keySet()));
        model.addAttribute("categoryValues", new ArrayList<>(byCategory.values()));
        model.addAttribute("serviceTypeLabels", new ArrayList<>(byServiceType.keySet()));
        model.addAttribute("serviceTypeValues", new ArrayList<>(byServiceType.values()));
        List<String> vehicleLabels = new ArrayList<>(byVehicle.keySet());
        model.addAttribute("vehicleLabels", vehicleLabels);
        model.addAttribute("vehicleValues", new ArrayList<>(byVehicle.values()));
        return "reports";
    }
}
