package org.yilena.myShortLink.project.common.convention.errorCode.codes;


import org.yilena.myShortLink.project.common.convention.errorCode.type.StaticErrorCode;

public final class WarningErrorCodes {
    public static final StaticErrorCode USER_UPDATE_ERROR = new StaticErrorCode("R000001", "用户模块更新信息接口出现攻击现象！");
    public static final StaticErrorCode HTTP_FILTER_ERROR = new StaticErrorCode("R000002", "HTTP过滤器异常，请立刻处理！");
}
