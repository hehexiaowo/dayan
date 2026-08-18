package com.dayan.tool.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.tool.dto.ToolAiCreatorCreateDTO;
import com.dayan.tool.dto.ToolAiCreatorQueryDTO;
import com.dayan.tool.entity.ToolAiCreator;
import com.dayan.tool.vo.ToolAiCreatorListVO;
import com.dayan.tool.vo.ToolAiCreatorVO;

/**
 * AI 创作项目 CRUD 与状态机守卫。
 *
 * <p>归属校验：agentCode 强制取登录上下文（{@link #requireOwned}，越权按不存在处理），
 * 流水线各阶段复用。素材为前端聚合提交的快照（materials），本服务只做目的必选素材校验。
 */
public interface ToolAiCreatorService {

    /**
     * 创建项目（目的/形态/风格/读者/主题 + 素材引用与快照）。
     *
     * @param dto 创建参数（materials 为前端聚合素材块）
     * @return 项目主键
     */
    Long create(ToolAiCreatorCreateDTO dto);

    /**
     * 项目详情（恢复草稿）。
     */
    ToolAiCreatorVO getDetail(Long id);

    /**
     * 我的创作分页。
     */
    PageResult<ToolAiCreatorListVO> page(ToolAiCreatorQueryDTO dto);

    /**
     * 删除草稿。
     */
    void delete(Long id);

    /**
     * 取本人项目（越权按不存在处理），供流水线各阶段复用。
     */
    ToolAiCreator requireOwned(Long id);

    /**
     * 供流水线保存阶段产物（不越权校验，实体来自 requireOwned）。
     */
    int updateById(ToolAiCreator entity);

    /**
     * 解析实体为 VO（素材名从 refs 直读，不再回查业务库）。
     */
    ToolAiCreatorVO toVO(ToolAiCreator entity);
}
