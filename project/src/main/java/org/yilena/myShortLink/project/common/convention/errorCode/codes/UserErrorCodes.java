package org.yilena.myShortLink.project.common.convention.errorCode.codes;


import org.yilena.myShortLink.project.common.convention.errorCode.type.DynamicErrorCode;
import org.yilena.myShortLink.project.common.convention.errorCode.type.StaticErrorCode;
/*
    注意，正式项目中错误码一定要放入公共模块当中，或者直接根据不同模块进一步分级避免重复
    我们这里这么写很不严谨，只是单纯图方便的做法
 */
public final class UserErrorCodes {

    public static final StaticErrorCode USER_ERROR = new StaticErrorCode("U000001", "请稍后再试~");
    public static final DynamicErrorCode USERNAME_ALREADY_EXIST = new DynamicErrorCode("U000002", "[{0}]已存在");
    public static final StaticErrorCode USER_NOT_EXIST = new StaticErrorCode("U000003", "用户不存在");
    public static final StaticErrorCode USER_QUERY_ONE_USER_LIMIT = new StaticErrorCode("U000004", "歇一下再搜吧~");
    public static final StaticErrorCode USER_LOGIN_LIMIT = new StaticErrorCode("U000005", "请勿短时间内重复登录");
    public static final StaticErrorCode USER_LOGIN_PASSWORD_ERROR = new StaticErrorCode("U000006", "用户名或密码错误");
    public static final StaticErrorCode LOGIN_OUT_TIME = new StaticErrorCode("U000007", "登录已过期");
    public static final StaticErrorCode USER_LOGOUT_ERROR = new StaticErrorCode("U000008", "当前账户未登录！");
    public static final StaticErrorCode GROUP_COUNT_OUT = new StaticErrorCode("U000009", "当前用户已创建组数达到上限");
    public static final StaticErrorCode GROUP_NOT_EXIST = new StaticErrorCode("U000010", "分组不存在");
    public static final StaticErrorCode SHORT_LINK_NOT_EXIST = new StaticErrorCode("U000011", "短链不存在");
    public static final StaticErrorCode NOT_FOUND_ERROR = new StaticErrorCode("U000012", "未找到该资源");
    public static final StaticErrorCode SHORT_LINK_NOT_EXIST_OR_NOT_ENABLE = new StaticErrorCode("U000013", "短链不存在或已禁用");
    public static final StaticErrorCode SHORT_LINK_NOT_EXIST_OR_ENABLE = new StaticErrorCode("U000014", "短链不存在或已启用");
    public static final StaticErrorCode SHORT_LINK_IS_ENABLE = new StaticErrorCode("U000015", "已启用的短链无法删除！");
}