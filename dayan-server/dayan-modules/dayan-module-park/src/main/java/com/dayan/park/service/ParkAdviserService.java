package com.dayan.park.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.park.dto.ParkAdviserCreateDTO;
import com.dayan.park.dto.ParkAdviserQueryDTO;
import com.dayan.park.dto.ParkAdviserUpdateDTO;
import com.dayan.park.vo.ParkAdviserVO;

import java.util.List;

/**
 * 机构顾问（park_adviser）服务。
 */
public interface ParkAdviserService {

    PageResult<ParkAdviserVO> page(ParkAdviserQueryDTO query);

    List<ParkAdviserVO> listByPark(String parkCode);

    ParkAdviserVO getDetail(Long id);

    Long create(ParkAdviserCreateDTO dto);

    void update(Long id, ParkAdviserUpdateDTO dto);

    void delete(Long id);
}
