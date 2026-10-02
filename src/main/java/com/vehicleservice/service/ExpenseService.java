package com.vehicleservice.service;

import com.vehicleservice.dto.ExpenseForm;
import com.vehicleservice.exception.ResourceNotFoundException;
import com.vehicleservice.model.Expense;
import com.vehicleservice.model.ExpenseType;
import com.vehicleservice.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final VehicleService vehicleService;

    public ExpenseService(ExpenseRepository expenseRepository, VehicleService vehicleService) {
        this.expenseRepository = expenseRepository;
        this.vehicleService = vehicleService;
    }

    @Transactional(readOnly = true)
    public List<Expense> search(Long userId, Long vehicleId, ExpenseType expenseType,
                                LocalDate fromDate, LocalDate toDate, String query) {
        String q = (query == null || query.isBlank()) ? null : query.trim();
        return expenseRepository.search(userId, vehicleId, expenseType, fromDate, toDate, q);
    }

    @Transactional(readOnly = true)
    public Expense getExpense(Long expenseId, Long userId) {
        return expenseRepository.findByIdAndVehicleUserId(expenseId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", expenseId));
    }

    @Transactional(readOnly = true)
    public List<Expense> getRecentExpenses(Long userId) {
        return expenseRepository.findTop5ByVehicleUserIdOrderByExpenseDateDescIdDesc(userId);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalExpenses(Long userId) {
        return expenseRepository.sumAmountByUserId(userId);
    }

    public Expense create(Long userId, ExpenseForm form) {
        Expense expense = new Expense();
        expense.setVehicle(vehicleService.getVehicle(form.getVehicleId(), userId));
        applyForm(expense, form);
        return expenseRepository.save(expense);
    }

    public Expense update(Long expenseId, Long userId, ExpenseForm form) {
        Expense expense = getExpense(expenseId, userId);
        expense.setVehicle(vehicleService.getVehicle(form.getVehicleId(), userId));
        applyForm(expense, form);
        return expense;
    }

    public void delete(Long expenseId, Long userId) {
        expenseRepository.delete(getExpense(expenseId, userId));
    }

    public static BigDecimal sumAmount(List<Expense> expenses) {
        return expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void applyForm(Expense expense, ExpenseForm form) {
        expense.setExpenseType(form.getExpenseType());
        expense.setAmount(form.getAmount());
        expense.setExpenseDate(form.getExpenseDate());
        expense.setDescription(form.getDescription() == null || form.getDescription().isBlank()
                ? null : form.getDescription().trim());
    }
}
