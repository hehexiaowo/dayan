package com.dayan.park.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.park.dto.ParkDisplayBlockCreateDTO;
import com.dayan.park.dto.ParkDisplayBlockQueryDTO;
import com.dayan.park.dto.ParkDisplayBlockUpdateDTO;
import com.dayan.park.vo.ParkDisplayBlockVO;

import java.util.List;

/**
 * 机构展示板块（park_display_block）服务。
 */
public interface ParkDisplayBlockService {

    PageResult<ParkDisplayBlockVO> page(ParkDisplayBlockQueryDTO query);

    List<ParkDisplayBlockVO> listByPark(String parkCode);

    ParkDisplayBlockVO getDetail(Long id);

    Long create(ParkDisplayBlockCreateDTO dto);

    void update(Long id, ParkDisplayBlockUpdateDTO dto);

    void delete(Long id);
}
