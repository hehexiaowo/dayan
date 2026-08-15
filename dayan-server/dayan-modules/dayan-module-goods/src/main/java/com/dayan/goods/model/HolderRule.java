package com.dayan.goods.model;

import lombok.Data;

/**
 * 权益人构成规则（goods_equity.holder_rule JSON 的结构化形态）。
 *
 * <p>对照权益文档 6 档终身权益的权益人差异：
 * <ul>
 *   <li>个人尊贵版：{self:1, spouse:0, parent:0}</li>
 *   <li>家庭尊享版：{self:1, spouse:1, parent:0}</li>
 *   <li>豪华N人版：{self:1, spouse:1, parent:N-2, designateAtActivation:true}（双方父母中激活时指定）</li>
 *   <li>至尊6人版：{self:1, spouse:1, parent:4}（双方父母全员）</li>
 * </ul>
 * 总人数 total() 应与 goods_equity.person_count 一致；parent 含公婆/岳父母。
 */
@Data
public class HolderRule {

    /** 本人席位（固定 1） */
    private Integer self = 1;

    /** 配偶席位（0 或 1） */
    private Integer spouse = 0;

    /** 父母席位（0~4，含双方父母/公婆/岳父母） */
    private Integer parent = 0;

    /** 父母人选是否须在激活时指定（豪华版 true；至尊版/无父母席位 false） */
    private Boolean designateAtActivation = false;

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }

    /** 权益人总数 = 本人 + 配偶 + 父母 */
    public int total() {
        return nz(self) + nz(spouse) + nz(parent);
    }
}
