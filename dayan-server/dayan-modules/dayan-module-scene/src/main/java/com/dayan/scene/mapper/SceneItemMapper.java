package com.dayan.scene.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.scene.entity.SceneItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * scene_item 数据访问层。
 */
@Mapper
public interface SceneItemMapper extends BaseMapper<SceneItem> {
}
