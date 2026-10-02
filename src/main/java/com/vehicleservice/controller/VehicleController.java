package com.vehicleservice.controller;

import com.vehicleservice.dto.VehicleForm;
import com.vehicleservice.model.Expense;
import com.vehicleservice.model.FuelType;
import com.vehicleservice.model.Reminder;
import com.vehicleservice.model.ServiceRecord;
import com.vehicleservice.model.ServiceType;
import com.vehicleservice.model.User;
import com.vehicleservice.model.Vehicle;
import com.vehicleservice.service.ExpenseService;
import com.vehicleservice.service.ReminderService;
import com.vehicleservice.service.ServiceRecordService;
import com.vehicleservice.service.UserService;
import com.vehicleservice.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;

/** Vehicle CRUD, search and the per-vehicle service history page. */
@Controller
@RequestMapping("/vehicles")
public class VehicleController {

    private final UserService userService;
    private final VehicleService vehicleService;
    private final ServiceRecordService serviceRecordService;
    private final ReminderService reminderService;
    private final ExpenseService expenseService;

    public VehicleController(UserService userService, VehicleService vehicleService,
                             ServiceRecordService serviceRecordService, ReminderService reminderService,
                             ExpenseService expenseService) {
        this.userService = userService;
        this.vehicleService = vehicleService;
        this.serviceRecordService = serviceRecordService;
        this.reminderService = reminderService;
        this.expenseService = expenseService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q, Principal principal, Model model) {
        User user = userService.getCurrentUser(principal);
        model.addAttribute("vehicles", vehicleService.searchVehicles(user.getId(), q));
        model.addAttribute("totalVehicles", vehicleService.countVehicles(user.getId()));
        model.addAttribute("q", q);
        return "vehicles";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        prepareForm(model, new VehicleForm(), "/vehicles", false);
        return "vehicle-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("vehicleForm") VehicleForm form, BindingResult bindingResult,
                         Principal principal, Model model, RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser(principal);
        validateBusinessRules(form, bindingResult, user.getId(), null);
        if (bindingResult.hasErrors()) {
            prepareForm(model, form, "/vehicles", false);
            return "vehicle-form";
        }
        Vehicle vehicle = vehicleService.create(user, form);
        redirectAttributes.addFlashAttribute("successMessage", "Vehicle " + vehicle.getRegistrationNumber() + " added.");
        return "redirect:/vehicles/" + vehicle.getId();
    }

    /** Vehicle details + complete service history (with optional filters). */
    @GetMapping("/{id}")
    public String details(@PathVariable Long id,
                          @RequestParam(required = false) ServiceType serviceType,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                          Principal principal, Model model) {
        User user = userService.getCurrentUser(principal);
        Vehicle vehicle = vehicleService.getVehicle(id, user.getId());   // ownership check
        List<ServiceRecord> history = serviceRecordService.getVehicleHistory(id, user.getId(), serviceType, from, to);
        List<Reminder> reminders = reminderService.search(user.getId(), id, null, null);
        List<Expense> expenses = expenseService.search(user.getId(), id, null, null, null, null);

        model.addAttribute("vehicle", vehicle);
        model.addAttribute("history", history);
        model.addAttribute("historyTotal", ServiceRecordService.sumCost(history));
        model.addAttribute("reminders", reminders);
        model.addAttribute("activeReminderCount", reminders.stream().filter(r -> r.getStatus().isActive()).count());
        model.addAttribute("expenseTotal", ExpenseService.sumAmount(expenses));
        model.addAttribute("serviceTypes", ServiceType.values());
        model.addAttribute("selectedServiceType", serviceType);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("filtersApplied", serviceType != null || from != null || to != null);
        return "vehicle-details";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Principal principal, Model model) {
        User user = userService.getCurrentUser(principal);
        Vehicle vehicle = vehicleService.getVehicle(id, user.getId());
        prepareForm(model, VehicleForm.from(vehicle), "/vehicles/" + id, true);
        model.addAttribute("vehicleId", id);
        return "vehicle-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("vehicleForm") VehicleForm form,
                         BindingResult bindingResult, Principal principal, Model model,
                         RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser(principal);
        vehicleService.getVehicle(id, user.getId());   // ownership check before anything else
        validateBusinessRules(form, bindingResult, user.getId(), id);
        if (bindingResult.hasErrors()) {
            prepareForm(model, form, "/vehicles/" + id, true);
            model.addAttribute("vehicleId", id);
            return "vehicle-form";
        }
        vehicleService.update(id, user.getId(), form);
        redirectAttributes.addFlashAttribute("successMessage", "Vehicle updated.");
        return "redirect:/vehicles/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser(principal);
        String registration = vehicleService.getVehicle(id, user.getId()).getRegistrationNumber();
        vehicleService.delete(id, user.getId());
        redirectAttributes.addFlashAttribute("successMessage",
                "Vehicle " + registration + " and all its records were deleted.");
        return "redirect:/vehicles";
    }

    private void validateBusinessRules(VehicleForm form, BindingResult bindingResult, Long userId, Long vehicleId) {
        if (!bindingResult.hasFieldErrors("registrationNumber")
                && vehicleService.isRegistrationTaken(userId, form.getRegistrationNumber(), vehicleId)) {
            bindingResult.rejectValue("registrationNumber", "duplicate",
                    "You already have a vehicle with this registration number");
        }
        if (!bindingResult.hasFieldErrors("year") && form.getYear() != null
                && form.getYear() > Year.now().getValue() + 1) {
            bindingResult.rejectValue("year", "future", "Year cannot be later than next year");
        }
    }

    private void prepareForm(Model model, VehicleForm form, String formAction, boolean editMode) {
        model.addAttribute("vehicleForm", form);
        model.addAttribute("formAction", formAction);
        model.addAttribute("editMode", editMode);
        model.addAttribute("fuelTypes", FuelType.values());
    }
}
