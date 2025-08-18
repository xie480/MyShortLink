package org.yilena.myShortLink.project.common.convention.errorCode.type;

public abstract class AbsErrorCode implements IErrorCode {
    protected final String code;
    protected final String message;
    
    protected AbsErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
    
    @Override
    public String code() {
        return code;
    }
    
    @Override
    public String message() {
        return message;
    }
}