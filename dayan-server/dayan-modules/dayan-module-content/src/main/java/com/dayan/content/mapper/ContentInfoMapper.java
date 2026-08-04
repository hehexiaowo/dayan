package com.dayan.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.content.entity.ContentInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * content_info 数据访问层。
 */
@Mapper
public interface ContentInfoMapper extends BaseMapper<ContentInfo> {
}
