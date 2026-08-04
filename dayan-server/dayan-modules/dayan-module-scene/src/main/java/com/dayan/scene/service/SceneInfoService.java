package com.dayan.scene.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.scene.dto.SceneInfoAuditDTO;
import com.dayan.scene.dto.SceneInfoCreateDTO;
import com.dayan.scene.dto.SceneInfoQueryDTO;
import com.dayan.scene.dto.SceneInfoUpdateDTO;
import com.dayan.scene.vo.SceneInfoVO;

import java.util.List;

/**
 * 场景信息服务。
 *
 * <p>{@code scene_info} 平台共享表，{@code sceneCode} 由后端生成（"SC" + 5 位序列），
 * {@code sceneName} 全表唯一。
 */
public interface SceneInfoService {

    PageResult<SceneInfoVO> page(SceneInfoQueryDTO query);

    List<SceneInfoVO> list(SceneInfoQueryDTO query);

    SceneInfoVO getDetail(String sceneCode);

    String create(SceneInfoCreateDTO dto);

    void update(String sceneCode, SceneInfoUpdateDTO dto);

    void delete(String sceneCode);

    /** 提交审核（草稿态提交，audit_status 置为待审） */
    void submit(String sceneCode);

    /** 审核（audit_status → 1通过 / 2驳回） */
    void audit(SceneInfoAuditDTO dto);

    /** 上架（scene_status 0→1，要求 audit_status=1 通过） */
    void shelves(String sceneCode);

    /** 下架（scene_status 1→2） */
    void offshelves(String sceneCode);

    /** 重新上架（scene_status 2→1） */
    void reshelves(String sceneCode);

    /** 满期（scene_status 1→3，活动到期或名额约满） */
    void full(String sceneCode);
}
