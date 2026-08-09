package com.dayan.park.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.park.dto.ParkFacilityTypeCreateDTO;
import com.dayan.park.dto.ParkFacilityTypeQueryDTO;
import com.dayan.park.dto.ParkFacilityTypeUpdateDTO;
import com.dayan.park.vo.ParkFacilityTypeVO;

import java.util.List;

/**
 * 机构设施类型（park_facility_type）服务。
 */
public interface ParkFacilityTypeService {

    PageResult<ParkFacilityTypeVO> page(ParkFacilityTypeQueryDTO query);

    List<ParkFacilityTypeVO> listByPark(String parkCode);

    ParkFacilityTypeVO getDetail(Long id);

    Long create(ParkFacilityTypeCreateDTO dto);

    void update(Long id, ParkFacilityTypeUpdateDTO dto);

    void delete(Long id);
}
