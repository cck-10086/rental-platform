package com.rental.service;

import com.rental.entity.HouseViewing;
import java.util.List;

public interface HouseViewingService {
    HouseViewing add(HouseViewing viewing);
    List<HouseViewing> listByHouse(Long houseId);
    List<HouseViewing> listByUser();
    void delete(Long id);
}
