package com.moxi.common.response;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // 认证错误 1001-1999
    TOKEN_INVALID(1001, "Token无效"),
    TOKEN_EXPIRED(1002, "Token已过期"),
    PERMISSION_DENIED(1003, "权限不足"),
    UNAUTHORIZED(1004, "未登录"),

    // 用户错误 2001-2999
    PHONE_ALREADY_REGISTERED(2001, "手机号已注册"),
    EMAIL_ALREADY_REGISTERED(2002, "邮箱已注册"),
    VERIFY_CODE_ERROR(2003, "验证码错误"),
    VERIFY_CODE_EXPIRED(2004, "验证码已过期"),
    VERIFY_CODE_TOO_FREQUENT(2005, "验证码发送过于频繁"),
    VERIFY_CODE_COOLDOWN(2005, "验证码发送过于频繁，请60秒后重试"),
    USER_NOT_FOUND(2006, "用户不存在"),
    PASSWORD_ERROR(2007, "密码错误"),
    USER_DISABLED(2008, "账号已被封禁"),
    ACCOUNT_DISABLED(2008, "账号已被封禁"),
    IP_REQUEST_LIMIT_EXCEEDED(2009, "今日请求次数已达上限"),

    // 内容错误 3001-3999
    QUOTA_INSUFFICIENT(3001, "今日额度不足"),
    SENSITIVE_WORD_DETECTED(3002, "内容包含敏感词"),
    GENERATION_FAILED(3003, "AI生成失败"),
    CONTENT_NOT_FOUND(3004, "笔记不存在"),
    GENERATION_TIMEOUT(3005, "AI生成超时"),
    PROMPT_NOT_FOUND(3006, "Prompt模板不存在"),

    // 计费错误 4001-4999
    ORDER_ALREADY_EXISTS(4001, "订单已存在"),
    PAYMENT_NOT_COMPLETED(4002, "支付未完成"),
    PLAN_NOT_FOUND(4003, "套餐不存在"),
    ORDER_NOT_FOUND(4004, "订单不存在"),
    ORDER_STATUS_INVALID(4005, "订单状态不允许此操作"),

    // 系统错误 5001-5999
    AI_SERVICE_UNAVAILABLE(5001, "AI服务暂不可用"),
    COLLECTOR_SERVICE_ERROR(5002, "采集服务异常"),
    INTERNAL_ERROR(5003, "系统内部错误"),
    RATE_LIMIT_EXCEEDED(5004, "请求过于频繁，请稍后再试");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
