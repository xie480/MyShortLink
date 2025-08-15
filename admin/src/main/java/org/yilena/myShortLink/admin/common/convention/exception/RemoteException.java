package org.yilena.myShortLink.admin.common.convention.exception;

import org.yilena.myShortLink.admin.common.convention.errorCode.codes.IErrorCode;
import org.yilena.myShortLink.admin.common.convention.errorCode.codes.RemoteErrorCodes;

public class RemoteException extends AbstractException {
 
    public RemoteException(String message) {
        this(message, null, RemoteErrorCodes.REMOTE_ERROR);
    }
 
    public RemoteException(String message, IErrorCode errorCode) {
        this(message, null, errorCode);
    }
 
    public RemoteException(String message, Throwable throwable, IErrorCode errorCode) {
        super(message, throwable, errorCode);
    }
 
    @Override
    public String toString() {
        return "RemoteException{" +
                "code='" + errorCode + "'," +
                "message='" + errorMessage + "'" +
                '}';
    }
}