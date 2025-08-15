package org.yilena.myShortLink.admin.common.convention.errorCode.codes;

import org.yilena.myShortLink.admin.common.convention.errorCode.ErrorCodeFactory;

public final class RemoteErrorCodes {
    public static final IErrorCode REMOTE_ERROR = ErrorCodeFactory.of("R000001", "第三方远程服务端默认错误");
}