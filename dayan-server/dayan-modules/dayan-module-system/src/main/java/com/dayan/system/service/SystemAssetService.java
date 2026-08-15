package com.dayan.system.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.system.dto.SystemAssetCreateDTO;
import com.dayan.system.dto.SystemAssetQueryDTO;
import com.dayan.system.dto.SystemAssetUpdateDTO;
import com.dayan.system.vo.SystemAssetVO;

import java.util.List;

/**
 * 系统素材仓库（system_asset）服务接口。
 *
 * <p>素材仓库是全系统文件/地址登记中心：只存地址与冗余分类（类型1/类型2/关联编码），
 * 真实引用关系由各业务表持有（删除保护按 AssetRefMap 反查业务表）。
 */
public interface SystemAssetService {

    PageResult<SystemAssetVO> page(SystemAssetQueryDTO query);

    /**
     * 按分类三元组查询全部素材（如 refType1=park + refCode=机构编码）。
     */
    List<SystemAssetVO> listByRef(String refType1, String refCode);

    /**
     * 按分类三元组 + 素材类型查询。
     */
    List<SystemAssetVO> listByRefAndType(String refType1, String refCode, Integer assetType);

    SystemAssetVO getDetail(Long id);

    Long create(SystemAssetCreateDTO dto);

    void update(Long id, SystemAssetUpdateDTO dto);

    void delete(Long id);

    /**
     * 幂等登记：同 (assetUrl, refType1, refCode, refType2) 已存在则返回已存 id，否则创建。
     * 供各业务模块上传后自动登记素材仓库（storage_type 固定 1=本地OSS）。
     *
     * @param refType1  类型1：业务维度（空=platform）
     * @param refCode   关联编码：业务实体编码（空=无关联）
     * @param assetType 素材类型（1图片 2视频 3文件 4VR）
     * @param assetUrl  文件 key
     * @param refType2  类型2：细分分类（空=media_mgmt）
     * @param assetName 文件名（可为 null）
     * @param fileSize  文件大小字节（可为 null）
     * @return 素材 id（已存或新建）
     */
    Long registerIfAbsent(String refType1, String refCode, Integer assetType, String assetUrl,
                          String refType2, String assetName, Long fileSize);
}
