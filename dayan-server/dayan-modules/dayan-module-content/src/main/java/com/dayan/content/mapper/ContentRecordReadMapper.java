package com.dayan.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.content.entity.ContentRecordRead;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * content_record_read 数据访问层。
 */
@Mapper
public interface ContentRecordReadMapper extends BaseMapper<ContentRecordRead> {

    /**
     * 按内容编码统计独立访客数（reader_code 去重，忽略空值）。
     */
    @Select("SELECT COUNT(DISTINCT reader_code) FROM content_record_read "
            + "WHERE content_code = #{contentCode} AND reader_code IS NOT NULL AND reader_code <> '' AND deleted = 0")
    long countUvByContentCode(@Param("contentCode") String contentCode);
}
