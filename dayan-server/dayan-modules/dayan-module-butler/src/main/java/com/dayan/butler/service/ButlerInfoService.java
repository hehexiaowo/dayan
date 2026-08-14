package com.dayan.butler.service;

import com.dayan.butler.dto.ButlerAccountOpenDTO;
import com.dayan.butler.dto.ButlerInfoCreateDTO;
import com.dayan.butler.dto.ButlerInfoQueryDTO;
import com.dayan.butler.dto.ButlerInfoUpdateDTO;
import com.dayan.butler.vo.ButlerInfoVO;
import com.dayan.common.core.resp.PageResult;

import java.util.List;

/**
 * 管家信息服务。
 *
 * <p>{@code butlerCode} 由系统生成（BT + 5 位序列），全表唯一。
 *
 * <p>管家保留独立账号体系（butler_account，面向未来管家端）；
 * 同时可开通 organ_account 后台账号（{@link #openAccount}）直接登录 admin。
 */
public interface ButlerInfoService {

    PageResult<ButlerInfoVO> page(ButlerInfoQueryDTO query);

    List<ButlerInfoVO> list(ButlerInfoQueryDTO query);

    ButlerInfoVO getDetail(String butlerCode);

    String create(ButlerInfoCreateDTO dto);

    void update(String butlerCode, ButlerInfoUpdateDTO dto);

    /**
     * 为管家开通 organ 后台账号（organ_account + organ_employee + ROLE_BUTLER）。
     *
     * @return 生成的 organ_account.account_code
     */
    String openAccount(String butlerCode, ButlerAccountOpenDTO dto);

    void delete(String butlerCode);
}
