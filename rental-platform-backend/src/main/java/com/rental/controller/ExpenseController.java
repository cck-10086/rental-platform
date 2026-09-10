package com.rental.controller;

import com.rental.common.Result;
import com.rental.entity.Expense;
import com.rental.service.ExpenseService;
import com.rental.common.PageResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expense")
public class ExpenseController {

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    private final ExpenseService expenseService;

    @PostMapping("/add")
    public Result<?> add(@Valid @RequestBody Expense expense) {
        expenseService.add(expense);
        return Result.success();
    }

    @PutMapping("/update")
    public Result<?> update(@Valid @RequestBody Expense expense) {
        expenseService.update(expense);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        expenseService.delete(id);
        return Result.success();
    }

    @GetMapping("/list")
    public Result<PageResult<Expense>> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(expenseService.listByUser(current, size));
    }

    @GetMapping("/list/{contractId}")
    public Result<List<Expense>> listByContract(@PathVariable Long contractId) {
        return Result.success(expenseService.listByContract(contractId));
    }

    @PutMapping("/mark-paid/{id}")
    public Result<?> markPaid(@PathVariable Long id) {
        expenseService.markPaid(id);
        return Result.success();
    }
}
