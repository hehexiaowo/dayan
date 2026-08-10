package com.dayan.park.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.park.dto.RegionQueryDTO;
import com.dayan.park.entity.ParkAsset;
import com.dayan.park.entity.ParkCareType;
import com.dayan.park.entity.ParkDisplayBlock;
import com.dayan.park.entity.ParkFacilityType;
import com.dayan.park.entity.ParkFoodType;
import com.dayan.park.entity.ParkInfo;
import com.dayan.park.entity.ParkPeriphery;
import com.dayan.park.entity.ParkPricing;
import com.dayan.park.entity.ParkRoomType;
import com.dayan.park.entity.ParkScore;
import com.dayan.park.entity.ParkServiceType;
import com.dayan.park.mapper.ParkAssetMapper;
import com.dayan.park.mapper.ParkCareTypeMapper;
import com.dayan.park.mapper.ParkDisplayBlockMapper;
import com.dayan.park.mapper.ParkFacilityTypeMapper;
import com.dayan.park.mapper.ParkFoodTypeMapper;
import com.dayan.park.mapper.ParkInfoMapper;
import com.dayan.park.mapper.ParkPeripheryMapper;
import com.dayan.park.mapper.ParkPricingMapper;
import com.dayan.park.mapper.ParkRoomTypeMapper;
import com.dayan.park.mapper.ParkScoreMapper;
import com.dayan.park.mapper.ParkServiceTypeMapper;
import com.dayan.park.service.ParkAgentQueryService;
import com.dayan.park.vo.CategoryCountVO;
import com.dayan.park.vo.ParkAssetVO;
import com.dayan.park.vo.ParkCardVO;
import com.dayan.park.vo.ParkCareTypeVO;
import com.dayan.park.vo.ParkDisplayBlockVO;
import com.dayan.park.vo.ParkFacilityTypeVO;
import com.dayan.park.vo.ParkFoodTypeVO;
import com.dayan.park.vo.ParkFullDetailVO;
import com.dayan.park.vo.ParkInfoVO;
import com.dayan.park.vo.ParkPeripheryVO;
import com.dayan.park.vo.ParkPricingVO;
import com.dayan.park.vo.ParkRoomTypeVO;
import com.dayan.park.vo.ParkScoreVO;
import com.dayan.park.vo.ParkServiceTypeVO;
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
    private final ParkAssetMapper parkAssetMapper;
    private final ParkRoomTypeMapper parkRoomTypeMapper;
    private final ParkPricingMapper parkPricingMapper;
    private final ParkCareTypeMapper parkCareTypeMapper;
    private final ParkFoodTypeMapper parkFoodTypeMapper;
    private final ParkFacilityTypeMapper parkFacilityTypeMapper;
    private final ParkServiceTypeMapper parkServiceTypeMapper;
    private final ParkPeripheryMapper parkPeripheryMapper;
    private final ParkScoreMapper parkScoreMapper;
    private final ParkDisplayBlockMapper parkDisplayBlockMapper;

    /** 活力长居：CCRC */
    private static final List<Integer> VITAL_TYPES = List.of(1);
    /** 照护长居：养老院/CB/认知症/NH */
    private static final List<Integer> CARE_TYPES = List.of(2, 3, 4, 7);
    /** 直辖市 provinceCode（跳过 city 层，面包屑不输出城市段） */
    private static final List<String> MUNICIPALITY_CODES = List.of("110000", "120000", "310000", "500000");

    @Override
    public List<CategoryCountVO> countByCategory() {
        Integer vitalCount = countByAbilityTypes(VITAL_TYPES);
        Integer careCount = countByAbilityTypes(CARE_TYPES);
        // sojourn 走独立查询路径（基于 park_room_type.stay_type=2 联表）
        Integer sojournCount = parkInfoMapper.selectProvinceListForSojourn().stream()
                .mapToInt(RegionItem::getCount)
                .sum();

        return List.of(
                new CategoryCountVO("vital", "活力长居", vitalCount, true),
                new CategoryCountVO("care", "照护长居", careCount, true),
                new CategoryCountVO("sojourn", "旅居养老", sojournCount, true)
        );
    }

    @Override
    public RegionDrillResult drillRegion(RegionQueryDTO query) {
        // sojourn 走独立查询路径（基于 stay_type 联表，不走 ability_type）
        if ("sojourn".equals(query.getCategory())) {
            return drillRegionForSojourn(query);
        }

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

    /**
     * sojourn 独立下钻路径：基于 park_room_type.stay_type=2 联表查询，不走 ability_type。
     * 结构与 drillRegion 一致（4 个 level 分支 + 面包屑 + 中心点）。
     */
    private RegionDrillResult drillRegionForSojourn(RegionQueryDTO query) {
        RegionDrillResult result = new RegionDrillResult();
        result.setLevel(query.getLevel());

        switch (query.getLevel()) {
            case "province" -> {
                List<RegionItem> provinces = parkInfoMapper.selectProvinceListForSojourn();
                result.setItems(provinces);
                result.setBreadcrumb("旅居养老");
            }
            case "city" -> {
                List<RegionItem> cities = parkInfoMapper.selectCityListForSojourn(query.getProvinceCode());
                result.setItems(cities);
                result.setBreadcrumb("旅居养老 / " + extractProvinceName(query.getProvinceCode()));
            }
            case "district" -> {
                List<RegionItem> districts = parkInfoMapper.selectDistrictListForSojourn(
                        query.getProvinceCode(), query.getCityCode());
                result.setItems(districts);
                String crumb = "旅居养老 / " + extractProvinceName(query.getProvinceCode());
                if (!MUNICIPALITY_CODES.contains(query.getProvinceCode())) {
                    crumb += " / " + extractCityName(query.getCityCode(), districts);
                }
                result.setBreadcrumb(crumb);
            }
            case "park" -> {
                List<ParkCardVO> parks = parkInfoMapper.selectParkCardListForSojourn(
                        query.getProvinceCode(), query.getCityCode(), query.getDistrictCode());
                result.setParkList(parks);
                String crumb = "旅居养老 / " + extractProvinceName(query.getProvinceCode());
                if (!MUNICIPALITY_CODES.contains(query.getProvinceCode())) {
                    crumb += " / " + extractCityName(query.getCityCode(), null);
                }
                crumb += " / " + extractDistrictName(query.getDistrictCode(), parks);
                result.setBreadcrumb(crumb);
            }
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的层级: " + query.getLevel());
        }

        // 地图中心点
        RegionCenterVO center = parkInfoMapper.selectRegionCenterForSojourn(
                query.getProvinceCode(), query.getCityCode(), query.getDistrictCode());
        if (center != null) {
            result.setCenterLng(center.getCenterLng());
            result.setCenterLat(center.getCenterLat());
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

    @Override
    public ParkFullDetailVO getFullDetail(String parkCode) {
        // 1. 查主表（仅已发布+已上线+未删除）
        ParkInfo park = parkInfoMapper.selectOne(new LambdaQueryWrapper<ParkInfo>()
                .eq(ParkInfo::getParkCode, parkCode)
                .eq(ParkInfo::getIsPublished, 1)
                .eq(ParkInfo::getOperateStatus, 1)
                .eq(ParkInfo::getDeleted, 0)
                .last("LIMIT 1"));
        if (park == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "机构不存在或未上线: " + parkCode);
        }

        ParkFullDetailVO vo = new ParkFullDetailVO();

        // 2. 主信息
        ParkInfoVO infoVO = new ParkInfoVO();
        BeanUtils.copyProperties(park, infoVO);
        vo.setParkInfo(infoVO);

        // 3. 子实体（条件：park_code + status=1 + deleted=0，deleted 由 @TableLogic 自动过滤）
        vo.setAssets(copyList(
                parkAssetMapper.selectList(activeWrapper(parkCode, ParkAsset::getParkCode, ParkAsset::getStatus)),
                ParkAssetVO::new));

        vo.setRoomTypes(copyList(
                parkRoomTypeMapper.selectList(activeWrapper(parkCode, ParkRoomType::getParkCode, ParkRoomType::getStatus)),
                ParkRoomTypeVO::new));

        vo.setPricingList(copyList(
                parkPricingMapper.selectList(activeWrapper(parkCode, ParkPricing::getParkCode, ParkPricing::getStatus)),
                ParkPricingVO::new));

        vo.setCareTypes(copyList(
                parkCareTypeMapper.selectList(activeWrapper(parkCode, ParkCareType::getParkCode, ParkCareType::getStatus)),
                ParkCareTypeVO::new));

        vo.setFoodTypes(copyList(
                parkFoodTypeMapper.selectList(activeWrapper(parkCode, ParkFoodType::getParkCode, ParkFoodType::getStatus)),
                ParkFoodTypeVO::new));

        vo.setFacilityTypes(copyList(
                parkFacilityTypeMapper.selectList(activeWrapper(parkCode, ParkFacilityType::getParkCode, ParkFacilityType::getStatus)),
                ParkFacilityTypeVO::new));

        vo.setServiceTypes(copyList(
                parkServiceTypeMapper.selectList(activeWrapper(parkCode, ParkServiceType::getParkCode, ParkServiceType::getStatus)),
                ParkServiceTypeVO::new));

        vo.setPeripheries(copyList(
                parkPeripheryMapper.selectList(activeWrapper(parkCode, ParkPeriphery::getParkCode, ParkPeriphery::getStatus)),
                ParkPeripheryVO::new));

        vo.setDisplayBlocks(copyList(
                parkDisplayBlockMapper.selectList(activeWrapper(parkCode, ParkDisplayBlock::getParkCode, ParkDisplayBlock::getStatus)),
                ParkDisplayBlockVO::new));

        // 4. 评分（单条，无 status 字段，只按 parkCode 查）
        ParkScore score = parkScoreMapper.selectOne(new LambdaQueryWrapper<ParkScore>()
                .eq(ParkScore::getParkCode, parkCode)
                .last("LIMIT 1"));
        if (score != null) {
            ParkScoreVO scoreVO = new ParkScoreVO();
            BeanUtils.copyProperties(score, scoreVO);
            vo.setScore(scoreVO);
        }

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
            case "sojourn" -> throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "sojourn 走独立查询路径，不应到达 resolveAbilityTypes");
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

    /**
     * 构建子实体通用查询条件：parkCode + status=1（deleted 由 @TableLogic 自动过滤）。
     */
    private <T> LambdaQueryWrapper<T> activeWrapper(
            String parkCode,
            com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, String> parkCodeGetter,
            com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, Integer> statusGetter) {
        return new LambdaQueryWrapper<T>()
                .eq(parkCodeGetter, parkCode)
                .eq(statusGetter, 1);
    }

    /**
     * 批量 entity → VO 拷贝。
     */
    private <E, V> List<V> copyList(List<E> entities, java.util.function.Supplier<V> voSupplier) {
        if (entities == null || entities.isEmpty()) return List.of();
        return entities.stream().map(e -> {
            V vo = voSupplier.get();
            BeanUtils.copyProperties(e, vo);
            return vo;
        }).toList();
    }
}
