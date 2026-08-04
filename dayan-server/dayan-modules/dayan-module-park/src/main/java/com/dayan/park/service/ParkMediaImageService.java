package com.dayan.park.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.park.dto.ParkMediaImageCreateDTO;
import com.dayan.park.dto.ParkMediaImageQueryDTO;
import com.dayan.park.dto.ParkMediaImageUpdateDTO;
import com.dayan.park.vo.ParkMediaImageVO;

import java.util.List;

/**
 * 机构图片（park_media_image）服务。
 *
 * <p>按 parkCode 维度 CRUD；URL 唯一校验（同 parkCode 下 imageUrl 不重复）。
 */
public interface ParkMediaImageService {

    PageResult<ParkMediaImageVO> page(ParkMediaImageQueryDTO query);

    List<ParkMediaImageVO> listByPark(String parkCode);

    ParkMediaImageVO getDetail(Long id);

    Long create(ParkMediaImageCreateDTO dto);

    void update(Long id, ParkMediaImageUpdateDTO dto);

    void delete(Long id);
}
