package com.rental.service;

import com.rental.entity.House;
import com.rental.common.PageResult;
import java.util.List;

public interface HouseService {
    House add(House house);
    void update(House house);
    void delete(Long id);
    PageResult<House> listByUser(long current, long size);
    House getDetail(Long id);
}
