package com.dayan.equity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.code.SequenceProvider;
import com.dayan.common.core.crypto.AesGcmUtil;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.statemachine.StateMachineEngine;
import com.dayan.equity.dto.ActivateDTO;
import com.dayan.equity.dto.ChangeDoneDTO;
import com.dayan.equity.dto.ChangeHolderDTO;
import com.dayan.equity.dto.ChangeRollbackDTO;
import com.dayan.equity.dto.EquityDepotQueryDTO;
import com.dayan.equity.dto.OutboundDTO;
import com.dayan.equity.dto.StockInDTO;
import com.dayan.equity.dto.VoidDTO;
import com.dayan.equity.entity.EquityActivate;
import com.dayan.equity.entity.EquityBatch;
import com.dayan.equity.entity.EquityChangeHolder;
import com.dayan.equity.entity.EquityDepot;
import com.dayan.equity.entity.EquityTemplate;
import com.dayan.equity.entity.EquityUsePerson;
import com.dayan.equity.enums.EquityEvent;
import com.dayan.equity.mapper.EquityActivateMapper;
import com.dayan.equity.mapper.EquityChangeHolderMapper;
import com.dayan.equity.mapper.EquityDepotMapper;
import com.dayan.equity.mapper.EquityUsePersonMapper;
import com.dayan.equity.service.EquityBatchService;
import com.dayan.equity.service.EquityDepotService;
import com.dayan.equity.service.EquityTemplateService;
import com.dayan.equity.vo.EquityDepotVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 权益卡/函（equity_depot）服务实现 —— 核心链路。
 *
 * <p>核心链路（所有写操作 {@code @Transactional}，状态变更均经 EQUITY_SM 状态机）：
 * <ul>
 *   <li>{@link #stockIn}     批量入库：生成 N 张权益 + 联动 produced_count/remain_count + 推进批次状态</li>
 *   <li>{@link #outbound}    出库：0→1 + 联动 outbound_count/remain_count</li>
 *   <li>{@link #activate}    激活：1→2 + 写 activate/expire_time + 插激活记录 + 联动 activated_count + 自动建默认使用人</li>
 *   <li>{@link #voidEquity}  作废：0/1→6 + 联动 voided_count/remain_count</li>
 *   <li>{@link #changeHolder} 发起更换：2→7 + 插 change_holder 待处理记录</li>
 *   <li>{@link #changeDone}   完成更换：7→2 + change_status=1 + 切换默认使用人</li>
 *   <li>{@link #changeRollback} 回滚更换：7→2 + change_status=2</li>
 * </ul>
 *
 * <p>加密：{@code card_secret}（随机串 AES-GCM 加密）、{@code use_person_id_card}（明文 AES-GCM 加密）。
 * 因 AES-GCM 每次 IV 随机导致同明文密文不同，身份证唯一校验改为：查同 equity_code 下所有使用人解密后比对
 * （使用人 ≤3，性能可接受）。
 *
 * <p>未使用 {@code @RequiredArgsConstructor}：因 AES 密钥需由 {@code @Value} 在构造时派生，改用显式构造器。
 */
@Slf4j
@Service
public class EquityDepotServiceImpl implements EquityDepotService {

    // ====== 编码常量 ======
    private static final String EQ_PREFIX = "EQ";
    private static final String EQ_SEQ_KEY = "code:seq:EQ:0";
    private static final int EQ_SEQ_WIDTH = 12;

    private static final String AC_PREFIX = "AC";
    private static final String AC_SEQ_KEY = "code:seq:AC:0";
    private static final int AC_SEQ_WIDTH = 10;

    private static final String DY_PREFIX = "DY";   // 卡激活码
    private static final int DY_LEN = 8;
    private static final String BF_PREFIX = "BF";   // 函绑定码
    private static final int BF_LEN = 12;
    /** 随机码查重最大重试次数 */
    private static final int RANDOM_RETRY_MAX = 5;

    // ====== 状态常量 ======
    private static final int BATCH_STATUS_PENDING = 0;
    private static final int BATCH_STATUS_PRODUCING = 1;
    private static final int BATCH_STATUS_COMPLETED = 2;

    private static final String DEFAULT_KEY_PASSWORD = "dayan-default-key";

    private final EquityDepotMapper depotMapper;
    private final EquityActivateMapper activateMapper;
    private final EquityUsePersonMapper usePersonMapper;
    private final EquityChangeHolderMapper changeHolderMapper;
    private final EquityBatchService batchService;
    private final EquityTemplateService templateService;
    private final SequenceProvider sequenceProvider;
    private final StateMachineEngine stateMachineEngine;
    /** AES 密钥 hex（由配置 dayan.aes.key 派生） */
    private final String aesKeyHex;

    public EquityDepotServiceImpl(
            EquityDepotMapper depotMapper,
            EquityActivateMapper activateMapper,
            EquityUsePersonMapper usePersonMapper,
            EquityChangeHolderMapper changeHolderMapper,
            EquityBatchService batchService,
            EquityTemplateService templateService,
            SequenceProvider sequenceProvider,
            StateMachineEngine stateMachineEngine,
            @Value("${dayan.aes.key:}") String configuredKey) {
        this.depotMapper = depotMapper;
        this.activateMapper = activateMapper;
        this.usePersonMapper = usePersonMapper;
        this.changeHolderMapper = changeHolderMapper;
        this.batchService = batchService;
        this.templateService = templateService;
        this.sequenceProvider = sequenceProvider;
        this.stateMachineEngine = stateMachineEngine;
        if (configuredKey == null || configuredKey.isBlank()) {
            this.aesKeyHex = AesGcmUtil.deriveKey(DEFAULT_KEY_PASSWORD);
            log.warn("未配置 dayan.aes.key，回退使用默认派生密钥（仅供开发/测试）");
        } else {
            this.aesKeyHex = AesGcmUtil.deriveKey(configuredKey);
        }
    }

    // ====== 查询 ======

    @Override
    public PageResult<EquityDepotVO> page(EquityDepotQueryDTO query) {
        LambdaQueryWrapper<EquityDepot> wrapper = buildQueryWrapper(query);
        Page<EquityDepot> page = depotMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<EquityDepotVO> records = page.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<EquityDepotVO> list(EquityDepotQueryDTO query) {
        return depotMapper.selectList(buildQueryWrapper(query)).stream()
                .map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public EquityDepotVO getDetail(String equityCode) {
        return toVO(requireEquity(equityCode));
    }

    @Override
    public EquityDepot requireEquity(String equityCode) {
        EquityDepot depot = depotMapper.selectOne(new LambdaQueryWrapper<EquityDepot>()
                .eq(EquityDepot::getEquityCode, equityCode)
                .last("LIMIT 1"));
        if (depot == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "权益不存在: " + equityCode);
        }
        return depot;
    }

    // ====== 核心链路：批量入库（stockIn） ======

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int stockIn(StockInDTO dto) {
        Integer carrierType = dto.getCarrierType();
        if (carrierType == null || (carrierType != 1 && carrierType != 2)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "载体类型必须为 1(权益卡) 或 2(权益函)");
        }
        int quantity = dto.getQuantity();
        if (quantity <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "入库数量必须大于 0");
        }

        // 校验批次、模板（取冗余字段）
        EquityBatch batch = batchService.requireBatch(dto.getBatchCode());
        EquityTemplate template = templateService.requireTemplate(batch.getTemplateCode());

        LocalDateTime now = LocalDateTime.now();
        Integer shelfLifeDays = template.getShelfLifeDays();
        LocalDateTime shelfExpireTime = (shelfLifeDays != null && shelfLifeDays > 0)
                ? now.plusDays(shelfLifeDays) : null;

        for (int i = 0; i < quantity; i++) {
            EquityDepot entity = new EquityDepot();
            String equityCode = generateEquityCode();
            entity.setEquityCode(equityCode);
            entity.setEquityNo(equityCode); // equity_no = equity_code
            entity.setTemplateCode(template.getTemplateCode());
            entity.setBatchCode(batch.getBatchCode());
            entity.setEquityType(template.getEquityType());
            entity.setEquityValue(template.getEquityValue());
            entity.setCostPrice(template.getCostPrice());
            entity.setChannelCode(dto.getChannelCode() != null ? dto.getChannelCode() : batch.getChannelCode());
            entity.setProduceTime(now);
            entity.setShelfExpireTime(shelfExpireTime);
            entity.setUseCount(0);
            entity.setMaxUseCount(template.getMaxUseCount());
            entity.setCardSecret(encryptCardSecret());
            entity.setCarrierType(carrierType);
            if (carrierType == 1) {
                // 权益卡：激活码 DY-8 位，生成后查重
                entity.setActivateCode(generateUniqueRandomCode(DY_PREFIX, DY_LEN, true));
            } else {
                // 权益函：绑定码 BF-12 位，生成后查重
                entity.setBindCode(generateUniqueRandomCode(BF_PREFIX, BF_LEN, false));
            }
            entity.setEquityStatus(EquityEvent.STATUS_STOCK);

            depotMapper.insert(entity);
        }

        // 联动批次：produced_count += quantity, remain_count += quantity
        batchService.incrementStat(batch.getBatchCode(), "produced_count", quantity);
        batchService.incrementStat(batch.getBatchCode(), "remain_count", quantity);

        // 推进批次状态：首次入库 0→1；全部入库 1→2
        EquityBatch refreshed = batchService.requireBatch(batch.getBatchCode());
        if (refreshed.getBatchStatus() != null && refreshed.getBatchStatus() == BATCH_STATUS_PENDING) {
            batchService.updateBatchStatus(batch.getBatchCode(), BATCH_STATUS_PRODUCING);
        }
        int produced = (refreshed.getProducedCount() == null ? 0 : refreshed.getProducedCount()) + quantity;
        int total = refreshed.getTotalQuantity() == null ? 0 : refreshed.getTotalQuantity();
        if (total > 0 && produced >= total) {
            batchService.updateBatchStatus(batch.getBatchCode(), BATCH_STATUS_COMPLETED);
        }

        log.info("权益批量入库成功: batchCode={}, quantity={}, carrierType={}", batch.getBatchCode(), quantity, carrierType);
        return quantity;
    }

    // ====== 核心链路：出库（outbound） ======

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int outbound(OutboundDTO dto) {
        List<String> codes = dto.getEquityCodes();
        if (codes == null || codes.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "权益编码列表不能为空");
        }

        LocalDateTime now = LocalDateTime.now();
        int success = 0;
        for (String code : codes) {
            EquityDepot depot = requireEquity(code);
            int from = depot.getEquityStatus() == null ? EquityEvent.STATUS_STOCK : depot.getEquityStatus();
            // 校验库存中（状态机会兜底，但提前给更友好提示）
            if (from != EquityEvent.STATUS_STOCK) {
                throw new BusinessException(ErrorCode.BUSINESS,
                        "权益未在库存中，无法出库: " + code + "（当前状态=" + from + "）");
            }
            int to = stateMachineEngine.transition(EquityEvent.DOMAIN, from, EquityEvent.OUTBOUND);

            EquityDepot update = new EquityDepot();
            update.setId(depot.getId());
            update.setEquityStatus(to);
            update.setOutboundChannelCode(dto.getOutboundChannelCode());
            update.setOutboundAgentCode(dto.getOutboundAgentCode());
            update.setOutboundTime(now);
            update.setLogisticsNo(dto.getLogisticsNo());
            depotMapper.updateById(update);

            // 联动批次：outbound_count += 1, remain_count -= 1
            batchService.incrementStat(depot.getBatchCode(), "outbound_count", 1);
            batchService.incrementStat(depot.getBatchCode(), "remain_count", -1);
            success++;
        }
        log.info("权益出库成功: count={}, outboundChannelCode={}", success, dto.getOutboundChannelCode());
        return success;
    }

    // ====== 核心链路：激活（activate） ======

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String activate(ActivateDTO dto) {
        Integer carrierType = dto.getCarrierType();
        EquityDepot depot;
        if (carrierType != null && carrierType == 2) {
            // 权益函：按 bindCode 查
            if (dto.getBindCode() == null || dto.getBindCode().isEmpty()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "权益函激活须提供 bindCode");
            }
            depot = findByBindCode(dto.getBindCode());
        } else {
            // 默认/卡：按 activateCode 查
            if (dto.getActivateCode() == null || dto.getActivateCode().isEmpty()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "权益卡激活须提供 activateCode");
            }
            depot = findByActivateCode(dto.getActivateCode());
        }

        int from = depot.getEquityStatus() == null ? EquityEvent.STATUS_STOCK : depot.getEquityStatus();
        if (from != EquityEvent.STATUS_OUTBOUND) {
            throw new BusinessException(ErrorCode.BUSINESS, "权益未出库或已激活（当前状态=" + from + "）");
        }

        EquityTemplate template = templateService.requireTemplate(depot.getTemplateCode());
        LocalDateTime now = LocalDateTime.now();
        Integer validDays = template.getValidDays();
        LocalDateTime expireTime = (validDays != null && validDays > 0) ? now.plusDays(validDays) : null;

        // 状态机 1→2
        int to = stateMachineEngine.transition(EquityEvent.DOMAIN, from, EquityEvent.ACTIVATE);

        // 1. 更新 equity_depot
        EquityDepot update = new EquityDepot();
        update.setId(depot.getId());
        update.setEquityStatus(to);
        update.setActivateTime(now);
        update.setExpireTime(expireTime);
        update.setClientCode(dto.getClientCode());
        depotMapper.updateById(update);

        // 2. 插 equity_activate 记录
        String activateRecordCode = generateActivateRecordCode();
        EquityActivate activate = new EquityActivate();
        activate.setActivateCode(activateRecordCode);
        activate.setEquityCode(depot.getEquityCode());
        activate.setTemplateCode(depot.getTemplateCode());
        activate.setClientCode(dto.getClientCode());
        activate.setClientFullName(dto.getClientFullName());
        activate.setClientPhone(dto.getClientPhone());
        activate.setActivateChannel(dto.getActivateChannel());
        activate.setActivateSourceCode(dto.getActivateSourceCode());
        activate.setActivateTime(now);
        activate.setExpireTime(expireTime);
        activate.setIsIdCardVerified(0);
        activate.setIsAgreementSigned(0);
        activate.setIpAddress(dto.getIpAddress());
        activate.setDeviceInfo(dto.getDeviceInfo());
        activateMapper.insert(activate);

        // 3. 联动批次：activated_count += 1
        batchService.incrementStat(depot.getBatchCode(), "activated_count", 1);

        // 4. 自动建默认使用人（仅当当前无任何使用人时，避免重复激活场景重复创建）
        Long existCount = usePersonMapper.selectCount(new LambdaQueryWrapper<EquityUsePerson>()
                .eq(EquityUsePerson::getEquityCode, depot.getEquityCode()));
        if (existCount == null || existCount == 0) {
            EquityUsePerson person = new EquityUsePerson();
            person.setEquityCode(depot.getEquityCode());
            person.setClientCode(dto.getClientCode());
            person.setUsePersonName(dto.getClientFullName());
            person.setRelationWithHolder("本人");
            person.setIsDefaultHolder(1);
            usePersonMapper.insert(person);
        }

        log.info("权益激活成功: equityCode={}, clientCode={}, activateRecordCode={}",
                depot.getEquityCode(), dto.getClientCode(), activateRecordCode);
        return activateRecordCode;
    }

    // ====== 核心链路：作废（void） ======

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voidEquity(VoidDTO dto) {
        EquityDepot depot = requireEquity(dto.getEquityCode());
        int from = depot.getEquityStatus() == null ? EquityEvent.STATUS_STOCK : depot.getEquityStatus();
        // 状态机校验 0/1→6，2 已激活会自动抛异常
        int to = stateMachineEngine.transition(EquityEvent.DOMAIN, from, EquityEvent.VOID);

        EquityDepot update = new EquityDepot();
        update.setId(depot.getId());
        update.setEquityStatus(to);
        update.setVoidReason(dto.getVoidReason());
        depotMapper.updateById(update);

        // 联动批次：voided_count += 1；仅 from=0 时 remain_count -= 1（已出库的 remain 已在出库时扣减）
        batchService.incrementStat(depot.getBatchCode(), "voided_count", 1);
        if (from == EquityEvent.STATUS_STOCK) {
            batchService.incrementStat(depot.getBatchCode(), "remain_count", -1);
        }
        log.info("权益作废成功: equityCode={}, from={}, reason={}", depot.getEquityCode(), from, dto.getVoidReason());
    }

    // ====== 核心链路：更换权益人 ======

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long changeHolder(ChangeHolderDTO dto) {
        EquityDepot depot = requireEquity(dto.getEquityCode());
        int from = depot.getEquityStatus() == null ? EquityEvent.STATUS_STOCK : depot.getEquityStatus();
        if (from != EquityEvent.STATUS_ACTIVATED) {
            throw new BusinessException(ErrorCode.BUSINESS, "仅已激活权益可更换权益人（当前状态=" + from + "）");
        }

        // 校验同权益无在途 change 记录
        Long pending = changeHolderMapper.selectCount(new LambdaQueryWrapper<EquityChangeHolder>()
                .eq(EquityChangeHolder::getEquityCode, depot.getEquityCode())
                .eq(EquityChangeHolder::getChangeStatus, EquityEvent.CHANGE_STATUS_PENDING));
        if (pending != null && pending > 0) {
            throw new BusinessException(ErrorCode.BUSINESS, "该权益已有在途更换记录，请先完成或回滚: " + depot.getEquityCode());
        }

        // 取当前默认使用人作为原权益人（若 oldUsePersonCode 未提供）
        String oldUsePersonCode = dto.getOldUsePersonCode();
        String oldPersonName = null;
        String oldPersonIdCardEncrypted = null;
        EquityUsePerson oldDefault = findDefaultUsePerson(depot.getEquityCode());
        if (oldDefault != null) {
            if (oldUsePersonCode == null || oldUsePersonCode.isEmpty()) {
                oldUsePersonCode = oldDefault.getClientCode();
            }
            oldPersonName = oldDefault.getUsePersonName();
            oldPersonIdCardEncrypted = oldDefault.getUsePersonIdCard();
        }

        // 状态机 2→7
        int to = stateMachineEngine.transition(EquityEvent.DOMAIN, from, EquityEvent.CHANGE_HOLDER);
        EquityDepot updateDepot = new EquityDepot();
        updateDepot.setId(depot.getId());
        updateDepot.setEquityStatus(to);
        depotMapper.updateById(updateDepot);

        // 插 change_holder 记录
        EquityChangeHolder record = new EquityChangeHolder();
        record.setEquityCode(depot.getEquityCode());
        record.setOldUsePersonCode(oldUsePersonCode);
        record.setOldPersonName(oldPersonName);
        record.setOldPersonIdCard(oldPersonIdCardEncrypted);
        record.setNewUsePersonCode(null); // 完成时再设置
        record.setNewPersonName(dto.getNewPersonName());
        record.setNewPersonIdCard(encryptIdCard(dto.getNewPersonIdCard()));
        record.setChangeReason(dto.getChangeReason());
        record.setChangeStatus(EquityEvent.CHANGE_STATUS_PENDING);
        record.setOperateTime(LocalDateTime.now());
        record.setOperatorCode(dto.getOperatorCode());
        changeHolderMapper.insert(record);

        log.info("发起更换权益人: equityCode={}, changeId={}", depot.getEquityCode(), record.getId());
        return record.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeDone(ChangeDoneDTO dto) {
        EquityDepot depot = requireEquity(dto.getEquityCode());
        EquityChangeHolder record = changeHolderMapper.selectById(dto.getChangeId());
        if (record == null || !depot.getEquityCode().equals(record.getEquityCode())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "更换记录不存在: id=" + dto.getChangeId());
        }
        if (record.getChangeStatus() == null || record.getChangeStatus() != EquityEvent.CHANGE_STATUS_PENDING) {
            throw new BusinessException(ErrorCode.BUSINESS, "更换记录非待处理状态，无法完成");
        }

        int from = depot.getEquityStatus() == null ? EquityEvent.STATUS_ACTIVATED : depot.getEquityStatus();
        if (from != EquityEvent.STATUS_CHANGING_HOLDER) {
            throw new BusinessException(ErrorCode.BUSINESS, "权益非更换权益人中状态，无法完成更换（当前=" + from + "）");
        }
        // 状态机 7→2
        int to = stateMachineEngine.transition(EquityEvent.DOMAIN, from, EquityEvent.CHANGE_DONE);
        EquityDepot updateDepot = new EquityDepot();
        updateDepot.setId(depot.getId());
        updateDepot.setEquityStatus(to);
        depotMapper.updateById(updateDepot);

        // 切换默认使用人：旧的置 0，新的置 1；新使用人不存在则创建
        String newCode = dto.getNewUsePersonCode();
        EquityUsePerson newPerson = null;
        if (newCode != null && !newCode.isEmpty()) {
            newPerson = usePersonMapper.selectOne(new LambdaQueryWrapper<EquityUsePerson>()
                    .eq(EquityUsePerson::getEquityCode, depot.getEquityCode())
                    .eq(EquityUsePerson::getClientCode, newCode)
                    .last("LIMIT 1"));
        }
        if (newPerson == null) {
            // 创建新使用人
            newPerson = new EquityUsePerson();
            newPerson.setEquityCode(depot.getEquityCode());
            newPerson.setClientCode(newCode);
            newPerson.setUsePersonName(dto.getNewPersonName() != null ? dto.getNewPersonName() : record.getNewPersonName());
            newPerson.setUsePersonPhone(dto.getNewPersonPhone());
            newPerson.setUsePersonIdCard(encryptIdCard(dto.getNewPersonIdCard()));
            newPerson.setIsDefaultHolder(1);
            usePersonMapper.insert(newPerson);
        } else {
            // 已存在 → 置默认
            usePersonMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<EquityUsePerson>()
                    .eq(EquityUsePerson::getId, newPerson.getId())
                    .set(EquityUsePerson::getIsDefaultHolder, 1));
        }
        // 旧的默认置 0
        EquityUsePerson oldDefault = findDefaultUsePerson(depot.getEquityCode());
        if (oldDefault != null && !oldDefault.getId().equals(newPerson.getId())) {
            usePersonMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<EquityUsePerson>()
                    .eq(EquityUsePerson::getId, oldDefault.getId())
                    .set(EquityUsePerson::getIsDefaultHolder, 0));
        }

        // 更新 change 记录：change_status=1，写 newUsePersonCode/操作信息
        EquityChangeHolder updateRecord = new EquityChangeHolder();
        updateRecord.setId(record.getId());
        updateRecord.setChangeStatus(EquityEvent.CHANGE_STATUS_DONE);
        updateRecord.setNewUsePersonCode(newPerson.getClientCode());
        updateRecord.setOperateTime(LocalDateTime.now());
        updateRecord.setOperatorCode(dto.getOperatorCode());
        changeHolderMapper.updateById(updateRecord);

        log.info("完成更换权益人: equityCode={}, changeId={}, newClientCode={}",
                depot.getEquityCode(), dto.getChangeId(), newPerson.getClientCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeRollback(ChangeRollbackDTO dto) {
        EquityDepot depot = requireEquity(dto.getEquityCode());
        EquityChangeHolder record = changeHolderMapper.selectById(dto.getChangeId());
        if (record == null || !depot.getEquityCode().equals(record.getEquityCode())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "更换记录不存在: id=" + dto.getChangeId());
        }
        if (record.getChangeStatus() == null || record.getChangeStatus() != EquityEvent.CHANGE_STATUS_PENDING) {
            throw new BusinessException(ErrorCode.BUSINESS, "更换记录非待处理状态，无法回滚");
        }

        int from = depot.getEquityStatus() == null ? EquityEvent.STATUS_ACTIVATED : depot.getEquityStatus();
        if (from != EquityEvent.STATUS_CHANGING_HOLDER) {
            throw new BusinessException(ErrorCode.BUSINESS, "权益非更换权益人中状态，无法回滚（当前=" + from + "）");
        }
        // 状态机 7→2（复用 change_done 事件，语义上回滚=权益恢复原持有人）
        int to = stateMachineEngine.transition(EquityEvent.DOMAIN, from, EquityEvent.CHANGE_DONE);
        EquityDepot updateDepot = new EquityDepot();
        updateDepot.setId(depot.getId());
        updateDepot.setEquityStatus(to);
        depotMapper.updateById(updateDepot);

        EquityChangeHolder updateRecord = new EquityChangeHolder();
        updateRecord.setId(record.getId());
        updateRecord.setChangeStatus(EquityEvent.CHANGE_STATUS_ROLLBACK);
        updateRecord.setOperateTime(LocalDateTime.now());
        updateRecord.setOperatorCode(dto.getOperatorCode());
        changeHolderMapper.updateById(updateRecord);

        log.info("回滚更换权益人: equityCode={}, changeId={}", depot.getEquityCode(), dto.getChangeId());
    }

    // ====== 状态机通用流转 ======

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer transition(String equityCode, String event) {
        EquityDepot depot = requireEquity(equityCode);
        int from = depot.getEquityStatus() == null ? EquityEvent.STATUS_STOCK : depot.getEquityStatus();
        int to = stateMachineEngine.transition(EquityEvent.DOMAIN, from, event);

        // 完成事件联动 used_count
        if (EquityEvent.COMPLETE.equals(event)) {
            batchService.incrementStat(depot.getBatchCode(), "used_count", 1);
        }
        // 过期类事件联动 expired_count/remain_count
        if (EquityEvent.SHELF_EXPIRE.equals(event)) {
            batchService.incrementStat(depot.getBatchCode(), "expired_count", 1);
            batchService.incrementStat(depot.getBatchCode(), "remain_count", -1);
        }
        if (EquityEvent.EXPIRE.equals(event)) {
            batchService.incrementStat(depot.getBatchCode(), "expired_count", 1);
        }

        // 使用计数维护
        EquityDepot update = new EquityDepot();
        update.setId(depot.getId());
        update.setEquityStatus(to);
        if (EquityEvent.START_SERVICE.equals(event) && depot.getFirstUseTime() == null) {
            update.setFirstUseTime(LocalDateTime.now());
        }
        if (EquityEvent.START_SERVICE.equals(event)) {
            update.setLastUseTime(LocalDateTime.now());
            update.setUseCount((depot.getUseCount() == null ? 0 : depot.getUseCount()) + 1);
        }
        depotMapper.updateById(update);
        log.info("权益状态流转: equityCode={}, {} --{}--> {}", equityCode, from, event, to);
        return to;
    }

    // ====== 内部方法 ======

    private EquityDepot findByActivateCode(String activateCode) {
        EquityDepot depot = depotMapper.selectOne(new LambdaQueryWrapper<EquityDepot>()
                .eq(EquityDepot::getActivateCode, activateCode)
                .last("LIMIT 1"));
        if (depot == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "激活码无效: " + activateCode);
        }
        return depot;
    }

    private EquityDepot findByBindCode(String bindCode) {
        EquityDepot depot = depotMapper.selectOne(new LambdaQueryWrapper<EquityDepot>()
                .eq(EquityDepot::getBindCode, bindCode)
                .last("LIMIT 1"));
        if (depot == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "绑定码无效: " + bindCode);
        }
        return depot;
    }

    private EquityUsePerson findDefaultUsePerson(String equityCode) {
        return usePersonMapper.selectOne(new LambdaQueryWrapper<EquityUsePerson>()
                .eq(EquityUsePerson::getEquityCode, equityCode)
                .eq(EquityUsePerson::getIsDefaultHolder, 1)
                .last("LIMIT 1"));
    }

    private String generateEquityCode() {
        long seq = sequenceProvider.next(EQ_SEQ_KEY);
        return EQ_PREFIX + String.format("%0" + EQ_SEQ_WIDTH + "d", seq);
    }

    private String generateActivateRecordCode() {
        long seq = sequenceProvider.next(AC_SEQ_KEY);
        return AC_PREFIX + String.format("%0" + AC_SEQ_WIDTH + "d", seq);
    }

    /**
     * 生成前缀 + 随机数字位的不连续码，生成后查 equity_depot 查重，冲突最多重试 {@link #RANDOM_RETRY_MAX} 次。
     *
     * @param prefix         前缀（DY/BF）
     * @param digitLen       数字位数
     * @param checkActivate  true 查 activate_code 列，false 查 bind_code 列
     */
    private String generateUniqueRandomCode(String prefix, int digitLen, boolean checkActivate) {
        for (int i = 0; i < RANDOM_RETRY_MAX; i++) {
            String code = prefix + randomDigits(digitLen);
            Long count = depotMapper.selectCount(new LambdaQueryWrapper<EquityDepot>()
                    .eq(checkActivate ? EquityDepot::getActivateCode : EquityDepot::getBindCode, code));
            if (count == null || count == 0) {
                return code;
            }
            log.warn("随机码冲突，重试 {}/{}: {}", i + 1, RANDOM_RETRY_MAX, code);
        }
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "随机码生成失败：多次冲突");
    }

    private static String randomDigits(int length) {
        return ThreadLocalRandom.current().ints(length, 0, 10)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining());
    }

    /** 卡密：随机 UUID 串 AES-GCM 加密后存储 */
    private String encryptCardSecret() {
        String plain = UUID.randomUUID().toString().replace("-", "");
        return AesGcmUtil.encrypt(plain, aesKeyHex);
    }

    /** 身份证明文 → AES-GCM 加密值 */
    private String encryptIdCard(String plain) {
        if (plain == null || plain.isEmpty()) {
            return null;
        }
        return AesGcmUtil.encrypt(plain, aesKeyHex);
    }

    private LambdaQueryWrapper<EquityDepot> buildQueryWrapper(EquityDepotQueryDTO query) {
        LambdaQueryWrapper<EquityDepot> wrapper = new LambdaQueryWrapper<EquityDepot>()
                .orderByDesc(EquityDepot::getCreatedAt);
        if (query.getEquityCode() != null && !query.getEquityCode().isEmpty()) {
            wrapper.eq(EquityDepot::getEquityCode, query.getEquityCode());
        }
        if (query.getEquityNo() != null && !query.getEquityNo().isEmpty()) {
            wrapper.eq(EquityDepot::getEquityNo, query.getEquityNo());
        }
        if (query.getTemplateCode() != null && !query.getTemplateCode().isEmpty()) {
            wrapper.eq(EquityDepot::getTemplateCode, query.getTemplateCode());
        }
        if (query.getBatchCode() != null && !query.getBatchCode().isEmpty()) {
            wrapper.eq(EquityDepot::getBatchCode, query.getBatchCode());
        }
        if (query.getChannelCode() != null && !query.getChannelCode().isEmpty()) {
            wrapper.eq(EquityDepot::getChannelCode, query.getChannelCode());
        }
        if (query.getAgentCode() != null && !query.getAgentCode().isEmpty()) {
            wrapper.eq(EquityDepot::getAgentCode, query.getAgentCode());
        }
        if (query.getClientCode() != null && !query.getClientCode().isEmpty()) {
            wrapper.eq(EquityDepot::getClientCode, query.getClientCode());
        }
        if (query.getCarrierType() != null) {
            wrapper.eq(EquityDepot::getCarrierType, query.getCarrierType());
        }
        if (query.getEquityStatus() != null) {
            wrapper.eq(EquityDepot::getEquityStatus, query.getEquityStatus());
        }
        if (query.getActivateCode() != null && !query.getActivateCode().isEmpty()) {
            wrapper.eq(EquityDepot::getActivateCode, query.getActivateCode());
        }
        if (query.getBindCode() != null && !query.getBindCode().isEmpty()) {
            wrapper.eq(EquityDepot::getBindCode, query.getBindCode());
        }
        return wrapper;
    }

    private EquityDepotVO toVO(EquityDepot entity) {
        EquityDepotVO vo = new EquityDepotVO();
        vo.setId(entity.getId());
        vo.setEquityCode(entity.getEquityCode());
        vo.setEquityNo(entity.getEquityNo());
        vo.setTemplateCode(entity.getTemplateCode());
        vo.setBatchCode(entity.getBatchCode());
        vo.setEquityType(entity.getEquityType());
        vo.setEquityValue(entity.getEquityValue());
        vo.setCostPrice(entity.getCostPrice());
        vo.setChannelCode(entity.getChannelCode());
        vo.setAgentCode(entity.getAgentCode());
        vo.setClientCode(entity.getClientCode());
        vo.setProduceTime(entity.getProduceTime());
        vo.setAllocateTime(entity.getAllocateTime());
        vo.setOutboundChannelCode(entity.getOutboundChannelCode());
        vo.setOutboundAgentCode(entity.getOutboundAgentCode());
        vo.setOutboundTime(entity.getOutboundTime());
        vo.setLogisticsNo(entity.getLogisticsNo());
        vo.setActivateTime(entity.getActivateTime());
        vo.setFirstUseTime(entity.getFirstUseTime());
        vo.setLastUseTime(entity.getLastUseTime());
        vo.setUseCount(entity.getUseCount());
        vo.setMaxUseCount(entity.getMaxUseCount());
        vo.setExpireTime(entity.getExpireTime());
        vo.setShelfExpireTime(entity.getShelfExpireTime());
        // 卡密脱敏
        vo.setCardSecret(entity.getCardSecret() != null ? "***" : null);
        vo.setCarrierType(entity.getCarrierType());
        vo.setActivateCode(entity.getActivateCode());
        vo.setBindCode(entity.getBindCode());
        vo.setQrCodeUrl(entity.getQrCodeUrl());
        vo.setOrderCode(entity.getOrderCode());
        vo.setEquityStatus(entity.getEquityStatus());
        vo.setVoidReason(entity.getVoidReason());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
