package com.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rental.common.SecurityUtil;
import com.rental.common.PageResult;
import com.rental.entity.RepairRequest;
import com.rental.mapper.RepairRequestMapper;
import com.rental.service.RepairRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class RepairRequestServiceImpl implements RepairRequestService {
    private static final Logger log = LoggerFactory.getLogger(RepairRequestServiceImpl.class);


    @Autowired
    private RepairRequestMapper repairRequestMapper;

    @Override
    public RepairRequest add(RepairRequest repairRequest) {
        Long userId = SecurityUtil.getCurrentUserId();
        repairRequest.setUserId(userId);
        repairRequestMapper.insert(repairRequest);
        log.info("报修申请添加成功: id={}, userId={}", repairRequest.getId(), userId);
        return repairRequest;
    }

    @Override
    public void update(RepairRequest repairRequest) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        RepairRequest existing = repairRequestMapper.selectById(repairRequest.getId());
        if (existing == null) {
            throw new RuntimeException("报修申请不存在");
        }
        if (!existing.getUserId().equals(currentUserId)) {
            throw new RuntimeException("无权操作该报修申请");
        }
        repairRequestMapper.updateById(repairRequest);
        log.info("报修申请更新成功: id={}", repairRequest.getId());
    }

    @Override
    public void delete(Long id) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        RepairRequest existing = repairRequestMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("报修申请不存在");
        }
        if (!existing.getUserId().equals(currentUserId)) {
            throw new RuntimeException("无权操作该报修申请");
        }
        repairRequestMapper.deleteById(id);
        log.info("报修申请删除成功: id={}", id);
    }

    @Override
    public PageResult<RepairRequest> listByUser(long current, long size) {
        Long userId = SecurityUtil.getCurrentUserId();
        LambdaQueryWrapper<RepairRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RepairRequest::getUserId, userId).orderByDesc(RepairRequest::getCreatedAt);
        Page<RepairRequest> page = repairRequestMapper.selectPage(new Page<>(current, size), wrapper);
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

}
