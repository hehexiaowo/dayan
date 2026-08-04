package com.dayan.park.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.park.dto.ParkCareTypeCreateDTO;
import com.dayan.park.dto.ParkCareTypeQueryDTO;
import com.dayan.park.dto.ParkCareTypeUpdateDTO;
import com.dayan.park.vo.ParkCareTypeVO;

import java.util.List;

/**
 * 照护类型（park_care_type）服务。
 */
public interface ParkCareTypeService {

    PageResult<ParkCareTypeVO> page(ParkCareTypeQueryDTO query);

    List<ParkCareTypeVO> listByPark(String parkCode);

    ParkCareTypeVO getDetail(Long id);

    Long create(ParkCareTypeCreateDTO dto);

    void update(Long id, ParkCareTypeUpdateDTO dto);

    void delete(Long id);
}
