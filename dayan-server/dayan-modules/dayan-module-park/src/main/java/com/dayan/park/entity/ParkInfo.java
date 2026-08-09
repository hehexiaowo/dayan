package com.dayan.park.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * 表 park_info 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("park_info")
public class ParkInfo extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 机构唯一编码 */
    private String parkCode;

    /** 机构名称 */
    private String fullName;

    /** 机构简称 */
    private String shortName;

    /** 所属供应商编码 */
    private String supplierCode;

    /** 品牌名称 */
    private String brand;

    /** 品牌简介 */
    private String brandIntroduction;

    /** 品牌Logo */
    private String brandLogo;

    /** 运营主体 */
    private String operationSubject;

    /** 运营主体介绍 */
    private String operationSubjectDescription;

    /** 重要股东 */
    private String importantShareholders;

    /** 合作公司主体 */
    private String partnerCompany;

    /** 营业执照号 */
    private String businessLicenseNo;

    /** 商务BD */
    private String businessBd;

    /** 机构类型 */
    private Integer abilityType;

    /** 机构类型描述 */
    private String abilityTypeDescription;

    /** 机构性质 */
    private Integer natureType;

    /** 机构性质描述 */
    private String natureTypeDescription;

    /** 特色标签 */
    private String specialtyTag;

    /** 评级 */
    private Integer dayanLevel;

    /** 省 */
    private String province;

    /** 省编码 */
    private String provinceCode;

    /** 城市 */
    private String city;

    /** 城市编码 */
    private String cityCode;

    /** 区 */
    private String district;

    /** 区编码 */
    private String districtCode;

    /** 具体地址 */
    private String address;

    /** 经度 */
    private BigDecimal longitude;

    /** 纬度 */
    private BigDecimal latitude;

    /** 客服电话 */
    private String serviceHotline;

    /** 机构介绍 */
    private String baseDescription;

    /** 机构特色 */
    private String specialtyDescription;

    /** 占地面积 */
    private String totalArea;

    /** 建筑面积 */
    private String buildingArea;

    /** 绿化率 */
    private String greenAreaRate;

    /** 总床位数 */
    private Integer totalBeds;

    /** 可用床位数 */
    private Integer availableBeds;

    /** 已入住率 */
    private String occupancyRate;

    /** 员工总数 */
    private Integer staffCount;

    /** 护理人员数 */
    private Integer nurseCount;

    /** 护患比 */
    private String nursePatientRatio;

    /** 最低月费 */
    private Integer minPriceDisplay;

    /** 最高月费 */
    private Integer maxPriceDisplay;

    /** 价格单位 */
    private String priceUnit;

    /** 入住最低年龄 */
    private Integer checkInAgeMin;

    /** 入住最高年龄 */
    private Integer checkInAgeMax;

    /** 入住说明 */
    private String checkInDescription;

    /** 押金金额 */
    private BigDecimal depositAmount;

    /** 押金说明 */
    private String depositDescription;

    /** 合同期限 */
    private Integer contractPeriod;

    /** 排序号 */
    private Integer sortOrder;

    /** 平台内评级 */
    private Integer isHot;

    /** 首页角标 */
    private String subScript;

    /** 运营状态（0=待审核, 1=已上线, 2=已下架, 3=暂停营业；PARK_SM 驱动） */
    private Integer operateStatus;

    /** 开业时间 */
    private LocalDateTime openingTime;

    /** 上架时间 */
    private LocalDateTime onlineTime;

    /** 下架时间 */
    private LocalDateTime offlineTime;

    /** 加入平台时间 */
    private LocalDateTime addPlatformTime;

    /** 是否已发布 */
    private Integer isPublished;

    /** 浏览次数 */
    private Integer viewCount;

    /** 收藏次数 */
    private Integer collectCount;

    /** 备注 */
    private String remark;

    /** 乐观锁版本 */
    @Version
    private Long version;
}
