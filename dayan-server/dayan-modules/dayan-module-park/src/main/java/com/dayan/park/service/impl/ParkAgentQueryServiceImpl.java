package com.dayan.park.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.park.dto.RegionQueryDTO;
import com.dayan.park.entity.ParkInfo;
import com.dayan.park.mapper.ParkInfoMapper;
import com.dayan.park.service.ParkAgentQueryService;
import com.dayan.park.vo.CategoryCountVO;
import com.dayan.park.vo.ParkCardVO;
import com.dayan.park.vo.ParkInfoVO;
import com.dayan.park.vo.RegionCenterVO;
import com.dayan.park.vo.RegionDrillResult;
import com.dayan.park.vo.RegionItem;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Agent 端机构查询服务实现。
 */
@Service
@RequiredArgsConstructor
public class ParkAgentQueryServiceImpl implements ParkAgentQueryService {

    private final ParkInfoMapper parkInfoMapper;

    /** 活力长居：CCRC */
    private static final List<Integer> VITAL_TYPES = List.of(1);
    /** 照护长居：养老院/CB/认知症/NH */
    private static final List<Integer> CARE_TYPES = List.of(2, 3, 4, 7);
    /** 直辖市 provinceCode（跳过 city 层，面包屑不输出城市段） */
    private static final List<String> MUNICIPALITY_CODES = List.of("110000", "120000", "310000", "500000");

    @Override
    public List<CategoryCountVO> countByCategory() {
        // 旅居养老当前无数据，固定返回 available=false
        // 前端判断 available=false 时点击只 toast 不跳转
        Integer vitalCount = countByAbilityTypes(VITAL_TYPES);
        Integer careCount = countByAbilityTypes(CARE_TYPES);

        return List.of(
                new CategoryCountVO("vital", "活力长居", vitalCount, true),
                new CategoryCountVO("care", "照护长居", careCount, true),
                new CategoryCountVO("sojourn", "旅居养老", 0, false)
        );
    }

    @Override
    public RegionDrillResult drillRegion(RegionQueryDTO query) {
        List<Integer> abilityTypes = resolveAbilityTypes(query.getCategory());
        RegionDrillResult result = new RegionDrillResult();
        result.setLevel(query.getLevel());

        switch (query.getLevel()) {
            case "province" -> {
                List<RegionItem> provinces = parkInfoMapper.selectProvinceList(abilityTypes);
                result.setItems(provinces);
                result.setBreadcrumb(categoryName(query.getCategory()));
            }
            case "city" -> {
                List<RegionItem> cities = parkInfoMapper.selectCityList(abilityTypes, query.getProvinceCode());
                result.setItems(cities);
                result.setBreadcrumb(categoryName(query.getCategory()) + " / " + provinceName(query.getProvinceCode(), cities));
            }
            case "district" -> {
                List<RegionItem> districts = parkInfoMapper.selectDistrictList(
                        abilityTypes, query.getProvinceCode(), query.getCityCode());
                result.setItems(districts);
                // 直辖市跳过 city 层面包屑（北京 / 北京市 冗余）
                String crumb = categoryName(query.getCategory())
                        + " / " + extractProvinceName(query.getProvinceCode());
                if (!MUNICIPALITY_CODES.contains(query.getProvinceCode())) {
                    crumb += " / " + extractCityName(query.getCityCode(), districts);
                }
                result.setBreadcrumb(crumb);
            }
            case "park" -> {
                List<ParkCardVO> parks = parkInfoMapper.selectParkCardList(
                        abilityTypes, query.getProvinceCode(), query.getCityCode(), query.getDistrictCode());
                result.setParkList(parks);
                String crumb = categoryName(query.getCategory())
                        + " / " + extractProvinceName(query.getProvinceCode());
                if (!MUNICIPALITY_CODES.contains(query.getProvinceCode())) {
                    crumb += " / " + extractCityName(query.getCityCode(), null);
                }
                crumb += " / " + extractDistrictName(query.getDistrictCode(), parks);
                result.setBreadcrumb(crumb);
            }
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的层级: " + query.getLevel());
        }

        // 地图中心点：范围内机构坐标 AVG（sojourn 无数据时跳过，前端用省级表兜底）
        if (!abilityTypes.isEmpty()) {
            RegionCenterVO center = parkInfoMapper.selectRegionCenter(
                    abilityTypes, query.getProvinceCode(), query.getCityCode(), query.getDistrictCode());
            if (center != null) {
                result.setCenterLng(center.getCenterLng());
                result.setCenterLat(center.getCenterLat());
            }
        }

        return result;
    }

