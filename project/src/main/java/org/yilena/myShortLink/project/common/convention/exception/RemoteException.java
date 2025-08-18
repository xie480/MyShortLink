package org.yilena.myShortLink.project.common.convention.exception;


import org.yilena.myShortLink.project.common.convention.errorCode.codes.RemoteErrorCodes;
import org.yilena.myShortLink.project.common.convention.errorCode.type.IErrorCode;

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
        return STR."RemoteException{code='\{errorCode}',message='\{errorMessage}'}";
    }
}