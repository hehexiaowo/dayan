package com.dayan.park.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.park.dto.ParkFacilityCreateDTO;
import com.dayan.park.dto.ParkFacilityQueryDTO;
import com.dayan.park.dto.ParkFacilityUpdateDTO;
import com.dayan.park.vo.ParkFacilityVO;

import java.util.List;

/**
 * 机构设施（park_facility）服务。
 */
public interface ParkFacilityService {

    PageResult<ParkFacilityVO> page(ParkFacilityQueryDTO query);

    List<ParkFacilityVO> listByPark(String parkCode);

    ParkFacilityVO getDetail(Long id);

    Long create(ParkFacilityCreateDTO dto);

    void update(Long id, ParkFacilityUpdateDTO dto);

    void delete(Long id);
}
