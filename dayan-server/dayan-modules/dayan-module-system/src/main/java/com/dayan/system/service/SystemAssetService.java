package com.dayan.system.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.system.dto.SystemAssetCreateDTO;
import com.dayan.system.dto.SystemAssetQueryDTO;
import com.dayan.system.dto.SystemAssetUpdateDTO;
import com.dayan.system.vo.SystemAssetVO;

import java.util.List;

/**
 * 系统素材库（system_asset）服务接口。
 */
public interface SystemAssetService {

    PageResult<SystemAssetVO> page(SystemAssetQueryDTO query);

    /**
     * 按机构编码查询全部素材。
     */
    List<SystemAssetVO> listByPark(String parkCode);

    /**
     * 按机构编码 + 素材类型查询。
     */
    List<SystemAssetVO> listByParkAndType(String parkCode, Integer assetType);

    SystemAssetVO getDetail(Long id);

    Long create(SystemAssetCreateDTO dto);

    void update(Long id, SystemAssetUpdateDTO dto);

    void delete(Long id);

    /**
     * 幂等注册：同 (parkCode, assetUrl, sourceType, sourceRefCode) 已存在则返回已存 id，否则创建。
     * 供各业务模块上传后自动登记素材库（storage_type 固定 1=本地OSS）。
     *
     * @param parkCode      机构编码（空=平台素材）
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
