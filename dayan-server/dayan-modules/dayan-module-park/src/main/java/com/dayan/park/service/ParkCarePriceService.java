package com.dayan.park.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.park.dto.ParkCarePriceCreateDTO;
import com.dayan.park.dto.ParkCarePriceQueryDTO;
import com.dayan.park.dto.ParkCarePriceUpdateDTO;
import com.dayan.park.vo.ParkCarePriceVO;

import java.util.List;

/**
 * 照护价格（park_care_price）服务。
 *
 * <p>校验：effectiveDate &lt; expireDate；isCurrent=1 同 careTypeCode 下唯一。
 */
public interface ParkCarePriceService {

    PageResult<ParkCarePriceVO> page(ParkCarePriceQueryDTO query);

    List<ParkCarePriceVO> listByCareType(String parkCode, String careTypeCode);

    ParkCarePriceVO getDetail(Long id);

    Long create(ParkCarePriceCreateDTO dto);

    void update(Long id, ParkCarePriceUpdateDTO dto);

    void delete(Long id);
}
