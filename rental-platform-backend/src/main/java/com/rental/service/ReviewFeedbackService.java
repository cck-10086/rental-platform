package com.rental.service;

import com.rental.entity.ReviewFeedback;

import java.util.List;

/**
 * 审查结果反馈服务：用户对审查记录标记 有用/误报，形成误报反馈闭环。
 */
public interface ReviewFeedbackService {

    /**
     * 提交或更新当前用户对某条审查记录的反馈。
     *
     * @param recordId 审查记录ID
     * @param feedback 反馈类型：useful-有用 / false_positive-误报
     */
    void submit(Long recordId, String feedback);

    /**
     * 查询当前用户对某合同全部审查记录的反馈（报告页回显按钮状态用）。
     *
     * @param contractId 合同ID
     * @return 该用户已提交的反馈列表
     */
    List<ReviewFeedback> listMyFeedback(Long contractId);
}
