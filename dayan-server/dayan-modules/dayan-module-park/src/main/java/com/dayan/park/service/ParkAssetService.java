package com.dayan.park.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.park.dto.ParkAssetCreateDTO;
import com.dayan.park.dto.ParkAssetQueryDTO;
import com.dayan.park.dto.ParkAssetUpdateDTO;
import com.dayan.park.vo.ParkAssetVO;

import java.util.List;

/**
 * 机构素材库（park_asset）服务接口。
 */
public interface ParkAssetService {

    PageResult<ParkAssetVO> page(ParkAssetQueryDTO query);

    /**
     * 按机构编码查询全部素材。
     */
    List<ParkAssetVO> listByPark(String parkCode);

    /**
     * 按机构编码 + 素材类型查询。
     */
    List<ParkAssetVO> listByParkAndType(String parkCode, Integer assetType);

    ParkAssetVO getDetail(Long id);

    Long create(ParkAssetCreateDTO dto);

    void update(Long id, ParkAssetUpdateDTO dto);

    void delete(Long id);

    /**
     * 幂等注册：同 (parkCode, assetUrl, sourceType, sourceRefCode) 已存在则返回已存 id，否则创建。
     * 供其他业务 tab 上传后自动注册到素材库。
     *
     * @param parkCode      机构编码
     * @param assetType     素材类型（1图片 2视频 3文件 4VR）
     * @param assetUrl      文件 key
     * @param sourceType    来源类型
     * @param sourceRefCode 来源编码（可为 null）
     * @param assetName     文件名（可为 null）
     * @param fileSize      文件大小字节（可为 null）
     * @return 素材 id（已存或新建）
     */
    Long registerIfAbsent(String parkCode, Integer assetType, String assetUrl,
                          String sourceType, String sourceRefCode,
                          String assetName, Long fileSize);
}
