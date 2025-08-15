package org.yilena.myShortLink.admin.common.convention.result;

import org.yilena.myShortLink.admin.common.convention.errorCode.codes.SystemErrorCodes;
import org.yilena.myShortLink.admin.common.convention.exception.AbstractException;

import java.util.Optional;

public final class Results {
 
    /**
     * 创建一个表示成功的 Result 对象，不包含任何数据
     */
    public static Result<Void> success() {
        return new Result<Void>()
                .setCode(Result.SUCCESS_CODE);
    }
 
    /**
     * 创建一个表示成功的 Result 对象，包含指定的数据
     */
    public static <T> Result<T> success(T data) {
        return new Result<T>()
                .setCode(Result.SUCCESS_CODE)
                .setData(data);
    }
 
    /**
     * 创建一个表示失败的 Result 对象，使用默认的错误代码和消息
     */
    public static Result<Void> failure() {
        return new Result<Void>()
                .setCode(SystemErrorCodes.SYSTEM_ERROR.code())
                .setMessage(SystemErrorCodes.SYSTEM_ERROR.message());
    }
 
    /**
     * 根据给定的异常创建一个表示失败的 Result 对象，如果异常的错误代码或消息为空，则使用默认的错误代码和消息
     */
    public static Result<Void> failure(AbstractException abstractException) {
        String errorCode = Optional.ofNullable(abstractException.getErrorCode())
                .orElse(SystemErrorCodes.SYSTEM_ERROR.code());
        String errorMessage = Optional.ofNullable(abstractException.getErrorMessage())
                .orElse(SystemErrorCodes.SYSTEM_ERROR.message());
        return new Result<Void>()
                .setCode(errorCode)
                .setMessage(errorMessage);
    }
 
    /**
     * 创建一个表示失败的 Result 对象，使用指定的错误代码和消息
     */
    public static Result<Void> failure(String errorCode, String errorMessage) {
        return new Result<Void>()
                .setCode(errorCode)
                .setMessage(errorMessage);
    }
}