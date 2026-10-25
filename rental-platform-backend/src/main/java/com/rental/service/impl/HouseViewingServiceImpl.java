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
import java.util.Objects;
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
        if (houseMapper.selectById(viewing.getHouseId()) == null) {
            throw new RuntimeException("房源不存在");
        }
        viewing.setId(null);
        viewing.setUserId(userId);
        viewing.setCreatedAt(null);
        houseViewingMapper.insert(viewing);
        log.info("看房记录添加成功: id={}, houseId={}", viewing.getId(), viewing.getHouseId());
        return viewing;
    }

    @Override
    public void update(HouseViewing viewing) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        HouseViewing existing = houseViewingMapper.selectById(viewing.getId());
        if (existing == null) {
            throw new RuntimeException("看房记录不存在");
        }
        if (!Objects.equals(existing.getUserId(), currentUserId)) {
            throw new RuntimeException("无权修改该看房记录");
        }
        if (houseMapper.selectById(viewing.getHouseId()) == null) {
            throw new RuntimeException("房源不存在");
        }
        // 仅复制允许编辑的字段，避免客户端覆盖用户和创建时间等系统字段
        existing.setHouseId(viewing.getHouseId());
        existing.setViewingTime(viewing.getViewingTime());
        existing.setImpression(viewing.getImpression());
        existing.setPros(viewing.getPros());
        existing.setCons(viewing.getCons());
        existing.setScore(viewing.getScore());
        houseViewingMapper.updateById(existing);
        log.info("看房记录更新成功: id={}", existing.getId());
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
        if (!Objects.equals(existing.getUserId(), currentUserId)) {
            throw new RuntimeException("无权删除该看房记录");
        }
        houseViewingMapper.deleteById(id);
        log.info("看房记录删除成功: id={}", id);
    }
}
