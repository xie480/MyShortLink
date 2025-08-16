package org.yilena.myShortLink.project.common.convention.errorCode.codes;


import org.yilena.myShortLink.project.common.convention.errorCode.ErrorCodeFactory;

public final class UserErrorCodes {

    public static final IErrorCode USER_ERROR = ErrorCodeFactory.of("U000001", "操作频繁~");
    public static final IErrorCode USERNAME_ALREADY_EXIST = ErrorCodeFactory.of("U000002", "用户名已存在");

}