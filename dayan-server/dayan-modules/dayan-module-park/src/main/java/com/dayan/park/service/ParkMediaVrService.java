package com.dayan.park.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.park.dto.ParkMediaVrCreateDTO;
import com.dayan.park.dto.ParkMediaVrQueryDTO;
import com.dayan.park.dto.ParkMediaVrUpdateDTO;
import com.dayan.park.vo.ParkMediaVrVO;

import java.util.List;

/**
 * 机构 VR（park_media_vr）服务。
 */
public interface ParkMediaVrService {

    PageResult<ParkMediaVrVO> page(ParkMediaVrQueryDTO query);

    List<ParkMediaVrVO> listByPark(String parkCode);

    ParkMediaVrVO getDetail(Long id);

    Long create(ParkMediaVrCreateDTO dto);

    void update(Long id, ParkMediaVrUpdateDTO dto);

    void delete(Long id);
}
