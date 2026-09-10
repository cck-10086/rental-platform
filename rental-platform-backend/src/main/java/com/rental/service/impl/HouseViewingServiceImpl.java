package com.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rental.common.SecurityUtil;
import com.rental.entity.House;
import com.rental.entity.HouseViewing;
import com.rental.mapper.HouseMapper;
import com.rental.mapper.HouseViewingMapper;
import com.rental.service.HouseViewingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class HouseViewingServiceImpl implements HouseViewingService {
    private static final Logger log = LoggerFactory.getLogger(HouseViewingServiceImpl.class);


    @Autowired
    private HouseViewingMapper houseViewingMapper;

    @Autowired
    private HouseMapper houseMapper;

    @Override
    public HouseViewing add(HouseViewing viewing) {
        Long userId = SecurityUtil.getCurrentUserId();
        viewing.setUserId(userId);
        houseViewingMapper.insert(viewing);
        log.info("看房记录添加成功: id={}, houseId={}", viewing.getId(), viewing.getHouseId());
        return viewing;
    }

    @Override
    public List<HouseViewing> listByHouse(Long houseId) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        LambdaQueryWrapper<HouseViewing> wrapper = new LambdaQueryWrapper<>();
        House house = houseMapper.selectById(houseId);
        // 房主可以查看该房源的全部看房记录，租客只能查看自己的记录
        if (house != null && house.getUserId().equals(currentUserId)) {
            wrapper.eq(HouseViewing::getHouseId, houseId);
        } else {
            wrapper.eq(HouseViewing::getHouseId, houseId).eq(HouseViewing::getUserId, currentUserId);
        }
        wrapper.orderByDesc(HouseViewing::getViewingTime);
        return houseViewingMapper.selectList(wrapper);
    }

    @Override
    public List<HouseViewing> listByUser() {
        Long userId = SecurityUtil.getCurrentUserId();
        LambdaQueryWrapper<HouseViewing> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HouseViewing::getUserId, userId).orderByDesc(HouseViewing::getViewingTime);
        return houseViewingMapper.selectList(wrapper);
    }

    @Override
    public void delete(Long id) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        HouseViewing existing = houseViewingMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("看房记录不存在");
        }
        if (!existing.getUserId().equals(currentUserId)) {
            throw new RuntimeException("无权删除该看房记录");
        }
        houseViewingMapper.deleteById(id);
        log.info("看房记录删除成功: id={}", id);
    }
}
