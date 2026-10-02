package com.vehicleservice.dto;

import com.vehicleservice.model.Expense;
import com.vehicleservice.model.ExpenseType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ExpenseForm {

    @NotNull(message = "Select a vehicle")
    private Long vehicleId;

    @NotNull(message = "Select an expense type")
    private ExpenseType expenseType;

    @NotNull(message = "Enter the amount")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Digits(integer = 8, fraction = 2, message = "Enter an amount like 1200 or 1200.50")
    private BigDecimal amount;

    @NotNull(message = "Enter the expense date")
    @PastOrPresent(message = "Expense date cannot be in the future")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expenseDate;

    @Size(max = 500, message = "Description must be at most 500 characters")
    private String description;

    public static ExpenseForm from(Expense expense) {
        ExpenseForm form = new ExpenseForm();
        form.setVehicleId(expense.getVehicle().getId());
        form.setExpenseType(expense.getExpenseType());
        form.setAmount(expense.getAmount());
        form.setExpenseDate(expense.getExpenseDate());
        form.setDescription(expense.getDescription());
        return form;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public ExpenseType getExpenseType() {
        return expenseType;
    }

    public void setExpenseType(ExpenseType expenseType) {
        this.expenseType = expenseType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
