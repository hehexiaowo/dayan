package com.dayan.park.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.park.entity.ParkInfo;
import com.dayan.park.vo.ParkCardVO;
import com.dayan.park.vo.RegionCenterVO;
import com.dayan.park.vo.RegionItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ParkInfoMapper extends BaseMapper<ParkInfo> {

    /**
     * 按分类(ability_type 集合)聚合省级机构数量。
     * 过滤：已发布(is_published=1) + 已上线(operate_status=1) + 未删除 + province_code 非空。
     * 按 province_code GROUP BY（自动合并"北京"和"北京市"两种写法）。
     */
    @Select("""
            <script>
            SELECT province_code AS code, MAX(province) AS name, COUNT(*) AS count
            FROM park_info
            WHERE deleted = 0 AND is_published = 1 AND operate_status = 1
              AND province_code IS NOT NULL AND province_code != ''
              AND ability_type IN
              <foreach collection="abilityTypes" item="t" open="(" separator="," close=")">#{t}</foreach>
            GROUP BY province_code
            ORDER BY count DESC, code
            </script>
            """)
    List<RegionItem> selectProvinceList(@Param("abilityTypes") List<Integer> abilityTypes);

    /**
     * 按分类 + 省份聚合市级机构数量。
     */
    @Select("""
            <script>
            SELECT city_code AS code, MAX(city) AS name, COUNT(*) AS count
            FROM park_info
            WHERE deleted = 0 AND is_published = 1 AND operate_status = 1
              AND province_code = #{provinceCode}
              AND city_code IS NOT NULL AND city_code != ''
              AND ability_type IN
              <foreach collection="abilityTypes" item="t" open="(" separator="," close=")">#{t}</foreach>
            GROUP BY city_code
            ORDER BY count DESC, code
            </script>
            """)
    List<RegionItem> selectCityList(@Param("abilityTypes") List<Integer> abilityTypes,
                                     @Param("provinceCode") String provinceCode);

    /**
     * 按分类 + 省 + 市聚合区县级机构数量。
     */
    @Select("""
            <script>
            SELECT district_code AS code, MAX(district) AS name, COUNT(*) AS count
            FROM park_info
            WHERE deleted = 0 AND is_published = 1 AND operate_status = 1
              AND province_code = #{provinceCode}
              AND city_code = #{cityCode}
              AND district_code IS NOT NULL AND district_code != ''
              AND ability_type IN
              <foreach collection="abilityTypes" item="t" open="(" separator="," close=")">#{t}</foreach>
            GROUP BY district_code
            ORDER BY count DESC, code
            </script>
            """)
    List<RegionItem> selectDistrictList(@Param("abilityTypes") List<Integer> abilityTypes,
                                         @Param("provinceCode") String provinceCode,
                                         @Param("cityCode") String cityCode);

    /**
     * 按分类 + 省 + 市 + 区查询机构卡片列表。
     */
    @Select("""
            <script>
            SELECT park_code, full_name, short_name, address,
                   province, province_code, city, city_code, district, district_code,
                   longitude, latitude, total_beds, available_beds,
                   min_price_display, max_price_display, price_unit,
                   operate_status, ability_type_description
            FROM park_info
            WHERE deleted = 0 AND is_published = 1 AND operate_status = 1
              AND province_code = #{provinceCode}
              AND city_code = #{cityCode}
              AND district_code = #{districtCode}
              AND ability_type IN
              <foreach collection="abilityTypes" item="t" open="(" separator="," close=")">#{t}</foreach>
            ORDER BY sort_order ASC, id ASC
            LIMIT 200
            </script>
            """)
    List<ParkCardVO> selectParkCardList(@Param("abilityTypes") List<Integer> abilityTypes,
                                         @Param("provinceCode") String provinceCode,
                                         @Param("cityCode") String cityCode,
                                         @Param("districtCode") String districtCode);

    /**
     * 当前筛选范围内机构坐标的平均中心点（地图定位用）。
     * 仅统计有坐标的已上线机构；level 越深入传的 code 越多（与 drillRegion 同参）。
     */
    @Select("""
            <script>
            SELECT AVG(longitude) AS centerLng, AVG(latitude) AS centerLat
            FROM park_info
            WHERE deleted = 0 AND is_published = 1 AND operate_status = 1
              AND longitude IS NOT NULL AND latitude IS NOT NULL
              <if test="provinceCode != null and provinceCode != ''">AND province_code = #{provinceCode}</if>
              <if test="cityCode != null and cityCode != ''">AND city_code = #{cityCode}</if>
              <if test="districtCode != null and districtCode != ''">AND district_code = #{districtCode}</if>
              AND ability_type IN
              <foreach collection="abilityTypes" item="t" open="(" separator="," close=")">#{t}</foreach>
            </script>
            """)
    RegionCenterVO selectRegionCenter(@Param("abilityTypes") List<Integer> abilityTypes,
                                      @Param("provinceCode") String provinceCode,
                                      @Param("cityCode") String cityCode,
                                      @Param("districtCode") String districtCode);
}
