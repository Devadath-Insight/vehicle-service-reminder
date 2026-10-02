package com.vehicleservice.controller;

import com.vehicleservice.dto.ExpenseForm;
import com.vehicleservice.model.Expense;
import com.vehicleservice.model.ExpenseType;
import com.vehicleservice.model.User;
import com.vehicleservice.model.Vehicle;
import com.vehicleservice.service.ExpenseService;
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

/** Expense CRUD with filters. */
@Controller
@RequestMapping("/expenses")
public class ExpenseController {

    private final UserService userService;
    private final VehicleService vehicleService;
    private final ExpenseService expenseService;

    public ExpenseController(UserService userService, VehicleService vehicleService, ExpenseService expenseService) {
        this.userService = userService;
        this.vehicleService = vehicleService;
        this.expenseService = expenseService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) Long vehicleId,
                       @RequestParam(required = false) ExpenseType expenseType,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       @RequestParam(required = false) String q,
                       Principal principal, Model model) {
        User user = userService.getCurrentUser(principal);
        List<Expense> expenses = expenseService.search(user.getId(), vehicleId, expenseType, from, to, q);

        model.addAttribute("expenses", expenses);
        model.addAttribute("filteredTotal", ExpenseService.sumAmount(expenses));
        model.addAttribute("vehicles", vehicleService.getVehicles(user.getId()));
        model.addAttribute("expenseTypes", ExpenseType.values());
        model.addAttribute("selectedVehicleId", vehicleId);
        model.addAttribute("selectedType", expenseType);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("q", q);
        model.addAttribute("filtersApplied", vehicleId != null || expenseType != null || from != null
                || to != null || (q != null && !q.isBlank()));
        return "expenses";
    }

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long vehicleId, Principal principal, Model model,
                          RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser(principal);
        List<Vehicle> vehicles = vehicleService.getVehicles(user.getId());
        if (vehicles.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Add a vehicle before recording an expense.");
            return "redirect:/vehicles/new";
        }
        ExpenseForm form = new ExpenseForm();
        form.setExpenseDate(LocalDate.now());
        if (vehicleId != null && vehicles.stream().anyMatch(v -> v.getId().equals(vehicleId))) {
            form.setVehicleId(vehicleId);
        }
        prepareForm(model, form, vehicles, "/expenses", false);
        return "expense-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("expenseForm") ExpenseForm form, BindingResult bindingResult,
                         Principal principal, Model model, RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser(principal);
        if (bindingResult.hasErrors()) {
            prepareForm(model, form, vehicleService.getVehicles(user.getId()), "/expenses", false);
            return "expense-form";
        }
        expenseService.create(user.getId(), form);
        redirectAttributes.addFlashAttribute("successMessage", "Expense added.");
        return "redirect:/expenses";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Principal principal, Model model) {
        User user = userService.getCurrentUser(principal);
        Expense expense = expenseService.getExpense(id, user.getId());
        prepareForm(model, ExpenseForm.from(expense), vehicleService.getVehicles(user.getId()),
                "/expenses/" + id, true);
        return "expense-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("expenseForm") ExpenseForm form,
                         BindingResult bindingResult, Principal principal, Model model,
                         RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser(principal);
        expenseService.getExpense(id, user.getId());   // ownership check
        if (bindingResult.hasErrors()) {
            prepareForm(model, form, vehicleService.getVehicles(user.getId()), "/expenses/" + id, true);
            return "expense-form";
        }
        expenseService.update(id, user.getId(), form);
        redirectAttributes.addFlashAttribute("successMessage", "Expense updated.");
        return "redirect:/expenses";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser(principal);
        expenseService.delete(id, user.getId());
        redirectAttributes.addFlashAttribute("successMessage", "Expense deleted.");
        return "redirect:/expenses";
    }

    private void prepareForm(Model model, ExpenseForm form, List<Vehicle> vehicles,
                             String formAction, boolean editMode) {
        model.addAttribute("expenseForm", form);
        model.addAttribute("vehicles", vehicles);
        model.addAttribute("expenseTypes", ExpenseType.values());
        model.addAttribute("formAction", formAction);
        model.addAttribute("editMode", editMode);
    }
}
