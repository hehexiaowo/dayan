package com.dayan.client.service;

import com.dayan.client.dto.ClientFamilyMemberCreateDTO;
import com.dayan.client.dto.ClientFamilyMemberUpdateDTO;
import com.dayan.client.vo.ClientFamilyMemberVO;

import java.util.List;

/**
 * 客户家庭成员服务。
 */
public interface ClientFamilyMemberService {

    /**
     * 按客户编码列出家庭成员。
     */
    List<ClientFamilyMemberVO> listByClient(String clientCode);

    /**
     * 新增家庭成员（同客户同关系同姓名唯一校验）。
     *
     * @return 主键 ID
     */
    Long create(ClientFamilyMemberCreateDTO dto);

    /**
     * 修改家庭成员。
     */
    void update(Long id, ClientFamilyMemberUpdateDTO dto);

    /**
     * 删除家庭成员。
     */
    void delete(Long id);
}
