package com.dayan.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.service.entity.ServiceSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * service_session 数据访问层。
 */
@Mapper
public interface ServiceSessionMapper extends BaseMapper<ServiceSession> {

    /**
     * 年度配额批量重置：将 quota_type=2（年度配额）且未在当前年重置过的活跃会话的 used_count 归零，
     * 并更新 quota_reset_year 为当前年。排除终态会话（已完成=6 / 已取消=7）。
     *
     * @param currentYear 当前年份（如 2026）
     * @return 受影响行数
     */
    @Update("UPDATE service_session SET used_count = 0, quota_reset_year = #{currentYear}, " +
            "updated_at = NOW(), updater = 'quota-reset-job' " +
            "WHERE quota_type = 2 AND deleted = 0 " +
            "AND (quota_reset_year IS NULL OR quota_reset_year < #{currentYear}) " +
            "AND session_status NOT IN (6, 7)")
    int resetAnnualQuota(@Param("currentYear") int currentYear);
}
