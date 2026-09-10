package com.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rental.common.SecurityUtil;
import com.rental.common.PageResult;
import com.rental.entity.MoveOutChecklist;
import com.rental.mapper.MoveOutChecklistMapper;
import com.rental.service.MoveOutChecklistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class MoveOutChecklistServiceImpl implements MoveOutChecklistService {
    private static final Logger log = LoggerFactory.getLogger(MoveOutChecklistServiceImpl.class);


    @Autowired
    private MoveOutChecklistMapper moveOutChecklistMapper;

    @Override
    public MoveOutChecklist create(MoveOutChecklist checklist) {
        Long userId = SecurityUtil.getCurrentUserId();
        checklist.setUserId(userId);
        moveOutChecklistMapper.insert(checklist);
        log.info("退房清单创建成功: id={}, userId={}", checklist.getId(), userId);
        return checklist;
    }

    @Override
    public void update(MoveOutChecklist checklist) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        MoveOutChecklist existing = moveOutChecklistMapper.selectById(checklist.getId());
        if (existing == null) {
            throw new RuntimeException("退房清单不存在");
        }
        if (!existing.getUserId().equals(currentUserId)) {
            throw new RuntimeException("无权操作该退房清单");
        }
        moveOutChecklistMapper.updateById(checklist);
        log.info("退房清单更新成功: id={}", checklist.getId());
    }

    @Override
    public void delete(Long id) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        MoveOutChecklist existing = moveOutChecklistMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("退房清单不存在");
        }
        if (!existing.getUserId().equals(currentUserId)) {
            throw new RuntimeException("无权操作该退房清单");
        }
        moveOutChecklistMapper.deleteById(id);
        log.info("退房清单删除成功: id={}", id);
    }

    @Override
    public PageResult<MoveOutChecklist> listByUser(long current, long size) {
        Long userId = SecurityUtil.getCurrentUserId();
        LambdaQueryWrapper<MoveOutChecklist> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MoveOutChecklist::getUserId, userId).orderByDesc(MoveOutChecklist::getCreatedAt);
        Page<MoveOutChecklist> page = moveOutChecklistMapper.selectPage(new Page<>(current, size), wrapper);
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    @Override
    public MoveOutChecklist getDetail(Long id) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        MoveOutChecklist checklist = moveOutChecklistMapper.selectById(id);
        if (checklist == null) {
            throw new RuntimeException("退房清单不存在");
        }
        if (!checklist.getUserId().equals(currentUserId)) {
            throw new RuntimeException("无权查看该退房清单");
        }
        return checklist;
    }
}
