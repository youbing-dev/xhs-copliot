package com.moxi.common.constant;

public interface RedisKeys {

    String TOKEN_BLACKLIST = "auth:blacklist:";
    String VERIFY_CODE = "verify:code:";
    String VERIFY_COOLDOWN = "verify:cooldown:";
    String VERIFY_IP_COUNT = "verify:ip:";
    String QUOTA_USER = "quota:user:";
    String RATE_LIMIT = "rate:limit:";
    String AI_TOKEN_METER = "ai:tokens:";
    String PAY_CALLBACK_DEDUP = "pay:callback:";

    static String quotaUser(Long userId, String date) {
        return QUOTA_USER + userId + ":" + date;
    }

    static String aiTokenMeter(Long userId, String date) {
        return AI_TOKEN_METER + userId + ":" + date;
    }

    static String payCallbackDedup(String transactionId) {
        return PAY_CALLBACK_DEDUP + transactionId;
    }
}
