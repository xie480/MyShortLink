package org.yilena.myShortLink.admin.common.convention.errorCode;

import org.yilena.myShortLink.admin.common.convention.errorCode.codes.IErrorCode;

import java.text.MessageFormat;

public final class ErrorCodeFactory {
    
    // 创建静态错误码
    public static IErrorCode of(String code, String message) {
        StaticErrorCode errorCode = new StaticErrorCode(code, message);
        ErrorCodeRegistry.register(errorCode);
        return errorCode;
    }
    
    // 创建动态错误码
    public static IErrorCode dynamic(String code, String messageTemplate) {
        DynamicErrorCode errorCode = new DynamicErrorCode(code, messageTemplate);
        ErrorCodeRegistry.register(errorCode);
        return errorCode;
    }
    
    // 静态错误码实现
    private static class StaticErrorCode extends AbsErrorCode {
        StaticErrorCode(String code, String message) {
            super(code, message);
        }
    }
    
    // 动态错误码实现
    private static class DynamicErrorCode extends AbsErrorCode {
        private final String template;
        
        DynamicErrorCode(String code, String template) {
            super(code, template);
            this.template = template;
        }
        
        @Override
        public String message() {
            return template; 
        }
        
        public String formatMessage(Object... args) {
            return MessageFormat.format(template, args);
        }
    }
}