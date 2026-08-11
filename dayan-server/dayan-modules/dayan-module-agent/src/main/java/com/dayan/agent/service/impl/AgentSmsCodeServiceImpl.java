package com.dayan.agent.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.agent.entity.AgentAccount;
import com.dayan.agent.mapper.AgentAccountMapper;
import com.dayan.agent.service.AgentSmsCodeService;
import com.dayan.agent.vo.SmsSendVO;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.redis.RedisKey;
import com.dayan.common.sms.SmsService;
import com.dayan.common.sms.dto.SmsResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

/**
 * Agent 端短信验证码服务实现。
 *
 * <p>验证码 6 位数字，存入 Redis（key=dayan:sms:code:agent:{mobile}，TTL 5min）。
 * <p>60 秒重发冷却（key=dayan:sms:cooldown:agent:{mobile}，TTL 60s）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentSmsCodeServiceImpl implements AgentSmsCodeService {

    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration COOLDOWN_TTL = Duration.ofSeconds(60);
    private static final String SMS_TEMPLATE_LOGIN = "LOGIN_CODE";

    private final StringRedisTemplate redisTemplate;
    private final SmsService smsService;
    private final AgentAccountMapper accountMapper;

    @Override
    public SmsSendVO sendCode(String mobile, String channelCode) {
        // 1. 冷却检查
        String cooldownKey = RedisKey.smsCooldown(SCENE, mobile);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            long remain = redisTemplate.getExpire(cooldownKey);
            throw new BusinessException(ErrorCode.BUSINESS,
                    "发送太频繁，请" + Math.max(remain, 1) + "秒后重试");
        }

        // 2. 校验账号存在（防止对无关手机号发码）
        Long count = accountMapper.selectCount(new LambdaQueryWrapper<AgentAccount>()
                .eq(AgentAccount::getChannelCode, channelCode)
                .eq(AgentAccount::getPhone, mobile));
        if (count == null || count == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "该手机号未关联此渠道的代理人账号");
        }

        // 3. 生成验证码
        String code = RandomUtil.randomNumbers(6);

        // 4. 存入 Redis
        String codeKey = RedisKey.smsCode(SCENE, mobile);
        redisTemplate.opsForValue().set(codeKey, code, CODE_TTL);
        redisTemplate.opsForValue().set(cooldownKey, "1", COOLDOWN_TTL);

        // 5. 发送短信（Mock 实现会将 code 填入 devCode）
        SmsResult result = smsService.send(mobile, SMS_TEMPLATE_LOGIN, Map.of("code", code));
        if (!result.isSuccess()) {
            log.warn("短信发送失败: mobile={}, error={}", mobile, result.getError());
            throw new BusinessException(ErrorCode.BUSINESS, "短信发送失败，请稍后重试");
        }

        log.info("Agent 验证码已发送: mobile={}", mobile);
        return SmsSendVO.builder()
                .sent(true)
                .devCode(result.getDevCode())
                .build();
    }

    @Override
    public boolean verifyAndConsume(String mobile, String code) {
        String codeKey = RedisKey.smsCode(SCENE, mobile);
        String stored = redisTemplate.opsForValue().get(codeKey);
        if (stored == null) {
            return false;
        }
        if (!stored.equals(code)) {
            return false;
        }
        // 校验通过，删除验证码（一次性使用）
        redisTemplate.delete(codeKey);
        return true;
    }
}
