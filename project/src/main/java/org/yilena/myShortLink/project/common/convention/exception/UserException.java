package org.yilena.myShortLink.project.common.convention.exception;

import org.yilena.myShortLink.admin.common.convention.errorCode.codes.IErrorCode;
import org.yilena.myShortLink.admin.common.convention.errorCode.codes.UserErrorCodes;
import org.yilena.myShortLink.admin.common.convention.exception.AbstractException;

public class UserException extends AbstractException {

    public UserException (IErrorCode errorCode) {
        this(null, null, errorCode);
    }
 
    public UserException (String message) {
        this(message, null, UserErrorCodes.USER_ERROR);
    }
 
    public UserException (String message, IErrorCode errorCode) {
        this(message, null, errorCode);
    }
 
    public UserException (String message, Throwable throwable, IErrorCode errorCode) {
        super(message, throwable, errorCode);
    }
 
    @Override
    public String toString() {
        return "ClientException{" +
                "code='" + errorCode + "'," +
                "message='" + errorMessage + "'" +
                '}';
    }
}