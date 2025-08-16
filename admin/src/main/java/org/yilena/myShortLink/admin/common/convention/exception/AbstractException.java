package org.yilena.myShortLink.admin.common.convention.exception;

import lombok.Getter;
import org.springframework.util.StringUtils;
import org.yilena.myShortLink.admin.common.convention.errorCode.type.IErrorCode;

import java.util.Optional;

@Getter
public abstract class AbstractException extends RuntimeException {
 
    public final String errorCode;
 
    public final String errorMessage;
 
    public AbstractException(String message, Throwable throwable, IErrorCode errorCode) {
        super(message, throwable);
        this.errorCode = errorCode.code();
        this.errorMessage = Optional.ofNullable(StringUtils.hasLength(message) ? message : null).orElse(errorCode.message());
    }
}