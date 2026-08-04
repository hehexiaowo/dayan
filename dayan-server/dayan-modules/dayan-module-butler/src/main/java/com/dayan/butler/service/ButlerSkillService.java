package com.dayan.butler.service;

import com.dayan.butler.dto.ButlerSkillCreateDTO;
import com.dayan.butler.dto.ButlerSkillQueryDTO;
import com.dayan.butler.dto.ButlerSkillUpdateDTO;
import com.dayan.butler.vo.ButlerSkillVO;
import com.dayan.common.core.resp.PageResult;

import java.util.List;

/**
 * 管家技能服务。
 *
 * <p>按 butlerCode 查询，记录技能标签/资质。
 */
public interface ButlerSkillService {

    PageResult<ButlerSkillVO> page(ButlerSkillQueryDTO query);

    List<ButlerSkillVO> list(ButlerSkillQueryDTO query);

    ButlerSkillVO getDetail(Long id);

    Long create(ButlerSkillCreateDTO dto);

    void update(Long id, ButlerSkillUpdateDTO dto);

    void delete(Long id);
}
