package org.yilena.myShortLink.project.common.convention.errorCode.codes;


import org.yilena.myShortLink.project.common.convention.errorCode.type.DynamicErrorCode;
import org.yilena.myShortLink.project.common.convention.errorCode.type.StaticErrorCode;

public final class SystemErrorCodes {
    /*
        普通错误码
     */
    public static final StaticErrorCode SYSTEM_ERROR = new StaticErrorCode("S000001", "系统繁忙~");
    public static final DynamicErrorCode MQ_RETRY = new DynamicErrorCode("S000002", "uv为[{0}]的消息正在准备重试……");
    public static final DynamicErrorCode MQ_NEED_RETRY = new DynamicErrorCode("S000003", "uv为[{0}]的消息需要重试……");
}