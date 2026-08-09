package com.dayan.park.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.park.dto.ParkServiceTypeCreateDTO;
import com.dayan.park.dto.ParkServiceTypeQueryDTO;
import com.dayan.park.dto.ParkServiceTypeUpdateDTO;
import com.dayan.park.vo.ParkServiceTypeVO;

import java.util.List;

/**
 * 机构服务类型（park_service_type）服务。
 */
public interface ParkServiceTypeService {

    PageResult<ParkServiceTypeVO> page(ParkServiceTypeQueryDTO query);

    List<ParkServiceTypeVO> listByPark(String parkCode);

    ParkServiceTypeVO getDetail(Long id);

    Long create(ParkServiceTypeCreateDTO dto);

    void update(Long id, ParkServiceTypeUpdateDTO dto);

    void delete(Long id);
}
