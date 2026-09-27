package com.moxi.ai.meter;

import com.moxi.common.constant.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * AI Token 用量计量器。
 *
 * <p>基于 Redis Hash 统计用户每日 token 消耗与请求次数，
 * key 形如 {@code ai:tokens:{userId}:{yyyy-MM-dd}}，过期时间为 48 小时。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenMeter {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String FIELD_TOTAL_TOKENS = "total_tokens";
    private static final String FIELD_REQUEST_COUNT = "request_count";

    /** 新 key 的过期时间（小时） */
    private static final long TTL_HOURS = 48;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 记录一次生成的 token 用量。
     *
     * <p>对 total_tokens 累加 token 数，对 request_count 自增 1；
     * 首次写入（request_count 自增后为 1）时设置 48 小时过期。</p>
     *
     * @param userId 用户 ID
     * @param tokens 本次 token 消耗
     */
    public void recordUsage(Long userId, int tokens) {
        String date = LocalDate.now().format(DATE_FMT);
        String key = RedisKeys.aiTokenMeter(userId, date);

        stringRedisTemplate.opsForHash().increment(key, FIELD_TOTAL_TOKENS, tokens);
        Long count = stringRedisTemplate.opsForHash().increment(key, FIELD_REQUEST_COUNT, 1);
        if (count != null && count == 1) {
            stringRedisTemplate.expire(key, TTL_HOURS, TimeUnit.HOURS);
        }

        log.info("Recorded token usage, userId={}, tokens={}, date={}, key={}", userId, tokens, date, key);
    }

    /**
     * 获取用户今日 token 用量，未记录则返回 0。
     *
     * @param userId 用户 ID
     * @return 今日 token 总量
     */
    public int getTodayTokenUsage(Long userId) {
        String key = RedisKeys.aiTokenMeter(userId, LocalDate.now().format(DATE_FMT));
        Object value = stringRedisTemplate.opsForHash().get(key, FIELD_TOTAL_TOKENS);
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Parse today token usage failed, userId={}, value={}", userId, value);
            return 0;
        }
    }

    /**
     * 获取用户今日请求次数，未记录则返回 0。
     *
     * @param userId 用户 ID
     * @return 今日请求次数
     */
    public int getTodayRequestCount(Long userId) {
        String key = RedisKeys.aiTokenMeter(userId, LocalDate.now().format(DATE_FMT));
        Object value = stringRedisTemplate.opsForHash().get(key, FIELD_REQUEST_COUNT);
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Parse today request count failed, userId={}, value={}", userId, value);
            return 0;
        }
    }
}
