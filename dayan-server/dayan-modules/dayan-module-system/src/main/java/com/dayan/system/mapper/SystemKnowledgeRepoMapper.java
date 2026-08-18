package com.dayan.knowledge.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.knowledge.entity.KnowledgeRepo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 知识仓库 Mapper。
 */
@Mapper
public interface KnowledgeRepoMapper extends BaseMapper<KnowledgeRepo> {

    /**
     * 查渠道全称（VO 展示用；直接读 channel_info 表避免 knowledge → channel 模块依赖）。
     * channel_info 为平台共享表（租户拦截忽略清单内），可全量查询。
     */
    @Select("SELECT full_name FROM channel_info WHERE channel_code = #{channelCode} AND deleted = 0 LIMIT 1")
    String selectChannelFullName(@Param("channelCode") String channelCode);

    /**
     * 查渠道简称（列表「归属」列展示用；同上直读 channel_info 表）。
     */
    @Select("SELECT short_name FROM channel_info WHERE channel_code = #{channelCode} AND deleted = 0 LIMIT 1")
    String selectChannelShortName(@Param("channelCode") String channelCode);

    /**
     * 按渠道编码集合批量查仓库（树形继承解析用）。
     *
     * <p>跳过租户拦截：channel 端需看到「本渠道 + 后代渠道」的仓库（自动追加 channel_code=本渠道
     * 会过滤掉子渠道）。可见性由业务层 {@code getRepoTree} 的渠道树范围校验兜底。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("<script>" +
            "SELECT * FROM knowledge_repo WHERE deleted = 0 AND repo_type = 2 AND channel_code IN " +
            "<foreach collection='channelCodes' item='code' open='(' separator=',' close=')'>#{code}</foreach>" +
            "</script>")
    List<KnowledgeRepo> selectByChannelCodes(@Param("channelCodes") Collection<String> channelCodes);

    /**
     * 按 id 查仓库（跳过租户拦截；chat/retrieve 使用继承库时需跨渠道读取）。
     *
     * <p>可见性由业务层 {@code requireRepoVisible} 校验（仓库所属渠道 ∈ 当前渠道 ∪ 祖先 ∪ 后代）。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM knowledge_repo WHERE id = #{id} AND deleted = 0 LIMIT 1")
    KnowledgeRepo selectByIdIgnoreTenant(@Param("id") Long id);
}
