package com.dayan.park.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Agent 端机构查询-区域下钻参数。
 *
 * <p>下钻流程：category + level=province → 选省 → level=city + provinceCode → 选市
 * → level=district + provinceCode + cityCode → 选区 → level=park + districtCode → 机构清单。
 * 直辖市(北京/上海/天津/重庆)由前端判断跳过 city 层。
 */
@Data
public class RegionQueryDTO {

    /** 分类：vital=活力长居, care=照护长居, sojourn=旅居养老 */
    @NotBlank(message = "分类不能为空")
    @Pattern(regexp = "vital|care|sojourn", message = "分类只能是 vital/care/sojourn")
    private String category;

    /** 当前下钻层级：province/city/district/park */
    @NotBlank(message = "层级不能为空")
    @Pattern(regexp = "province|city|district|park", message = "层级只能是 province/city/district/park")
    private String level;

    /** 省级行政区划码，level=city/district/park 时必传 */
    private String provinceCode;

    /** 市级行政区划码，level=district/park 时必传 */
    private String cityCode;

    /** 区县行政区划码，level=park 时必传 */
    private String districtCode;
}
