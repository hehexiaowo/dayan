package com.dayan.equity.controller.client;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.R;
import com.dayan.common.security.StpKit;
import com.dayan.equity.dto.EquityUsePersonCreateDTO;
import com.dayan.equity.dto.EquityUsePersonUpdateDTO;
import com.dayan.equity.dto.SetDefaultHolderDTO;
import com.dayan.equity.entity.EquityDepot;
import com.dayan.equity.entity.EquityUsePerson;
import com.dayan.equity.mapper.EquityDepotMapper;
import com.dayan.equity.mapper.EquityUsePersonMapper;
import com.dayan.equity.service.EquityUsePersonService;
import com.dayan.equity.vo.EquityUsePersonVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 权益使用人 - client 端接口（持卡人管理自己权益下的权益人）。
 * 路径：/client-api/equity/use-persons/*。
 * 所有写操作先校验 equityCode 归属当前 client。
 */
@Tag(name = "Client 权益使用人")
@RestController
@RequestMapping("/equity/use-persons")
@RequiredArgsConstructor
public class ClientEquityUsePersonController {

    private final EquityUsePersonService equityUsePersonService;
    private final EquityDepotMapper equityDepotMapper;
    private final EquityUsePersonMapper equityUsePersonMapper;

    private String currentClientCode() {
        Object code = StpKit.CLIENT.getSession().get("clientCode");
        if (code == null) throw new IllegalStateException("未获取到登录身份");
        return code.toString();
    }

    /** 校验 equityCode 归属当前 client */
    private void checkOwnership(String equityCode) {
        EquityDepot depot = equityDepotMapper.selectOne(new LambdaQueryWrapper<EquityDepot>()
                .eq(EquityDepot::getEquityCode, equityCode).last("LIMIT 1"));
        if (depot == null || !currentClientCode().equals(depot.getClientCode())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "权益不存在或无权访问");
        }
    }

    /** 校验使用人 id 归属当前 client（通过其 equityCode 反查） */
    private EquityUsePerson checkPersonOwnership(Long id) {
        EquityUsePerson p = equityUsePersonMapper.selectById(id);
        if (p == null) throw new BusinessException(ErrorCode.NOT_FOUND, "使用人不存在");
        checkOwnership(p.getEquityCode());
        return p;
    }

    @Operation(summary = "新增使用人")
    @PostMapping
    public R<Long> create(@RequestBody @Valid EquityUsePersonCreateDTO dto) {
        checkOwnership(dto.getEquityCode());
        dto.setClientCode(currentClientCode()); // 强制登录态
        return R.ok(equityUsePersonService.create(dto));
    }

    @Operation(summary = "修改使用人")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody EquityUsePersonUpdateDTO dto) {
        checkPersonOwnership(id);
        equityUsePersonService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除使用人")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        checkPersonOwnership(id);
        equityUsePersonService.delete(id);
        return R.ok();
    }

    @Operation(summary = "设置默认权益人")
    @PutMapping("/{id}/default")
    public R<Void> setDefault(@PathVariable Long id) {
        EquityUsePerson p = checkPersonOwnership(id);
        SetDefaultHolderDTO dto = new SetDefaultHolderDTO();
        dto.setId(id);
        dto.setEquityCode(p.getEquityCode());
        equityUsePersonService.setDefault(dto);
        return R.ok();
    }

    /**
     * 常用权益人（跨权益聚合去重）：供新增/激活时复用预填。
     * 按身份证号（加密列无法跨行比，改用 姓名+电话 组合去重）聚合。
     */
    @Operation(summary = "常用权益人（跨权益去重，复用预填）")
    @GetMapping("/suggest")
    public R<List<EquityUsePersonVO>> suggest() {
        String clientCode = currentClientCode();
        // 找到当前 client 名下所有权益
        List<EquityDepot> depots = equityDepotMapper.selectList(new LambdaQueryWrapper<EquityDepot>()
                .eq(EquityDepot::getClientCode, clientCode)
                .select(EquityDepot::getEquityCode));
        if (depots.isEmpty()) return R.ok(List.of());
        List<String> equityCodes = depots.stream().map(EquityDepot::getEquityCode).collect(Collectors.toList());
        // 查这些权益下全部使用人
        List<EquityUsePerson> persons = equityUsePersonMapper.selectList(new LambdaQueryWrapper<EquityUsePerson>()
                .in(EquityUsePerson::getEquityCode, equityCodes));
        // 按 姓名+电话 去重（占位"待填写-N"过滤掉）
        Map<String, EquityUsePerson> dedup = new LinkedHashMap<>();
        for (EquityUsePerson p : persons) {
            if (p.getUsePersonName() == null || p.getUsePersonName().startsWith("待填写")) continue;
            String key = p.getUsePersonName() + "|" + (p.getUsePersonPhone() == null ? "" : p.getUsePersonPhone());
            dedup.putIfAbsent(key, p);
        }
        // 转 VO（复用 getDetail 解密身份证）
        List<EquityUsePersonVO> result = new ArrayList<>();
        for (EquityUsePerson p : dedup.values()) {
            EquityUsePersonVO vo = equityUsePersonService.getDetail(p.getId());
            if (vo != null) result.add(vo);
        }
        return R.ok(result);
    }
}
