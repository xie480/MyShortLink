package org.yilena.myShortLink.admin.common.convention.errorCode.codes;

import org.yilena.myShortLink.admin.common.convention.errorCode.type.DynamicErrorCode;
import org.yilena.myShortLink.admin.common.convention.errorCode.type.StaticErrorCode;

public final class UserErrorCodes {

    public static final StaticErrorCode USER_ERROR = new StaticErrorCode("U000001", "请稍后再试~");
    public static final DynamicErrorCode USERNAME_ALREADY_EXIST = new DynamicErrorCode("U000002", "[{0}]已存在");
    public static final StaticErrorCode USER_NOT_EXIST = new StaticErrorCode("U000003", "用户不存在");
    public static final StaticErrorCode USER_QUERY_ONE_USER_LIMIT = new StaticErrorCode("U000004", "歇一下再搜吧~");
    public static final StaticErrorCode USER_LOGIN_LIMIT = new StaticErrorCode("U000005", "请勿短时间内重复登录");
    public static final StaticErrorCode USER_LOGIN_PASSWORD_ERROR = new StaticErrorCode("U000006", "用户名或密码错误");
    public static final StaticErrorCode LOGIN_OUT_TIME = new StaticErrorCode("U000007", "登录已过期");
    public static final StaticErrorCode USER_LOGOUT_ERROR = new StaticErrorCode("U000008", "当前账户未登录！");
}