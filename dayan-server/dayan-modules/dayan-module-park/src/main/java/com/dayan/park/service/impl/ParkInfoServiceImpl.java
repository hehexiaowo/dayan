package com.dayan.park.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.code.BusinessCode;
import com.dayan.common.core.code.CodeGenerator;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.statemachine.StateMachineEngine;
import com.dayan.common.core.util.HtmlSanitizer;
import com.dayan.park.dto.ParkInfoCreateDTO;
import com.dayan.park.dto.ParkInfoQueryDTO;
import com.dayan.park.dto.ParkInfoUpdateDTO;
import com.dayan.park.entity.ParkInfo;
import com.dayan.park.entity.SupplierInfoView;
import com.dayan.park.mapper.ParkInfoMapper;
import com.dayan.park.mapper.SupplierInfoViewMapper;
import com.dayan.park.service.ParkInfoService;
import com.dayan.park.vo.ParkInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import org.springframework.util.StringUtils;

/**
 * 机构主信息（park_info）服务实现。
 *
 * <p>平台共享表，查询不带 channel_code。
 *
 * <p>状态机接入（PARK_SM）：operate_status 取值 0=待审核 / 1=已上线 / 2=已下架 / 3=暂停营业；
 * PARK_SM 规则由 system 模块的 {@code StateMachineWarmUpRunner} 启动时全量加载到 Redis，
 * 本服务仅调用 {@link StateMachineEngine#transition} 校验并取得 to 状态后落库，
 * 同时联动维护 is_published（operate_status==1 时为 1，其余为 0）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParkInfoServiceImpl implements ParkInfoService {

    /** PARK_SM 状态机域标识（machine_code） */
    private static final String SM_DOMAIN = "PARK_SM";
    /** 供应商已合作状态值（对齐 DDL：status=1=已合作；任务 1 修正后语义） */
    private static final int SUPPLIER_STATUS_APPROVED = 1;
    /** operate_status=已上线 */
    private static final int OPERATE_STATUS_ONLINE = 1;
    /** operate_status 初始值：待审核 */
    private static final int OPERATE_STATUS_DEFAULT = 0;

    private final ParkInfoMapper parkInfoMapper;
    private final SupplierInfoViewMapper supplierInfoViewMapper;
    private final CodeGenerator codeGenerator;
    private final StateMachineEngine stateMachineEngine;

    @Override
    public PageResult<ParkInfoVO> page(ParkInfoQueryDTO query) {
        LambdaQueryWrapper<ParkInfo> wrapper = buildQueryWrapper(query);
        Page<ParkInfo> page = parkInfoMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ParkInfoVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ParkInfoVO> list(ParkInfoQueryDTO query) {
        return parkInfoMapper.selectList(buildQueryWrapper(query)).stream()
                .sorted(Comparator
                        .comparingInt((ParkInfo p) -> p.getSortOrder() == null ? 0 : p.getSortOrder())
                        .thenComparingLong(p -> p.getId() == null ? 0L : p.getId()))
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public ParkInfoVO getDetail(String parkCode) {
        return toVO(requirePark(parkCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(ParkInfoCreateDTO dto) {
        // 供应商关联校验
        validateSupplier(dto.getSupplierCode());

        String parkCode = codeGenerator.generate(BusinessCode.PARK);

        ParkInfo entity = new ParkInfo();
        entity.setParkCode(parkCode);
        entity.setFullName(dto.getFullName());
        entity.setShortName(dto.getShortName());
        entity.setSupplierCode(dto.getSupplierCode());
        entity.setBrand(dto.getBrand());
        entity.setBrandIntroduction(dto.getBrandIntroduction());
        entity.setBrandLogo(dto.getBrandLogo());
        entity.setOperationSubject(dto.getOperationSubject());
        entity.setOperationSubjectDescription(dto.getOperationSubjectDescription());
        entity.setImportantShareholders(dto.getImportantShareholders());
        entity.setPartnerCompany(dto.getPartnerCompany());
        entity.setBusinessLicenseNo(dto.getBusinessLicenseNo());
        entity.setBusinessBd(dto.getBusinessBd());
        entity.setAbilityType(dto.getAbilityType());
        entity.setAbilityTypeDescription(dto.getAbilityTypeDescription());
        entity.setNetworkTags(
                dto.getNetworkTags() != null ? String.join(",", dto.getNetworkTags()) : null);
        entity.setVitalConfig(dto.getVitalConfig());
        entity.setCareConfig(dto.getCareConfig());
        entity.setSojournConfig(dto.getSojournConfig());
        entity.setNatureType(dto.getNatureType());
        entity.setNatureTypeDescription(dto.getNatureTypeDescription());
        entity.setSpecialtyTag(dto.getSpecialtyTag());
        entity.setDayanLevel(dto.getDayanLevel());
        entity.setProvince(dto.getProvince());
        entity.setProvinceCode(dto.getProvinceCode());
        entity.setCity(dto.getCity());
        entity.setCityCode(dto.getCityCode());
        entity.setDistrict(dto.getDistrict());
        entity.setDistrictCode(dto.getDistrictCode());
        entity.setAddress(dto.getAddress());
        // 坐标校验
        validateCoordinate(dto.getLongitude(), dto.getLatitude());
        entity.setLongitude(dto.getLongitude());
        entity.setLatitude(dto.getLatitude());
        entity.setServiceHotline(dto.getServiceHotline());
        entity.setBaseDescription(HtmlSanitizer.clean(dto.getBaseDescription()));
        entity.setSpecialtyDescription(HtmlSanitizer.clean(dto.getSpecialtyDescription()));
        entity.setTotalArea(dto.getTotalArea());
        entity.setBuildingArea(dto.getBuildingArea());
        entity.setGreenAreaRate(dto.getGreenAreaRate());
        entity.setTotalBeds(dto.getTotalBeds());
        entity.setAvailableBeds(dto.getAvailableBeds());
        entity.setOccupancyRate(dto.getOccupancyRate());
        entity.setStaffCount(dto.getStaffCount());
        entity.setNurseCount(dto.getNurseCount());
        entity.setNursePatientRatio(dto.getNursePatientRatio());
        entity.setMinPriceDisplay(dto.getMinPriceDisplay());
        entity.setMaxPriceDisplay(dto.getMaxPriceDisplay());
        entity.setPriceUnit(dto.getPriceUnit());
        entity.setCheckInAgeMin(dto.getCheckInAgeMin());
        entity.setCheckInAgeMax(dto.getCheckInAgeMax());
        entity.setCheckInDescription(dto.getCheckInDescription());
        entity.setDepositAmount(dto.getDepositAmount());
        entity.setDepositDescription(dto.getDepositDescription());
        entity.setContractPeriod(dto.getContractPeriod());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setIsHot(dto.getIsHot() == null ? 0 : dto.getIsHot());
        entity.setSubScript(dto.getSubScript());
        entity.setRemark(dto.getRemark());

        // 初始状态：待审核，未发布
        entity.setOperateStatus(OPERATE_STATUS_DEFAULT);
        entity.setIsPublished(0);
        entity.setViewCount(0);
        entity.setCollectCount(0);

        parkInfoMapper.insert(entity);
        log.info("创建机构成功: parkCode={}, supplierCode={}", parkCode, dto.getSupplierCode());
        return parkCode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String parkCode, ParkInfoUpdateDTO dto) {
        ParkInfo existing = requirePark(parkCode);
        ParkInfo update = new ParkInfo();
        update.setId(existing.getId());

        if (dto.getFullName() != null) update.setFullName(dto.getFullName());
        if (dto.getShortName() != null) update.setShortName(dto.getShortName());
        if (dto.getSupplierCode() != null) {
            validateSupplier(dto.getSupplierCode());
            update.setSupplierCode(dto.getSupplierCode());
        }
        if (dto.getBrand() != null) update.setBrand(dto.getBrand());
        if (dto.getBrandIntroduction() != null) update.setBrandIntroduction(dto.getBrandIntroduction());
        if (dto.getBrandLogo() != null) update.setBrandLogo(dto.getBrandLogo());
        if (dto.getOperationSubject() != null) update.setOperationSubject(dto.getOperationSubject());
        if (dto.getOperationSubjectDescription() != null)
            update.setOperationSubjectDescription(dto.getOperationSubjectDescription());
        if (dto.getImportantShareholders() != null) update.setImportantShareholders(dto.getImportantShareholders());
        if (dto.getPartnerCompany() != null) update.setPartnerCompany(dto.getPartnerCompany());
        if (dto.getBusinessLicenseNo() != null) update.setBusinessLicenseNo(dto.getBusinessLicenseNo());
        if (dto.getBusinessBd() != null) update.setBusinessBd(dto.getBusinessBd());
        if (dto.getAbilityType() != null) update.setAbilityType(dto.getAbilityType());
        if (dto.getAbilityTypeDescription() != null)
            update.setAbilityTypeDescription(dto.getAbilityTypeDescription());
        if (dto.getNetworkTags() != null)
            update.setNetworkTags(String.join(",", dto.getNetworkTags()));
        if (dto.getVitalConfig() != null) update.setVitalConfig(dto.getVitalConfig());
        if (dto.getCareConfig() != null) update.setCareConfig(dto.getCareConfig());
        if (dto.getSojournConfig() != null) update.setSojournConfig(dto.getSojournConfig());
        if (dto.getNatureType() != null) update.setNatureType(dto.getNatureType());
        if (dto.getNatureTypeDescription() != null)
            update.setNatureTypeDescription(dto.getNatureTypeDescription());
        if (dto.getSpecialtyTag() != null) update.setSpecialtyTag(dto.getSpecialtyTag());
        if (dto.getDayanLevel() != null) update.setDayanLevel(dto.getDayanLevel());
        if (dto.getProvince() != null) update.setProvince(dto.getProvince());
        if (dto.getProvinceCode() != null) update.setProvinceCode(dto.getProvinceCode());
        if (dto.getCity() != null) update.setCity(dto.getCity());
        if (dto.getCityCode() != null) update.setCityCode(dto.getCityCode());
        if (dto.getDistrict() != null) update.setDistrict(dto.getDistrict());
        if (dto.getDistrictCode() != null) update.setDistrictCode(dto.getDistrictCode());
        if (dto.getAddress() != null) update.setAddress(dto.getAddress());
        if (dto.getLongitude() != null || dto.getLatitude() != null) {
            validateCoordinate(
                    dto.getLongitude() != null ? dto.getLongitude() : existing.getLongitude(),
                    dto.getLatitude() != null ? dto.getLatitude() : existing.getLatitude());
            if (dto.getLongitude() != null) update.setLongitude(dto.getLongitude());
            if (dto.getLatitude() != null) update.setLatitude(dto.getLatitude());
        }
        if (dto.getServiceHotline() != null) update.setServiceHotline(dto.getServiceHotline());
        if (dto.getBaseDescription() != null) update.setBaseDescription(HtmlSanitizer.clean(dto.getBaseDescription()));
        if (dto.getSpecialtyDescription() != null) update.setSpecialtyDescription(HtmlSanitizer.clean(dto.getSpecialtyDescription()));
        if (dto.getTotalArea() != null) update.setTotalArea(dto.getTotalArea());
        if (dto.getBuildingArea() != null) update.setBuildingArea(dto.getBuildingArea());
        if (dto.getGreenAreaRate() != null) update.setGreenAreaRate(dto.getGreenAreaRate());
        if (dto.getTotalBeds() != null) update.setTotalBeds(dto.getTotalBeds());
        if (dto.getAvailableBeds() != null) update.setAvailableBeds(dto.getAvailableBeds());
        if (dto.getOccupancyRate() != null) update.setOccupancyRate(dto.getOccupancyRate());
        if (dto.getStaffCount() != null) update.setStaffCount(dto.getStaffCount());
        if (dto.getNurseCount() != null) update.setNurseCount(dto.getNurseCount());
        if (dto.getNursePatientRatio() != null) update.setNursePatientRatio(dto.getNursePatientRatio());
        if (dto.getMinPriceDisplay() != null) update.setMinPriceDisplay(dto.getMinPriceDisplay());
        if (dto.getMaxPriceDisplay() != null) update.setMaxPriceDisplay(dto.getMaxPriceDisplay());
        if (dto.getPriceUnit() != null) update.setPriceUnit(dto.getPriceUnit());
        if (dto.getCheckInAgeMin() != null) update.setCheckInAgeMin(dto.getCheckInAgeMin());
        if (dto.getCheckInAgeMax() != null) update.setCheckInAgeMax(dto.getCheckInAgeMax());
        if (dto.getCheckInDescription() != null) update.setCheckInDescription(dto.getCheckInDescription());
        if (dto.getDepositAmount() != null) update.setDepositAmount(dto.getDepositAmount());
        if (dto.getDepositDescription() != null) update.setDepositDescription(dto.getDepositDescription());
        if (dto.getContractPeriod() != null) update.setContractPeriod(dto.getContractPeriod());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        if (dto.getIsHot() != null) update.setIsHot(dto.getIsHot());
        if (dto.getSubScript() != null) update.setSubScript(dto.getSubScript());
        if (dto.getRemark() != null) update.setRemark(dto.getRemark());

        parkInfoMapper.updateById(update);
        log.info("更新机构成功: parkCode={}", parkCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String parkCode) {
        ParkInfo existing = requirePark(parkCode);
        parkInfoMapper.deleteById(existing.getId());
        log.info("删除机构成功: parkCode={}", parkCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer transition(String parkCode, String event) {
        ParkInfo existing = requirePark(parkCode);
        Integer from = existing.getOperateStatus();
        int currentFrom = from == null ? OPERATE_STATUS_DEFAULT : from;

        // G-10：机构上线（approve）前校验供应商仍为已合作状态，防止供应商被驳回后机构仍能上线。
        // 仅对 approve 事件校验（其他事件如 offline/online/suspend/resume 不改变机构对外合法性前提）。
        if ("approve".equals(event)) {
            validateSupplier(existing.getSupplierCode());
        }

        // 调用状态机引擎校验并取得目标状态（PARK_SM 规则已由 system 模块预热到 Redis）
        int to = stateMachineEngine.transition(SM_DOMAIN, currentFrom, event);

        ParkInfo update = new ParkInfo();
        update.setId(existing.getId());
        update.setOperateStatus(to);
        // 联动 is_published：仅已上线(1)对外可见
        update.setIsPublished(to == OPERATE_STATUS_ONLINE ? 1 : 0);
        parkInfoMapper.updateById(update);
        log.info("机构状态流转: parkCode={}, {} --{}--> {}", parkCode, currentFrom, event, to);
        return to;
    }

    // ====== 内部方法 ======

    private LambdaQueryWrapper<ParkInfo> buildQueryWrapper(ParkInfoQueryDTO query) {
        LambdaQueryWrapper<ParkInfo> wrapper = new LambdaQueryWrapper<ParkInfo>()
                .orderByDesc(ParkInfo::getCreatedAt);
        if (query.getParkCode() != null && !query.getParkCode().isEmpty()) {
            wrapper.eq(ParkInfo::getParkCode, query.getParkCode());
        }
        if (query.getFullName() != null && !query.getFullName().isEmpty()) {
            wrapper.like(ParkInfo::getFullName, query.getFullName());
        }
        if (query.getSupplierCode() != null && !query.getSupplierCode().isEmpty()) {
            wrapper.eq(ParkInfo::getSupplierCode, query.getSupplierCode());
        }
        if (query.getCityCode() != null && !query.getCityCode().isEmpty()) {
            wrapper.eq(ParkInfo::getCityCode, query.getCityCode());
        }
        if (query.getAbilityType() != null) {
            wrapper.eq(ParkInfo::getAbilityType, query.getAbilityType());
        }
        if (StringUtils.hasText(query.getNetworkTag())) {
            wrapper.apply("FIND_IN_SET({0}, network_tags)", query.getNetworkTag());
        }
        if (query.getNatureType() != null) {
            wrapper.eq(ParkInfo::getNatureType, query.getNatureType());
        }
        if (query.getDayanLevel() != null) {
            wrapper.eq(ParkInfo::getDayanLevel, query.getDayanLevel());
        }
        if (query.getOperateStatus() != null) {
            wrapper.eq(ParkInfo::getOperateStatus, query.getOperateStatus());
        }
        if (query.getIsPublished() != null) {
            wrapper.eq(ParkInfo::getIsPublished, query.getIsPublished());
        }
        if (query.getIsHot() != null) {
            wrapper.eq(ParkInfo::getIsHot, query.getIsHot());
        }
        return wrapper;
    }

    private ParkInfo requirePark(String parkCode) {
        ParkInfo park = parkInfoMapper.selectOne(new LambdaQueryWrapper<ParkInfo>()
                .eq(ParkInfo::getParkCode, parkCode)
                .last("LIMIT 1"));
        if (park == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "机构不存在: " + parkCode);
        }
        return park;
    }

    /** 校验供应商存在且 status=1（已合作） */
    private void validateSupplier(String supplierCode) {
        if (supplierCode == null || supplierCode.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "供应商编码不能为空");
        }
        SupplierInfoView supplier = supplierInfoViewMapper.selectOne(new LambdaQueryWrapper<SupplierInfoView>()
                .eq(SupplierInfoView::getSupplierCode, supplierCode)
                .last("LIMIT 1"));
        if (supplier == null) {
            throw new BusinessException(ErrorCode.BUSINESS, "供应商不存在: " + supplierCode);
        }
        if (supplier.getStatus() == null || supplier.getStatus() != SUPPLIER_STATUS_APPROVED) {
            throw new BusinessException(ErrorCode.BUSINESS, "供应商未通过审核，无法关联机构: " + supplierCode);
        }
    }

    /** 坐标校验：经度 -180~180，纬度 -90~90 */
    private void validateCoordinate(BigDecimal longitude, BigDecimal latitude) {
        if (longitude == null && latitude == null) {
            return;
        }
        if (longitude == null || latitude == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "经度与纬度必须同时提供");
        }
        validateRange(longitude, "经度", -180, 180);
        validateRange(latitude, "纬度", -90, 90);
    }

    private void validateRange(BigDecimal value, String label, double min, double max) {
        double v = value.doubleValue();
        if (v < min || v > max) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    label + "超出范围(" + min + "~" + max + "): " + v);
        }
    }

    private ParkInfoVO toVO(ParkInfo entity) {
        ParkInfoVO vo = new ParkInfoVO();
        vo.setId(entity.getId());
        vo.setParkCode(entity.getParkCode());
        vo.setFullName(entity.getFullName());
        vo.setShortName(entity.getShortName());
        vo.setSupplierCode(entity.getSupplierCode());
        vo.setBrand(entity.getBrand());
        vo.setBrandIntroduction(entity.getBrandIntroduction());
        vo.setBrandLogo(entity.getBrandLogo());
        vo.setOperationSubject(entity.getOperationSubject());
        vo.setOperationSubjectDescription(entity.getOperationSubjectDescription());
        vo.setImportantShareholders(entity.getImportantShareholders());
        vo.setPartnerCompany(entity.getPartnerCompany());
        vo.setBusinessLicenseNo(entity.getBusinessLicenseNo());
        vo.setBusinessBd(entity.getBusinessBd());
        vo.setAbilityType(entity.getAbilityType());
        vo.setAbilityTypeDescription(entity.getAbilityTypeDescription());
        vo.setNetworkTags(
                entity.getNetworkTags() != null
                        ? Arrays.asList(entity.getNetworkTags().split(","))
                        : null);
        vo.setNatureType(entity.getNatureType());
        vo.setNatureTypeDescription(entity.getNatureTypeDescription());
        vo.setSpecialtyTag(entity.getSpecialtyTag());
        vo.setDayanLevel(entity.getDayanLevel());
        vo.setProvince(entity.getProvince());
        vo.setProvinceCode(entity.getProvinceCode());
        vo.setCity(entity.getCity());
        vo.setCityCode(entity.getCityCode());
        vo.setDistrict(entity.getDistrict());
        vo.setDistrictCode(entity.getDistrictCode());
        vo.setAddress(entity.getAddress());
        vo.setLongitude(entity.getLongitude());
        vo.setLatitude(entity.getLatitude());
        vo.setServiceHotline(entity.getServiceHotline());
        vo.setBaseDescription(entity.getBaseDescription());
        vo.setSpecialtyDescription(entity.getSpecialtyDescription());
        vo.setTotalArea(entity.getTotalArea());
        vo.setBuildingArea(entity.getBuildingArea());
        vo.setGreenAreaRate(entity.getGreenAreaRate());
        vo.setTotalBeds(entity.getTotalBeds());
        vo.setAvailableBeds(entity.getAvailableBeds());
        vo.setOccupancyRate(entity.getOccupancyRate());
        vo.setStaffCount(entity.getStaffCount());
        vo.setNurseCount(entity.getNurseCount());
        vo.setNursePatientRatio(entity.getNursePatientRatio());
        vo.setMinPriceDisplay(entity.getMinPriceDisplay());
        vo.setMaxPriceDisplay(entity.getMaxPriceDisplay());
        vo.setPriceUnit(entity.getPriceUnit());
        vo.setCheckInAgeMin(entity.getCheckInAgeMin());
        vo.setCheckInAgeMax(entity.getCheckInAgeMax());
        vo.setCheckInDescription(entity.getCheckInDescription());
        vo.setDepositAmount(entity.getDepositAmount());
        vo.setDepositDescription(entity.getDepositDescription());
        vo.setContractPeriod(entity.getContractPeriod());
        vo.setSortOrder(entity.getSortOrder());
        vo.setIsHot(entity.getIsHot());
        vo.setSubScript(entity.getSubScript());
        vo.setOperateStatus(entity.getOperateStatus());
        vo.setOpeningTime(entity.getOpeningTime());
        vo.setOnlineTime(entity.getOnlineTime());
        vo.setOfflineTime(entity.getOfflineTime());
        vo.setAddPlatformTime(entity.getAddPlatformTime());
        vo.setIsPublished(entity.getIsPublished());
        vo.setViewCount(entity.getViewCount());
        vo.setCollectCount(entity.getCollectCount());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
