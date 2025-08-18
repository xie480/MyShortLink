package org.yilena.myShortLink.project.common.convention.errorCode.type;


import java.text.MessageFormat;

// 动态错误码实现
public class DynamicErrorCode extends AbsErrorCode {
    private final String template;

    public DynamicErrorCode(String code, String template) {
        super(code, template);
        this.template = template;
    }

    @Override
    public String message() {
        return template;
    }

    public DynamicErrorCode formatMessage(Object... args) {
            return new DynamicErrorCode(code, MessageFormat.format(template, args));
    }
}