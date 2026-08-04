package com.dayan.butler.service;

import com.dayan.butler.dto.ButlerAccountRoleRelCreateDTO;
import com.dayan.butler.dto.ButlerAccountRoleRelQueryDTO;
import com.dayan.butler.dto.ButlerAccountRoleRelUpdateDTO;
import com.dayan.butler.vo.ButlerAccountRoleRelVO;
import com.dayan.common.core.resp.PageResult;

import java.util.List;

/**
 * 管家账号-角色关联服务。
 *
 * <p>P5 仅 CRUD 框架，RBAC 查询后置。
 */
public interface ButlerAccountRoleRelService {

    PageResult<ButlerAccountRoleRelVO> page(ButlerAccountRoleRelQueryDTO query);

    List<ButlerAccountRoleRelVO> list(ButlerAccountRoleRelQueryDTO query);

    ButlerAccountRoleRelVO getDetail(Long id);

    Long create(ButlerAccountRoleRelCreateDTO dto);

    void update(Long id, ButlerAccountRoleRelUpdateDTO dto);

    void delete(Long id);
}
