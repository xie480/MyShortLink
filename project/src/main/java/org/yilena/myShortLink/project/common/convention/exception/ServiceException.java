package org.yilena.myShortLink.project.common.convention.exception;


import org.yilena.myShortLink.project.common.convention.errorCode.codes.IErrorCode;
import org.yilena.myShortLink.project.common.convention.errorCode.codes.SystemErrorCodes;

import java.util.Optional;

public class ServiceException extends AbstractException {
 
    public ServiceException(String message) {
        this(message, null, SystemErrorCodes.SYSTEM_ERROR);
    }
 
    public ServiceException(IErrorCode errorCode) {
        this(null, errorCode);
    }
 
    public ServiceException(String message, IErrorCode errorCode) {
        this(message, null, errorCode);
    }
 
    public ServiceException(String message, Throwable throwable, IErrorCode errorCode) {
        super(Optional.ofNullable(message).orElse(errorCode.message()), throwable, errorCode);
    }
 
    @Override
    public String toString() {
        return "ServiceException{" +
                "code='" + errorCode + "'," +
                "message='" + errorMessage + "'" +
                '}';
    }
}