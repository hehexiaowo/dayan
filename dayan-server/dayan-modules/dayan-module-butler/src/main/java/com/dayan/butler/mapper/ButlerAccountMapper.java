package com.dayan.butler.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.butler.entity.ButlerAccount;
import org.apache.ibatis.annotations.Mapper;

/**
 * butler_account 数据访问层。
 */
@Mapper
public interface ButlerAccountMapper extends BaseMapper<ButlerAccount> {
}
