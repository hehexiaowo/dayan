package com.dayan.butler.service;

import com.dayan.butler.dto.ButlerScheduleCreateDTO;
import com.dayan.butler.dto.ButlerScheduleQueryDTO;
import com.dayan.butler.dto.ButlerScheduleUpdateDTO;
import com.dayan.butler.vo.ButlerScheduleVO;
import com.dayan.common.core.resp.PageResult;

import java.util.List;

/**
 * 管家排班服务。
 *
 * <p>按 butlerCode/scheduleDate 查询，scheduleType：1=上班 / 2=休假 / 3=外勤 / 4=培训。
 * 同 butlerCode + 同 scheduleDate 下 status=1 的排班不可时间重叠（应用层校验）。
 */
public interface ButlerScheduleService {

    PageResult<ButlerScheduleVO> page(ButlerScheduleQueryDTO query);

    List<ButlerScheduleVO> list(ButlerScheduleQueryDTO query);

    ButlerScheduleVO getDetail(Long id);

    Long create(ButlerScheduleCreateDTO dto);

    void update(Long id, ButlerScheduleUpdateDTO dto);

    void delete(Long id);
}
