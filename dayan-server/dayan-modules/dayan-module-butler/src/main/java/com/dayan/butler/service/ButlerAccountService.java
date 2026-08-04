package com.dayan.butler.service;

import com.dayan.butler.dto.ButlerAccountCreateDTO;
import com.dayan.butler.dto.ButlerAccountQueryDTO;
import com.dayan.butler.dto.ButlerAccountUpdateDTO;
import com.dayan.butler.vo.ButlerAccountVO;
import com.dayan.common.core.resp.PageResult;

import java.util.List;

/**
 * 管家账号服务。
 *
 * <p>密码使用 BCrypt（复用 {@code common-security.PasswordService}）。
 * 一个管家可拥有多个账号（多账号），username 同 butlerCode 下唯一。
 */
public interface ButlerAccountService {

    PageResult<ButlerAccountVO> page(ButlerAccountQueryDTO query);

    List<ButlerAccountVO> list(ButlerAccountQueryDTO query);

    ButlerAccountVO getDetail(Long id);

    Long create(ButlerAccountCreateDTO dto);

    void update(Long id, ButlerAccountUpdateDTO dto);

    void resetPassword(Long id);

    void delete(Long id);
}
