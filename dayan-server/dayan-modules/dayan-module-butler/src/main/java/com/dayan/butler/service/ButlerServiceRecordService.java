package com.dayan.butler.service;

import com.dayan.butler.dto.ButlerServiceRecordCreateDTO;
import com.dayan.butler.dto.ButlerServiceRecordQueryDTO;
import com.dayan.butler.dto.ButlerServiceRecordUpdateDTO;
import com.dayan.butler.vo.ButlerServiceRecordVO;
import com.dayan.common.core.resp.PageResult;

import java.util.List;

/**
 * 管家服务记录服务。
 *
 * <p>按 butlerCode/clientCode 查询，communicateWay：1=电话 / 2=企微 / 3=微信 / 4=上门 / 5=其他。
 */
public interface ButlerServiceRecordService {

    PageResult<ButlerServiceRecordVO> page(ButlerServiceRecordQueryDTO query);

    List<ButlerServiceRecordVO> list(ButlerServiceRecordQueryDTO query);

    ButlerServiceRecordVO getDetail(Long id);

    Long create(ButlerServiceRecordCreateDTO dto);

    void update(Long id, ButlerServiceRecordUpdateDTO dto);

    void delete(Long id);
}
