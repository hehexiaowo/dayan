package com.dayan.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.content.entity.ContentCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * content_category 数据访问层。
 */
@Mapper
public interface ContentCategoryMapper extends BaseMapper<ContentCategory> {
}
