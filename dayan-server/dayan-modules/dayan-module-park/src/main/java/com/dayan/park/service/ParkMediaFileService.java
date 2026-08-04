package com.dayan.park.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.park.dto.ParkMediaFileCreateDTO;
import com.dayan.park.dto.ParkMediaFileQueryDTO;
import com.dayan.park.dto.ParkMediaFileUpdateDTO;
import com.dayan.park.vo.ParkMediaFileVO;

import java.util.List;

/**
 * 机构文件（park_media_file）服务。
 */
public interface ParkMediaFileService {

    PageResult<ParkMediaFileVO> page(ParkMediaFileQueryDTO query);

    List<ParkMediaFileVO> listByPark(String parkCode);

    ParkMediaFileVO getDetail(Long id);

    Long create(ParkMediaFileCreateDTO dto);

    void update(Long id, ParkMediaFileUpdateDTO dto);

    void delete(Long id);
}
