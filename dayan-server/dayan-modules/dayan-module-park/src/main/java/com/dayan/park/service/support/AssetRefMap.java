package com.dayan.park.service.support;

import java.util.List;

/**
 * 素材引用地图：声明哪些业务表哪些列可能存放素材 OSS key。
 * 素材删除前按此逐表反查，命中则拒删（列名均来自 db/migration 各表 DDL，改表需同步维护）。
 */
public final class AssetRefMap {

    /** 匹配方式：EXACT=列值全等；JSON_LIKE=JSON 数组元素（LIKE "key" 带引号）；HTML_LIKE=富文本内嵌（LIKE %key%） */
    public enum Match { EXACT, JSON_LIKE, HTML_LIKE }

    /**
     * @param table  表名
     * @param column 列名
     * @param label  拒删提示里的展示名
     * @param match  匹配方式
     */
    public record RefCheck(String table, String column, String label, Match match) {}

    public static final List<RefCheck> CHECKS = List.of(
            new RefCheck("park_info", "brand_logo", "机构-品牌Logo", Match.EXACT),
            new RefCheck("park_facility_type", "cover_image", "设施-封面图", Match.EXACT),
            new RefCheck("park_facility_type", "images", "设施-图片集", Match.JSON_LIKE),
            new RefCheck("park_service_type", "cover_image", "服务项目-图片", Match.EXACT),
            new RefCheck("park_room_type", "cover_image", "房型-封面图", Match.EXACT),
            new RefCheck("park_room_type", "images", "房型-图片集", Match.JSON_LIKE),
            new RefCheck("park_food_type", "cover_image", "餐饮方案-封面图", Match.EXACT),
            new RefCheck("park_adviser", "adviser_image", "顾问-头像", Match.EXACT),
            new RefCheck("park_display_block", "images", "展示板块-配图", Match.JSON_LIKE),
            new RefCheck("park_display_block", "content", "展示板块-正文内嵌图", Match.HTML_LIKE),
            new RefCheck("park_asset", "cover_url", "素材-视频封面", Match.EXACT),
            new RefCheck("park_asset", "thumbnail_url", "素材-VR缩略图", Match.EXACT),
            new RefCheck("goods_info", "cover_image", "商品-封面图", Match.EXACT),
            new RefCheck("goods_info", "image_urls", "商品-图片集", Match.JSON_LIKE),
            new RefCheck("goods_info", "video_url", "商品-视频", Match.EXACT),
            new RefCheck("scene_info", "cover_image", "场景-封面图", Match.EXACT),
            new RefCheck("scene_info", "image_urls", "场景-图片集", Match.JSON_LIKE),
            new RefCheck("scene_info", "video_url", "场景-视频", Match.EXACT),
            new RefCheck("course_info", "cover_image", "课程-封面图", Match.EXACT),
            new RefCheck("course_info", "video_url", "课程-视频", Match.EXACT),
            new RefCheck("content_info", "cover_image", "内容-封面图", Match.EXACT),
            new RefCheck("content_info", "content_body", "内容-正文内嵌图", Match.HTML_LIKE),
            new RefCheck("content_media", "media_url", "内容-媒体资源", Match.EXACT));

    private AssetRefMap() {}
}
