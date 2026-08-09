package com.dayan.park.service;

import com.dayan.park.dto.ParkScoreUpdateDTO;
import com.dayan.park.vo.ParkScoreVO;

/**
 * 机构评分服务（一对一，upsert 语义）。
 */
public interface ParkScoreService {

    /** 按机构编码获取评分（不存在返回 null 字段值的空 VO） */
    ParkScoreVO getByParkCode(String parkCode);

    /** 更新或创建评分（upsert） */
    void upsert(String parkCode, ParkScoreUpdateDTO dto);
}
