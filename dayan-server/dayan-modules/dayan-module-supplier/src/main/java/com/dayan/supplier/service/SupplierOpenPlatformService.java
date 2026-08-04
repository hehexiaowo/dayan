package com.dayan.supplier.service;

import com.dayan.supplier.dto.SupplierOpenPlatformCreateDTO;
import com.dayan.supplier.dto.SupplierOpenPlatformQueryDTO;
import com.dayan.supplier.dto.SupplierOpenPlatformUpdateDTO;
import com.dayan.supplier.vo.SupplierOpenPlatformVO;
import com.dayan.common.core.resp.PageResult;

/**
 * 供应商开放平台配置服务。
 *
 * <p>{@code app_secret} / {@code webhook_secret} 使用 AES-256-GCM 加密存储，
 * 密钥来源配置 {@code dayan.aes.key}，未配置时回退到 {@code AesGcmUtil.deriveKey("dayan-default-key")}。
 * 查询出参脱敏为 {@code ***}。
 */
public interface SupplierOpenPlatformService {

    PageResult<SupplierOpenPlatformVO> page(SupplierOpenPlatformQueryDTO query);

    SupplierOpenPlatformVO getDetail(Long id);

    Long create(SupplierOpenPlatformCreateDTO dto);

    void update(Long id, SupplierOpenPlatformUpdateDTO dto);

    void delete(Long id);
}
