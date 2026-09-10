package com.rental.mapper;

import com.rental.entity.Contract;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ContractMapper extends BaseMapper<Contract> {

    @Select("SELECT * FROM contract WHERE user_id = #{userId} AND deleted = 0 ORDER BY created_at DESC")
    List<Contract> selectByUserId(Long userId);
}
