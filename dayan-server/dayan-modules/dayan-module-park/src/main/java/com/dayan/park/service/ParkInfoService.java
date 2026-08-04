package com.dayan.park.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.park.dto.ParkInfoCreateDTO;
import com.dayan.park.dto.ParkInfoQueryDTO;
import com.dayan.park.dto.ParkInfoUpdateDTO;
import com.dayan.park.vo.ParkInfoVO;

import java.util.List;

/**
 * 机构主信息（park_info）服务。
 *
 * <p>平台共享表，不参与 channel_code 隔离；parkCode 为业务主键（PK+5）。
 * 状态字段 operate_status 由 {@link #transition} 经状态机引擎流转，禁止直接 update。
 */
public interface ParkInfoService {

    /** 分页查询 */
    PageResult<ParkInfoVO> page(ParkInfoQueryDTO query);

    /** 全量列表（按 sortOrder + id） */
    List<ParkInfoVO> list(ParkInfoQueryDTO query);

    /** 详情 */
    ParkInfoVO getDetail(String parkCode);

    /** 新增，返回生成的 parkCode */
    String create(ParkInfoCreateDTO dto);

    /** 更新 */
    void update(String parkCode, ParkInfoUpdateDTO dto);

    /** 删除 */
    void delete(String parkCode);

    /**
     * 状态机流转。
     *
     * @param parkCode 机构编码
     * @param event    事件（{@code ParkEvent} 常量）
     * @return 流转后的 operate_status
     */
    Integer transition(String parkCode, String event);
}
