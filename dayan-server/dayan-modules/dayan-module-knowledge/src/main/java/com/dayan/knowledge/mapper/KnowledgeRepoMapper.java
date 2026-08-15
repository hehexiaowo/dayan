package com.dayan.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.knowledge.entity.KnowledgeRepo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识仓库 Mapper。
 */
@Mapper
public interface KnowledgeRepoMapper extends BaseMapper<KnowledgeRepo> {
}
