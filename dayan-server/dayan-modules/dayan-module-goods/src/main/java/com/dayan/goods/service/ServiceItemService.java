package com.dayan.goods.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.goods.dto.ServiceItemCreateDTO;
import com.dayan.goods.dto.ServiceItemQueryDTO;
import com.dayan.goods.dto.ServiceItemUpdateDTO;
import com.dayan.goods.entity.ServiceItem;
import com.dayan.goods.vo.ServiceItemVO;

import java.util.List;

public interface ServiceItemService {

    PageResult<ServiceItemVO> page(ServiceItemQueryDTO query);

    List<ServiceItemVO> list(ServiceItemQueryDTO query);

    ServiceItemVO getDetail(String itemCode);

    /** 按编码查实体，不存在抛异常。供其他模块调用。 */
    ServiceItem requireItem(String itemCode);

    String create(ServiceItemCreateDTO dto);

    void update(String itemCode, ServiceItemUpdateDTO dto);

    void delete(String itemCode);
}
