package com.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rental.common.PageResult;
import com.rental.common.SecurityUtil;
import com.rental.entity.Contract;
import com.rental.entity.Expense;
import com.rental.entity.House;
import com.rental.entity.MoveOutChecklist;
import com.rental.entity.RepairRequest;
import com.rental.entity.User;
import com.rental.mapper.ContractMapper;
import com.rental.mapper.ExpenseMapper;
import com.rental.mapper.HouseMapper;
import com.rental.mapper.MoveOutChecklistMapper;
import com.rental.mapper.RepairRequestMapper;
import com.rental.mapper.UserMapper;
import com.rental.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理后台服务实现。
 */
@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ContractMapper contractMapper;

    @Autowired
    private HouseMapper houseMapper;

    @Autowired
    private ExpenseMapper expenseMapper;

    @Autowired
    private RepairRequestMapper repairRequestMapper;

    @Autowired
    private MoveOutChecklistMapper moveOutChecklistMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public PageResult<User> listUsers(String keyword, long current, long size) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword).or().like(User::getNickname, keyword));
        }
        wrapper.orderByAsc(User::getId);
        Page<User> page = userMapper.selectPage(new Page<>(current, size), wrapper);
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    @Override
    public void updateUserStatus(Long targetId, Integer status) {
        checkNotSelf(targetId, "不能修改自己的状态");
        User user = getUserOrThrow(targetId);
        user.setStatus(status);
        userMapper.updateById(user);
    }

    @Override
    public void updateUserRole(Long targetId, String role) {
        checkNotSelf(targetId, "不能修改自己的角色");
        if (!"admin".equals(role) && !"user".equals(role)) {
            throw new RuntimeException("角色只能是 admin 或 user");
        }
        User user = getUserOrThrow(targetId);
        user.setRole(role);
        userMapper.updateById(user);
    }

    @Override
    public void resetPassword(Long targetId, String password) {
        User user = getUserOrThrow(targetId);
        user.setPassword(passwordEncoder.encode(password));
        userMapper.updateById(user);
    }

    @Override
    public PageResult<Map<String, Object>> listContracts(String keyword, long current, long size) {
        LambdaQueryWrapper<Contract> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Contract::getTitle, keyword).or().like(Contract::getFileName, keyword));
        }
        wrapper.orderByDesc(Contract::getCreatedAt);
        Page<Contract> page = contractMapper.selectPage(new Page<>(current, size), wrapper);
        Map<Long, String> nameMap = buildUserNameMap(page.getRecords().stream().map(Contract::getUserId).toList());
        List<Map<String, Object>> rows = page.getRecords().stream().map(c -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", c.getId());
            row.put("title", c.getTitle());
            row.put("fileName", c.getFileName());
            row.put("fileType", c.getFileType());
            row.put("status", c.getStatus());
            row.put("riskLevel", c.getRiskLevel());
            row.put("riskCount", c.getRiskCount());
            row.put("createdAt", c.getCreatedAt());
            row.put("username", nameMap.get(c.getUserId()));
            return row;
        }).toList();
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), rows);
    }

    @Override
    public PageResult<Map<String, Object>> listHouses(String keyword, long current, long size) {
        LambdaQueryWrapper<House> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(House::getTitle, keyword).or().like(House::getAddress, keyword));
        }
        wrapper.orderByDesc(House::getCreatedAt);
        Page<House> page = houseMapper.selectPage(new Page<>(current, size), wrapper);
        Map<Long, String> nameMap = buildUserNameMap(page.getRecords().stream().map(House::getUserId).toList());
        List<Map<String, Object>> rows = page.getRecords().stream().map(h -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", h.getId());
            row.put("title", h.getTitle());
            row.put("address", h.getAddress());
            row.put("rentPrice", h.getRentPrice());
            row.put("deposit", h.getDeposit());
            row.put("area", h.getArea());
            row.put("status", h.getStatus());
            row.put("createdAt", h.getCreatedAt());
            row.put("username", nameMap.get(h.getUserId()));
            return row;
        }).toList();
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), rows);
    }

    @Override
    public PageResult<Map<String, Object>> listExpenses(String keyword, long current, long size) {
        LambdaQueryWrapper<Expense> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Expense::getExpenseType, keyword);
        }
        wrapper.orderByDesc(Expense::getCreatedAt);
        Page<Expense> page = expenseMapper.selectPage(new Page<>(current, size), wrapper);
        Map<Long, String> nameMap = buildUserNameMap(page.getRecords().stream().map(Expense::getUserId).toList());
        List<Map<String, Object>> rows = page.getRecords().stream().map(e -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", e.getId());
            row.put("expenseType", e.getExpenseType());
            row.put("amount", e.getAmount());
            row.put("billMonth", e.getBillMonth());
            row.put("dueDate", e.getDueDate());
            row.put("isPaid", e.getIsPaid());
            row.put("createdAt", e.getCreatedAt());
            row.put("username", nameMap.get(e.getUserId()));
            return row;
        }).toList();
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), rows);
    }

    @Override
    public PageResult<Map<String, Object>> listRepairs(String keyword, long current, long size) {
        LambdaQueryWrapper<RepairRequest> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(RepairRequest::getTitle, keyword).or().like(RepairRequest::getDescription, keyword));
        }
        wrapper.orderByDesc(RepairRequest::getCreatedAt);
        Page<RepairRequest> page = repairRequestMapper.selectPage(new Page<>(current, size), wrapper);
        Map<Long, String> nameMap = buildUserNameMap(page.getRecords().stream().map(RepairRequest::getUserId).toList());
        List<Map<String, Object>> rows = page.getRecords().stream().map(r -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", r.getId());
            row.put("title", r.getTitle());
            row.put("urgency", r.getUrgency());
            row.put("status", r.getStatus());
            row.put("createdAt", r.getCreatedAt());
            row.put("username", nameMap.get(r.getUserId()));
            return row;
        }).toList();
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), rows);
    }

    @Override
    public PageResult<Map<String, Object>> listMoveOuts(String keyword, long current, long size) {
        LambdaQueryWrapper<MoveOutChecklist> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(MoveOutChecklist::getStatus, keyword);
        }
        wrapper.orderByDesc(MoveOutChecklist::getCreatedAt);
        Page<MoveOutChecklist> page = moveOutChecklistMapper.selectPage(new Page<>(current, size), wrapper);
        Map<Long, String> nameMap = buildUserNameMap(page.getRecords().stream().map(MoveOutChecklist::getUserId).toList());
        List<Map<String, Object>> rows = page.getRecords().stream().map(m -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", m.getId());
            row.put("contractId", m.getContractId());
            row.put("moveOutDate", m.getMoveOutDate());
            row.put("totalDeposit", m.getTotalDeposit());
            row.put("deductionAmount", m.getDeductionAmount());
            row.put("refundAmount", m.getRefundAmount());
            row.put("status", m.getStatus());
            row.put("createdAt", m.getCreatedAt());
            row.put("username", nameMap.get(m.getUserId()));
            return row;
        }).toList();
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), rows);
    }

    @Override
    public Map<String, Object> stats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("userCount", userMapper.selectCount(null));
        stats.put("contractCount", contractMapper.selectCount(null));
        stats.put("pendingContractCount", contractMapper.selectCount(
                new LambdaQueryWrapper<Contract>().eq(Contract::getStatus, "pending")));
        stats.put("highRiskContractCount", contractMapper.selectCount(
                new LambdaQueryWrapper<Contract>().eq(Contract::getRiskLevel, "高风险")));
        stats.put("houseCount", houseMapper.selectCount(null));
        stats.put("pendingRepairCount", repairRequestMapper.selectCount(
                new LambdaQueryWrapper<RepairRequest>().eq(RepairRequest::getStatus, "pending")));

        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = monthStart.plusMonths(1);
        BigDecimal monthExpense = expenseMapper.selectList(
                        new LambdaQueryWrapper<Expense>().between(Expense::getCreatedAt, monthStart, monthEnd))
                .stream()
                .map(e -> e.getAmount() == null ? BigDecimal.ZERO : e.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("monthExpense", monthExpense);
        return stats;
    }

    private User getUserOrThrow(Long targetId) {
        User user = userMapper.selectById(targetId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return user;
    }

    private void checkNotSelf(Long targetId, String message) {
        if (targetId.equals(SecurityUtil.getCurrentUserId())) {
            throw new RuntimeException(message);
        }
    }

    private Map<Long, String> buildUserNameMap(Collection<Long> userIds) {
        Set<Long> ids = userIds.stream().filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));
    }
}
