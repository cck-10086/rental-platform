package com.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rental.common.SecurityUtil;
import com.rental.common.PageResult;
import com.rental.entity.House;
import com.rental.mapper.HouseMapper;
import com.rental.service.HouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class HouseServiceImpl implements HouseService {
    private static final Logger log = LoggerFactory.getLogger(HouseServiceImpl.class);


    @Autowired
    private HouseMapper houseMapper;

    @Override
    public House add(House house) {
        Long userId = SecurityUtil.getCurrentUserId();
        house.setUserId(userId);
        houseMapper.insert(house);
        log.info("房源添加成功: id={}, userId={}", house.getId(), userId);
        return house;
    }

    @Override
    public void update(House house) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        House existing = houseMapper.selectById(house.getId());
        if (existing == null) {
            throw new RuntimeException("房源不存在");
        }
        if (!existing.getUserId().equals(currentUserId)) {
            throw new RuntimeException("无权操作该房源");
        }
        houseMapper.updateById(house);
        log.info("房源更新成功: id={}", house.getId());
    }

    @Override
    public void delete(Long id) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        House existing = houseMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("房源不存在");
        }
        if (!existing.getUserId().equals(currentUserId)) {
            throw new RuntimeException("无权操作该房源");
        }
        houseMapper.deleteById(id);
        log.info("房源删除成功: id={}", id);
    }

    @Override
    public PageResult<House> listByUser(long current, long size) {
        Long userId = SecurityUtil.getCurrentUserId();
        LambdaQueryWrapper<House> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(House::getUserId, userId).orderByDesc(House::getCreatedAt);
        Page<House> page = houseMapper.selectPage(new Page<>(current, size), wrapper);
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    @Override
    public House getDetail(Long id) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        House house = houseMapper.selectById(id);
        if (house == null) {
            throw new RuntimeException("房源不存在");
        }
        if (!house.getUserId().equals(currentUserId)) {
            throw new RuntimeException("无权查看该房源");
        }
        return house;
    }
}
