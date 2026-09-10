package com.rental.service;

import com.rental.entity.MoveOutChecklist;
import com.rental.common.PageResult;
import java.util.List;

public interface MoveOutChecklistService {
    MoveOutChecklist create(MoveOutChecklist checklist);
    void update(MoveOutChecklist checklist);
    void delete(Long id);
    PageResult<MoveOutChecklist> listByUser(long current, long size);
    MoveOutChecklist getDetail(Long id);
}