    @Override
    public ParkInfoVO getPublishedDetail(String parkCode) {
        // agent 端只查已发布(is_published=1) + 已上线(operate_status=1) + 未删除的机构
        ParkInfo park = parkInfoMapper.selectOne(new LambdaQueryWrapper<ParkInfo>()
                .eq(ParkInfo::getParkCode, parkCode)
                .eq(ParkInfo::getIsPublished, 1)
                .eq(ParkInfo::getOperateStatus, 1)
                .eq(ParkInfo::getDeleted, 0)
                .last("LIMIT 1"));
        if (park == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "机构不存在或未上线: " + parkCode);
        }
        ParkInfoVO vo = new ParkInfoVO();
        BeanUtils.copyProperties(park, vo);
        return vo;
    }

    // ===== 内部方法 =====

    private Integer countByAbilityTypes(List<Integer> abilityTypes) {
        // 复用 province 聚合查询的 count 求和（避免多写一个 count SQL）
        return parkInfoMapper.selectProvinceList(abilityTypes).stream()
                .mapToInt(RegionItem::getCount)
                .sum();
    }

    private List<Integer> resolveAbilityTypes(String category) {
        return switch (category) {
            case "vital" -> VITAL_TYPES;
            case "care" -> CARE_TYPES;
            case "sojourn" -> List.of(); // 旅居无数据
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的分类: " + category);
        };
    }

    private String categoryName(String category) {
        return switch (category) {
            case "vital" -> "活力长居";
            case "care" -> "照护长居";
            case "sojourn" -> "旅居养老";
            default -> category;
        };
    }

    /** 从省份列表中找省份名（province 层下钻 city 时用） */
    private String provinceName(String provinceCode, List<RegionItem> cities) {
        // cities 是城市列表，不含省名；通过 provinceCode 推断
        return extractProvinceName(provinceCode);
    }

    /** 通过 provinceCode 推断省份名（简化处理：从已知映射取） */
    private String extractProvinceName(String provinceCode) {
        if (provinceCode == null) return "";
        return switch (provinceCode) {
            case "110000" -> "北京";
            case "120000" -> "天津";
            case "310000" -> "上海";
            case "500000" -> "重庆";
            case "130000" -> "河北省";
            case "320000" -> "江苏省";
            case "330000" -> "浙江省";
            case "430000" -> "湖南省";
            case "450000" -> "广西壮族自治区";
            case "460000" -> "海南省";
            default -> "未知区域";
        };
    }

    /** 从区县列表/机构列表中找城市名 */
    private String extractCityName(String cityCode, List<RegionItem> districts) {
        if (cityCode == null || cityCode.length() < 2) return "";
        // 直辖市 cityCode 如 110100 对应"北京市"，简化为通用映射
        return switch (cityCode.substring(0, 2)) {
            case "11" -> "北京市";
            case "12" -> "天津市";
            case "31" -> "上海市";
            case "50" -> "重庆市";
            default -> {
                // 非直辖市：无法从 cityCode 简单推断全名，用 code 占位
                // 面包屑只是辅助显示，详情信息在列表数据里
                yield "选择城市";
            }
        };
    }

    /** 从机构列表中找区县名 */
    private String extractDistrictName(String districtCode, List<ParkCardVO> parks) {
        if (parks == null || parks.isEmpty()) return "";
        return parks.stream()
                .filter(p -> districtCode != null && districtCode.equals(p.getDistrictCode()))
                .map(ParkCardVO::getDistrict)
                .findFirst()
                .orElse("");
    }
}
