package com.rental.service.impl;

import com.rental.entity.ReviewFeedback;
import com.rental.entity.ReviewRecord;
import com.rental.mapper.ContractMapper;
import com.rental.mapper.ReviewFeedbackMapper;
import com.rental.mapper.ReviewRecordMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 审查结果反馈服务单元测试：类型校验、越权防护、新增与更新反馈。
 */
@ExtendWith(MockitoExtension.class)
class ReviewFeedbackServiceImplTest {

    @Mock
    private ReviewFeedbackMapper reviewFeedbackMapper;

    @Mock
    private ReviewRecordMapper reviewRecordMapper;

    @Mock
    private ContractMapper contractMapper;

    @InjectMocks
    private ReviewFeedbackServiceImpl feedbackService;

    private void loginAs(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }

    private ReviewRecord record(Long id, Long userId) {
        ReviewRecord record = new ReviewRecord();
        record.setId(id);
        record.setUserId(userId);
        return record;
    }

    @Test
    void submitRejectsInvalidFeedbackType() {
        loginAs(1L);
        try {
            RuntimeException e = assertThrows(RuntimeException.class,
                    () -> feedbackService.submit(10L, "good"));
            assertEquals("反馈类型只能是 useful 或 false_positive", e.getMessage());
            // 类型校验先于查库执行，不应访问数据库
            verify(reviewRecordMapper, never()).selectById(any());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void submitRejectsOthersRecord() {
        loginAs(1L);
        try {
            when(reviewRecordMapper.selectById(10L)).thenReturn(record(10L, 2L));
            RuntimeException e = assertThrows(RuntimeException.class,
                    () -> feedbackService.submit(10L, "useful"));
            assertEquals("无权反馈该审查记录", e.getMessage());
            verify(reviewFeedbackMapper, never()).insert(any(ReviewFeedback.class));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void submitInsertsNewFeedback() {
        loginAs(1L);
        try {
            when(reviewRecordMapper.selectById(10L)).thenReturn(record(10L, 1L));
            when(reviewFeedbackMapper.selectOne(any())).thenReturn(null);

            feedbackService.submit(10L, "false_positive");

            verify(reviewFeedbackMapper).insert(any(ReviewFeedback.class));
            verify(reviewFeedbackMapper, never()).updateById(any(ReviewFeedback.class));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void submitUpdatesExistingFeedback() {
        loginAs(1L);
        try {
            when(reviewRecordMapper.selectById(10L)).thenReturn(record(10L, 1L));
            ReviewFeedback existing = new ReviewFeedback();
            existing.setId(99L);
            existing.setRecordId(10L);
            existing.setUserId(1L);
            existing.setFeedback("useful");
            when(reviewFeedbackMapper.selectOne(any())).thenReturn(existing);

            feedbackService.submit(10L, "false_positive");

            // 重复反馈视为更新：同一记录同一用户仅保留最新意见
            assertEquals("false_positive", existing.getFeedback());
            verify(reviewFeedbackMapper).updateById(existing);
            verify(reviewFeedbackMapper, never()).insert(any(ReviewFeedback.class));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
