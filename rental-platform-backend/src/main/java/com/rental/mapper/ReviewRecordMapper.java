package com.rental.mapper;

import com.rental.entity.ReviewRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReviewRecordMapper extends BaseMapper<ReviewRecord> {

    @Select("SELECT * FROM review_record WHERE contract_id = #{contractId} ORDER BY is_high_risk DESC, id ASC")
    List<ReviewRecord> selectByContractId(Long contractId);
}
