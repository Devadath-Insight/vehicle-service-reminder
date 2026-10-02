package com.vehicleservice.controller;

import com.vehicleservice.dto.ServiceRecordForm;
import com.vehicleservice.model.ServiceRecord;
import com.vehicleservice.model.ServiceType;
import com.vehicleservice.model.User;
import com.vehicleservice.model.Vehicle;
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
import java.util.List;

/** Service record CRUD with search, filters and sorting. */
@Controller
@RequestMapping("/services")
public class ServiceRecordController {

    private final UserService userService;
    private final VehicleService vehicleService;
    private final ServiceRecordService serviceRecordService;

    public ServiceRecordController(UserService userService, VehicleService vehicleService,
                                   ServiceRecordService serviceRecordService) {
        this.userService = userService;
        this.vehicleService = vehicleService;
        this.serviceRecordService = serviceRecordService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) Long vehicleId,
                       @RequestParam(required = false) ServiceType serviceType,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       @RequestParam(required = false) String q,
                       @RequestParam(required = false, defaultValue = "date_desc") String sort,
                       Principal principal, Model model) {
        User user = userService.getCurrentUser(principal);
        List<ServiceRecord> records = serviceRecordService.search(user.getId(), vehicleId, serviceType, from, to, q, sort);

        model.addAttribute("records", records);
        model.addAttribute("filteredTotal", ServiceRecordService.sumCost(records));
        model.addAttribute("vehicles", vehicleService.getVehicles(user.getId()));
        model.addAttribute("serviceTypes", ServiceType.values());
        model.addAttribute("selectedVehicleId", vehicleId);
        model.addAttribute("selectedServiceType", serviceType);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("q", q);
        model.addAttribute("sort", sort);
        model.addAttribute("filtersApplied", vehicleId != null || serviceType != null || from != null
                || to != null || (q != null && !q.isBlank()));
        return "service-records";
    }

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long vehicleId, Principal principal, Model model,
                          RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser(principal);
        List<Vehicle> vehicles = vehicleService.getVehicles(user.getId());
        if (vehicles.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Add a vehicle before recording a service.");
            return "redirect:/vehicles/new";
        }
        ServiceRecordForm form = new ServiceRecordForm();
        form.setServiceDate(LocalDate.now());
        if (vehicleId != null) {
            vehicles.stream().filter(v -> v.getId().equals(vehicleId)).findFirst().ifPresent(v -> {
                form.setVehicleId(v.getId());
                form.setMileage(v.getCurrentMileage());
            });
        }
        prepareForm(model, form, vehicles, "/services", false);
        return "service-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("serviceForm") ServiceRecordForm form, BindingResult bindingResult,
                         Principal principal, Model model, RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser(principal);
        validateBusinessRules(form, bindingResult);
        if (bindingResult.hasErrors()) {
            prepareForm(model, form, vehicleService.getVehicles(user.getId()), "/services", false);
            return "service-form";
        }
        serviceRecordService.create(user.getId(), form);
        boolean reminderMade = form.isCreateReminder()
                && (form.getNextServiceDate() != null || form.getNextServiceMileage() != null);
        redirectAttributes.addFlashAttribute("successMessage",
                reminderMade ? "Service record added and a reminder was set for the next service."
                             : "Service record added.");
        return "redirect:/services";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Principal principal, Model model) {
        User user = userService.getCurrentUser(principal);
        ServiceRecord record = serviceRecordService.getRecord(id, user.getId());
        prepareForm(model, ServiceRecordForm.from(record), vehicleService.getVehicles(user.getId()),
                "/services/" + id, true);
        return "service-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("serviceForm") ServiceRecordForm form,
                         BindingResult bindingResult, Principal principal, Model model,
                         RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser(principal);
        serviceRecordService.getRecord(id, user.getId());   // ownership check
        form.setCreateReminder(false);                      // reminders are only auto-created for new records
        validateBusinessRules(form, bindingResult);
        if (bindingResult.hasErrors()) {
            prepareForm(model, form, vehicleService.getVehicles(user.getId()), "/services/" + id, true);
            return "service-form";
        }
        serviceRecordService.update(id, user.getId(), form);
        redirectAttributes.addFlashAttribute("successMessage", "Service record updated.");
        return "redirect:/services";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser(principal);
        serviceRecordService.delete(id, user.getId());
        redirectAttributes.addFlashAttribute("successMessage", "Service record deleted.");
        return "redirect:/services";
    }

    private void validateBusinessRules(ServiceRecordForm form, BindingResult bindingResult) {
        if (form.getNextServiceDate() != null && form.getServiceDate() != null
                && !form.getNextServiceDate().isAfter(form.getServiceDate())) {
            bindingResult.rejectValue("nextServiceDate", "order", "Next service date must be after the service date");
        }
        if (form.getNextServiceMileage() != null && form.getMileage() != null
                && form.getNextServiceMileage() <= form.getMileage()) {
            bindingResult.rejectValue("nextServiceMileage", "order",
                    "Next service mileage must be higher than the current reading");
        }
    }

    private void prepareForm(Model model, ServiceRecordForm form, List<Vehicle> vehicles,
                             String formAction, boolean editMode) {
        model.addAttribute("serviceForm", form);
        model.addAttribute("vehicles", vehicles);
        model.addAttribute("serviceTypes", ServiceType.values());
        model.addAttribute("formAction", formAction);
        model.addAttribute("editMode", editMode);
    }
}
