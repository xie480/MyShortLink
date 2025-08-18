package org.yilena.myShortLink.project.common.convention.exception;


import org.yilena.myShortLink.project.common.convention.errorCode.codes.SystemErrorCodes;
import org.yilena.myShortLink.project.common.convention.errorCode.type.IErrorCode;

import java.util.Optional;

public class SystemException extends AbstractException {
 
    public SystemException(String message) {
        this(message, null, SystemErrorCodes.SYSTEM_ERROR);
    }
 
    public SystemException(IErrorCode errorCode) {
        this(null, errorCode);
    }
 
    public SystemException(String message, IErrorCode errorCode) {
        this(message, null, errorCode);
    }
 
    public SystemException(String message, Throwable throwable, IErrorCode errorCode) {
        super(Optional.ofNullable(message).orElse(errorCode.message()), throwable, errorCode);
    }
 
    @Override
    public String toString() {
        return STR."SystemException{code='\{errorCode}',message='\{errorMessage}'}";
    }
}