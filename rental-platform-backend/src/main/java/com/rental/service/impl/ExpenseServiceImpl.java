package com.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rental.common.SecurityUtil;
import com.rental.common.PageResult;
import com.rental.entity.Expense;
import com.rental.mapper.ExpenseMapper;
import com.rental.service.ExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ExpenseServiceImpl implements ExpenseService {
    private static final Logger log = LoggerFactory.getLogger(ExpenseServiceImpl.class);


    @Autowired
    private ExpenseMapper expenseMapper;

    @Override
    public Expense add(Expense expense) {
        Long userId = SecurityUtil.getCurrentUserId();
        expense.setUserId(userId);
        expenseMapper.insert(expense);
        log.info("费用记录添加成功: id={}, amount={}", expense.getId(), expense.getAmount());
        return expense;
    }

    @Override
    public void update(Expense expense) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        Expense existing = expenseMapper.selectById(expense.getId());
        if (existing == null) {
            throw new RuntimeException("费用记录不存在");
        }
        if (!existing.getUserId().equals(currentUserId)) {
            throw new RuntimeException("无权操作该费用记录");
        }
        expenseMapper.updateById(expense);
        log.info("费用记录更新成功: id={}", expense.getId());
    }

    @Override
    public void delete(Long id) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        Expense existing = expenseMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("费用记录不存在");
        }
        if (!existing.getUserId().equals(currentUserId)) {
            throw new RuntimeException("无权操作该费用记录");
        }
        expenseMapper.deleteById(id);
        log.info("费用记录删除成功: id={}", id);
    }

    @Override
    public PageResult<Expense> listByUser(long current, long size) {
        Long userId = SecurityUtil.getCurrentUserId();
        LambdaQueryWrapper<Expense> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Expense::getUserId, userId).orderByDesc(Expense::getCreatedAt);
        Page<Expense> page = expenseMapper.selectPage(new Page<>(current, size), wrapper);
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    @Override
    public List<Expense> listByContract(Long contractId) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        LambdaQueryWrapper<Expense> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Expense::getUserId, currentUserId)
                .eq(Expense::getContractId, contractId)
                .orderByDesc(Expense::getCreatedAt);
        return expenseMapper.selectList(wrapper);
    }

    @Override
    public void markPaid(Long id) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        Expense expense = expenseMapper.selectById(id);
        if (expense == null) {
            throw new RuntimeException("费用记录不存在");
        }
        if (!expense.getUserId().equals(currentUserId)) {
            throw new RuntimeException("无权操作该费用记录");
        }
        expense.setIsPaid(1);
        expense.setPaidDate(LocalDateTime.now());
        expenseMapper.updateById(expense);
        log.info("费用标记为已支付: id={}", id);
    }

}
