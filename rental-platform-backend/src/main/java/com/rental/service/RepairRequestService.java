package com.rental.service;

import com.rental.entity.RepairRequest;
import com.rental.common.PageResult;
import java.util.List;

public interface RepairRequestService {
    RepairRequest add(RepairRequest request);
    void update(RepairRequest request);
    void delete(Long id);
    PageResult<RepairRequest> listByUser(long current, long size);
}
