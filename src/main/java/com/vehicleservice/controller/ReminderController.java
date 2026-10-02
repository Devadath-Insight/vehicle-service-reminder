package com.vehicleservice.controller;

import com.vehicleservice.dto.ReminderForm;
import com.vehicleservice.model.Reminder;
import com.vehicleservice.model.ReminderStatus;
import com.vehicleservice.model.ReminderType;
import com.vehicleservice.model.User;
import com.vehicleservice.model.Vehicle;
import com.vehicleservice.service.ReminderService;
import com.vehicleservice.service.UserService;
import com.vehicleservice.service.VehicleService;
import jakarta.validation.Valid;
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
import java.util.List;

/** Reminder CRUD, filters, and mark-as-completed / reopen actions. */
@Controller
@RequestMapping("/reminders")
public class ReminderController {

    private final UserService userService;
    private final VehicleService vehicleService;
    private final ReminderService reminderService;

    public ReminderController(UserService userService, VehicleService vehicleService,
                              ReminderService reminderService) {
        this.userService = userService;
        this.vehicleService = vehicleService;
        this.reminderService = reminderService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) Long vehicleId,
                       @RequestParam(required = false) ReminderStatus status,
                       @RequestParam(required = false) ReminderType reminderType,
                       Principal principal, Model model) {
        User user = userService.getCurrentUser(principal);
        List<Reminder> reminders = reminderService.search(user.getId(), vehicleId, status, reminderType);

        model.addAttribute("reminders", reminders);
        model.addAttribute("statusCounts", reminderService.countByStatus(user.getId()));
        model.addAttribute("vehicles", vehicleService.getVehicles(user.getId()));
        model.addAttribute("statuses", ReminderStatus.values());
        model.addAttribute("reminderTypes", ReminderType.values());
        model.addAttribute("selectedVehicleId", vehicleId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedType", reminderType);
        model.addAttribute("filtersApplied", vehicleId != null || status != null || reminderType != null);
        return "reminders";
    }

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long vehicleId, Principal principal, Model model,
                          RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser(principal);
        List<Vehicle> vehicles = vehicleService.getVehicles(user.getId());
        if (vehicles.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Add a vehicle before creating a reminder.");
            return "redirect:/vehicles/new";
        }
        ReminderForm form = new ReminderForm();
        if (vehicleId != null && vehicles.stream().anyMatch(v -> v.getId().equals(vehicleId))) {
            form.setVehicleId(vehicleId);
        }
        prepareForm(model, form, vehicles, "/reminders", false);
        return "reminder-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("reminderForm") ReminderForm form, BindingResult bindingResult,
                         Principal principal, Model model, RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser(principal);
        validateBusinessRules(form, bindingResult);
        if (bindingResult.hasErrors()) {
            prepareForm(model, form, vehicleService.getVehicles(user.getId()), "/reminders", false);
            return "reminder-form";
        }
        Reminder reminder = reminderService.create(user.getId(), form);
        redirectAttributes.addFlashAttribute("successMessage",
                "Reminder created. Current status: " + reminder.getStatus().getDisplayName() + ".");
        return "redirect:/reminders";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Principal principal, Model model) {
        User user = userService.getCurrentUser(principal);
        Reminder reminder = reminderService.getReminder(id, user.getId());
        prepareForm(model, ReminderForm.from(reminder), vehicleService.getVehicles(user.getId()),
                "/reminders/" + id, true);
        return "reminder-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("reminderForm") ReminderForm form,
                         BindingResult bindingResult, Principal principal, Model model,
                         RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser(principal);
        reminderService.getReminder(id, user.getId());   // ownership check
        validateBusinessRules(form, bindingResult);
        if (bindingResult.hasErrors()) {
            prepareForm(model, form, vehicleService.getVehicles(user.getId()), "/reminders/" + id, true);
            return "reminder-form";
        }
        reminderService.update(id, user.getId(), form);
        redirectAttributes.addFlashAttribute("successMessage", "Reminder updated.");
        return "redirect:/reminders";
    }

    @PostMapping("/{id}/complete")
    public String complete(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser(principal);
        reminderService.markCompleted(id, user.getId());
        redirectAttributes.addFlashAttribute("successMessage", "Reminder marked as completed.");
        return "redirect:/reminders";
    }

    @PostMapping("/{id}/reopen")
    public String reopen(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser(principal);
        reminderService.reopen(id, user.getId());
        redirectAttributes.addFlashAttribute("successMessage", "Reminder reopened.");
        return "redirect:/reminders";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser(principal);
        reminderService.delete(id, user.getId());
        redirectAttributes.addFlashAttribute("successMessage", "Reminder deleted.");
        return "redirect:/reminders";
    }

    private void validateBusinessRules(ReminderForm form, BindingResult bindingResult) {
        if (form.getDueDate() == null && form.getDueMileage() == null) {
            bindingResult.rejectValue("dueDate", "required", "Enter a due date, a due mileage, or both");
        }
    }

    private void prepareForm(Model model, ReminderForm form, List<Vehicle> vehicles,
                             String formAction, boolean editMode) {
        model.addAttribute("reminderForm", form);
        model.addAttribute("vehicles", vehicles);
        model.addAttribute("reminderTypes", ReminderType.values());
        model.addAttribute("formAction", formAction);
        model.addAttribute("editMode", editMode);
    }
}
