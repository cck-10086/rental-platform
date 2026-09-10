package com.rental.service;

import com.rental.entity.Expense;
import com.rental.common.PageResult;
import java.util.List;

public interface ExpenseService {
    Expense add(Expense expense);
    void update(Expense expense);
    void delete(Long id);
    PageResult<Expense> listByUser(long current, long size);
    List<Expense> listByContract(Long contractId);
    void markPaid(Long id);
}
