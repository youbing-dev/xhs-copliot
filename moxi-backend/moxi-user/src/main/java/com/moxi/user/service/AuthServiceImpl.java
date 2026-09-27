package com.moxi.user.service;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moxi.common.exception.BusinessException;
import com.moxi.common.response.ErrorCode;
import com.moxi.common.util.JwtUtils;
import com.moxi.user.domain.User;
import com.moxi.user.domain.UserProfile;
import com.moxi.user.domain.UserSettings;
import com.moxi.user.dto.LoginRequest;
import com.moxi.user.dto.LoginResponse;
import com.moxi.user.dto.RegisterRequest;
import com.moxi.user.dto.UserInfo;
import com.moxi.user.repository.UserMapper;
import com.moxi.user.repository.UserProfileMapper;
import com.moxi.user.repository.UserSettingsMapper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String VERIFY_CODE_KEY = "verify:code:";
    private static final String VERIFY_COOLDOWN_KEY = "verify:cooldown:";
    private static final String VERIFY_IP_KEY = "verify:ip:";
    private static final String AUTH_BLACKLIST_KEY = "auth:blacklist:";

    /** 验证码有效期：5分钟 */
    private static final long CODE_TTL_MINUTES = 5;

    /** 发送冷却时间：60秒 */
    private static final long COOLDOWN_SECONDS = 60;

    /** IP 限制统计有效期：24小时 */
    private static final long IP_TTL_HOURS = 24;

    /** 单个IP每天最大发送次数 */
    private static final int IP_MAX_COUNT = 10;

    /** 默认访问令牌有效期（秒），用于兜底 */
    private static final long DEFAULT_EXPIRES_IN = 7200;

    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserSettingsMapper userSettingsMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void sendCode(String type, String target, String ip) {
        // 1. 检查发送冷却
        String cooldownKey = VERIFY_COOLDOWN_KEY + target;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(cooldownKey))) {
            throw new BusinessException(ErrorCode.VERIFY_CODE_COOLDOWN);
        }

        // 2. 检查IP发送次数
        String ipKey = VERIFY_IP_KEY + ip;
        String ipCountStr = stringRedisTemplate.opsForValue().get(ipKey);
        int ipCount = ipCountStr != null ? Integer.parseInt(ipCountStr) : 0;
        if (ipCount >= IP_MAX_COUNT) {
            throw new BusinessException(ErrorCode.IP_REQUEST_LIMIT_EXCEEDED);
        }

        // 3. 生成6位随机验证码
        String code = RandomUtil.randomNumbers(6);

        // 4. 存入Redis（5分钟有效期）
        String codeKey = VERIFY_CODE_KEY + type + ":" + target;
        stringRedisTemplate.opsForValue().set(codeKey, code, CODE_TTL_MINUTES, TimeUnit.MINUTES);

        // 5. 设置冷却（60秒）
        stringRedisTemplate.opsForValue().set(cooldownKey, "1", COOLDOWN_SECONDS, TimeUnit.SECONDS);

        // 6. 递增IP计数（首次设置过期时间）
        Long newCount = stringRedisTemplate.opsForValue().increment(ipKey);
        if (newCount != null && newCount == 1) {
            stringRedisTemplate.expire(ipKey, IP_TTL_HOURS, TimeUnit.HOURS);
        }

        // 7. 模拟发送短信/邮件
        log.info("【验证码】type={}, target={}, code={}", type, target, code);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse register(RegisterRequest req) {
        // 1. 验证验证码
        String contact = "phone".equals(req.getType()) ? req.getPhone() : req.getEmail();
        String codeKey = VERIFY_CODE_KEY + req.getType() + ":" + contact;
        String storedCode = stringRedisTemplate.opsForValue().get(codeKey);
        if (storedCode == null || !storedCode.equals(req.getCode())) {
            throw new BusinessException(ErrorCode.VERIFY_CODE_ERROR);
        }

        // 2. 检查手机号/邮箱是否已注册
        if ("phone".equals(req.getType())) {
            Long count = userMapper.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getPhone, req.getPhone())
            );
            if (count != null && count > 0) {
                throw new BusinessException(ErrorCode.PHONE_ALREADY_REGISTERED);
            }
        } else {
            Long count = userMapper.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getEmail, req.getEmail())
            );
            if (count != null && count > 0) {
                throw new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED);
            }
        }

        // 3. 创建用户
        User user = new User();
        if ("phone".equals(req.getType())) {
            user.setPhone(req.getPhone());
        } else {
            user.setEmail(req.getEmail());
        }
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setStatus(1);
        userMapper.insert(user);

        // 4. 创建用户资料
        UserProfile profile = new UserProfile();
        profile.setUserId(user.getId());
        profile.setNickname(StrUtil.isNotBlank(req.getNickname()) ? req.getNickname() : "用户" + user.getId());
        userProfileMapper.insert(profile);

        // 5. 创建用户设置（默认全部开启）
        UserSettings settings = new UserSettings();
        settings.setUserId(user.getId());
        settings.setNotifyEmail(true);
        settings.setNotifySms(true);
        settings.setNotifyPromotions(true);
        userSettingsMapper.insert(settings);

        // 6. 删除已使用的验证码
        stringRedisTemplate.delete(codeKey);

        // 7. 生成JWT令牌
        String accessToken = jwtUtils.generateAccessToken(user.getId());
        String refreshToken = jwtUtils.generateRefreshToken(user.getId());

        // 8. 构建响应
        UserInfo userInfo = new UserInfo(user.getId(), profile.getNickname(), profile.getAvatarUrl(), "free");
        long expiresIn = calculateExpiresIn(accessToken);

        return new LoginResponse(accessToken, refreshToken, expiresIn, userInfo);
    }

    @Override
    public LoginResponse login(LoginRequest req) {
        // 1. 按手机号或邮箱查找用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getPhone, req.getAccount())
                        .or()
                        .eq(User::getEmail, req.getAccount())
        );
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 2. 验证密码
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        // 3. 检查账号状态
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        // 4. 获取用户资料
        UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, user.getId())
        );

        // 5. 生成JWT令牌
        String accessToken = jwtUtils.generateAccessToken(user.getId());
        String refreshToken = jwtUtils.generateRefreshToken(user.getId());

        // 6. 构建响应
        String nickname = profile != null ? profile.getNickname() : "用户" + user.getId();
        String avatar = profile != null ? profile.getAvatarUrl() : null;
        UserInfo userInfo = new UserInfo(user.getId(), nickname, avatar, "free");
        long expiresIn = calculateExpiresIn(accessToken);

        return new LoginResponse(accessToken, refreshToken, expiresIn, userInfo);
    }

    @Override
    public LoginResponse refresh(String refreshToken) {
        // 1. 检查令牌是否过期
        if (jwtUtils.isTokenExpired(refreshToken)) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        }

        // 2. 解析令牌并验证类型
        Claims claims = jwtUtils.parseToken(refreshToken);
        String tokenType = claims.get("type", String.class);
        if (!"refresh".equals(tokenType)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        // 3. 提取用户ID
        Long userId = jwtUtils.extractUserId(refreshToken);

        // 4. 查询用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 5. 检查账号状态
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        // 6. 获取用户资料
        UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId)
        );

        // 7. 生成新的访问令牌
        String newAccessToken = jwtUtils.generateAccessToken(userId);

        // 8. 构建响应
        String nickname = profile != null ? profile.getNickname() : "用户" + userId;
        String avatar = profile != null ? profile.getAvatarUrl() : null;
        UserInfo userInfo = new UserInfo(userId, nickname, avatar, "free");
        long expiresIn = calculateExpiresIn(newAccessToken);

        return new LoginResponse(newAccessToken, refreshToken, expiresIn, userInfo);
    }

    @Override
    public void logout(String accessToken, Long userId) {
        if (accessToken == null) {
            return;
        }
        try {
            // 如果令牌已过期则无需加入黑名单
            if (jwtUtils.isTokenExpired(accessToken)) {
                return;
            }
            // 计算剩余有效期并加入黑名单
            Claims claims = jwtUtils.parseToken(accessToken);
            Date expiration = claims.getExpiration();
            long remainingMillis = expiration.getTime() - System.currentTimeMillis();
            if (remainingMillis > 0) {
                String blacklistKey = AUTH_BLACKLIST_KEY + accessToken;
                stringRedisTemplate.opsForValue().set(
                        blacklistKey,
                        String.valueOf(userId),
                        remainingMillis,
                        TimeUnit.MILLISECONDS
                );
            }
        } catch (Exception e) {
            log.warn("登出时将令牌加入黑名单失败: {}", e.getMessage());
        }
    }

    /**
     * 根据访问令牌计算剩余有效期（秒）
     */
    private long calculateExpiresIn(String accessToken) {
        try {
            Claims claims = jwtUtils.parseToken(accessToken);
            Date expiration = claims.getExpiration();
            return (expiration.getTime() - System.currentTimeMillis()) / 1000;
        } catch (Exception e) {
            log.warn("计算令牌有效期失败，使用默认值: {}", e.getMessage());
            return DEFAULT_EXPIRES_IN;
        }
    }
}
