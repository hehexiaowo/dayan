package com.dayan.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.service.entity.ServiceSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * service_session 数据访问层。
 */
@Mapper
public interface ServiceSessionMapper extends BaseMapper<ServiceSession> {

    /**
     * 统计某权益某服务项目的终身已消费次数（只数已完成 session_status=6）。
     *
     * <p>配额聚合：每次履约 = 1 个 session，消费后 used_count=1。
     * 终身配额（quota_type=1）用此方法统计总消费次数。
     *
     * @param equityCode 权益编码
     * @param itemCode   服务项目编码
     * @return 已消费次数
     */
    @Select("SELECT COUNT(*) FROM service_session " +
            "WHERE equity_code = #{equityCode} AND item_code = #{itemCode} " +
            "AND used_count = 1 AND deleted = 0 " +
            // ServiceSessionEvent.STATUS_COMPLETED = 6
            "AND session_status = 6")
    int countConsumedSessions(@Param("equityCode") String equityCode,
                              @Param("itemCode") String itemCode);

    /**
     * 统计某权益某服务项目的年度已消费次数（只数已完成且当年的 session）。
     *
     * <p>年度配额（quota_type=2）用此方法：只统计 quota_reset_year = 指定年份的消费。
     * 跨年后年度重置任务会把旧年的 quota_reset_year 推进到今年，旧消费不再计入今年配额。
     *
     * @param equityCode 权益编码
     * @param itemCode   服务项目编码
     * @param year       统计年份
     * @return 该年度已消费次数
     */
    @Select("SELECT COUNT(*) FROM service_session " +
            "WHERE equity_code = #{equityCode} AND item_code = #{itemCode} " +
            "AND used_count = 1 AND deleted = 0 " +
            // ServiceSessionEvent.STATUS_COMPLETED = 6
            "AND session_status = 6 " +
            "AND quota_type = 2 AND quota_reset_year = #{year}")
    int countConsumedSessionsAnnual(@Param("equityCode") String equityCode,
                                    @Param("itemCode") String itemCode,
                                    @Param("year") int year);

    /**
     * 年度配额重置（当前实现为空操作保留，详见 javadoc）。
     *
     * <p><b>设计说明</b>：年度配额（quota_type=2）的隔离靠 session 创建时的 quota_reset_year 快照 +
     * {@link #countConsumedSessionsAnnual} 按 quota_reset_year = 当前年 统计天然实现：
     * <ul>
     *   <li>2026 年创建的 session → quota_reset_year=2026，完成used_count=1</li>
     *   <li>countConsumedSessionsAnnual(year=2026) 数 quota_reset_year=2026 → 今年的消费</li>
     *   <li>跨年到 2027：新 session quota_reset_year=2027，countConsumedSessionsAnnual(year=2027) 只数 2027 行</li>
     *   <li>去年的 quota_reset_year=2026 行不会被今年统计匹配 → 年度配额自动重置</li>
     * </ul>
     *
     * <p>因此年度重置<b>不需要修改任何数据</b>——查询层天然按年份隔离。
     * 此方法保留为兼容 QuotaResetScheduler 调用 + 未来可能的清理需求（如归档去年数据）。
     *
     * @param currentYear 当前年份
     * @return 受影响行数（当前始终 0）
     */
    @Update("UPDATE service_session SET updated_at = NOW(), updater = 'quota-reset-job' " +
            "WHERE quota_type = 2 AND deleted = 0 " +
            // 当前实现不修改任何业务字段，仅预留扩展点
            "AND session_status = -1")  // -1 不匹配任何状态，确保 0 行影响
    int resetAnnualQuota(@Param("currentYear") int currentYear);
}
