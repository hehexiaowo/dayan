package com.dayan.park.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.park.dto.ParkPeripheryCreateDTO;
import com.dayan.park.dto.ParkPeripheryQueryDTO;
import com.dayan.park.dto.ParkPeripheryUpdateDTO;
import com.dayan.park.vo.ParkPeripheryVO;

import java.util.List;

/**
 * 机构周边信息（park_periphery）服务。
 */
public interface ParkPeripheryService {

    PageResult<ParkPeripheryVO> page(ParkPeripheryQueryDTO query);

    List<ParkPeripheryVO> listByPark(String parkCode);

    ParkPeripheryVO getDetail(Long id);

    Long create(ParkPeripheryCreateDTO dto);

    void update(Long id, ParkPeripheryUpdateDTO dto);

    void delete(Long id);
}
