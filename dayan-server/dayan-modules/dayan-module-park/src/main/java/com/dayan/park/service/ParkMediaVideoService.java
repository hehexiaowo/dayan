package com.dayan.park.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.park.dto.ParkMediaVideoCreateDTO;
import com.dayan.park.dto.ParkMediaVideoQueryDTO;
import com.dayan.park.dto.ParkMediaVideoUpdateDTO;
import com.dayan.park.vo.ParkMediaVideoVO;

import java.util.List;

/**
 * 机构视频（park_media_video）服务。
 */
public interface ParkMediaVideoService {

    PageResult<ParkMediaVideoVO> page(ParkMediaVideoQueryDTO query);

    List<ParkMediaVideoVO> listByPark(String parkCode);

    ParkMediaVideoVO getDetail(Long id);

    Long create(ParkMediaVideoCreateDTO dto);

    void update(Long id, ParkMediaVideoUpdateDTO dto);

    void delete(Long id);
}
