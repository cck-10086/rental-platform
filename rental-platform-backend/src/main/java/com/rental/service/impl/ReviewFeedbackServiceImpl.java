package com.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rental.common.SecurityUtil;
import com.rental.entity.Contract;
import com.rental.entity.ReviewFeedback;
import com.rental.entity.ReviewRecord;
import com.rental.mapper.ContractMapper;
import com.rental.mapper.ReviewFeedbackMapper;
import com.rental.mapper.ReviewRecordMapper;
import com.rental.service.ReviewFeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 审查结果反馈服务实现。
 * 同一用户对同一条记录仅保留最新一条反馈（重复提交视为更新），
 * 不做模型侧自动学习，仅落库供管理后台统计。
 */
@Service
public class ReviewFeedbackServiceImpl implements ReviewFeedbackService {

    /** 合法反馈类型 */
    private static final Set<String> VALID_FEEDBACK = Set.of("useful", "false_positive");

    @Autowired
    private ReviewFeedbackMapper reviewFeedbackMapper;

    @Autowired
    private ReviewRecordMapper reviewRecordMapper;

    @Autowired
    private ContractMapper contractMapper;

    @Override
    public void submit(Long recordId, String feedback) {
        if (!VALID_FEEDBACK.contains(feedback)) {
            throw new RuntimeException("反馈类型只能是 useful 或 false_positive");
        }
        Long userId = SecurityUtil.getCurrentUserId();

        // 反馈目标必须存在且属于当前用户，防止越权反馈
        ReviewRecord record = reviewRecordMapper.selectById(recordId);
        if (record == null) {
            throw new RuntimeException("审查记录不存在");
        }
        if (!record.getUserId().equals(userId)) {
            throw new RuntimeException("无权反馈该审查记录");
        }

        // 同一记录同一用户仅保留最新反馈：已有则更新，没有则新增
        ReviewFeedback existing = reviewFeedbackMapper.selectOne(new LambdaQueryWrapper<ReviewFeedback>()
                .eq(ReviewFeedback::getRecordId, recordId)
                .eq(ReviewFeedback::getUserId, userId)
                .last("LIMIT 1"));
        if (existing != null) {
            existing.setFeedback(feedback);
            reviewFeedbackMapper.updateById(existing);
        } else {
            ReviewFeedback item = new ReviewFeedback();
            item.setRecordId(recordId);
            item.setUserId(userId);
            item.setFeedback(feedback);
            reviewFeedbackMapper.insert(item);
        }
    }

    @Override
    public List<ReviewFeedback> listMyFeedback(Long contractId) {
        Long userId = SecurityUtil.getCurrentUserId();
        Contract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new RuntimeException("合同不存在");
        }
        if (!contract.getUserId().equals(userId)) {
            throw new RuntimeException("无权查看该合同");
        }

        // 找到该合同全部审查记录的ID，再查当前用户对这些记录的反馈
        List<ReviewRecord> records = reviewRecordMapper.selectList(
                new LambdaQueryWrapper<ReviewRecord>().eq(ReviewRecord::getContractId, contractId));
        if (records.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> recordIds = records.stream().map(ReviewRecord::getId).toList();
        return reviewFeedbackMapper.selectList(new LambdaQueryWrapper<ReviewFeedback>()
                .eq(ReviewFeedback::getUserId, userId)
                .in(ReviewFeedback::getRecordId, recordIds));
    }
}
