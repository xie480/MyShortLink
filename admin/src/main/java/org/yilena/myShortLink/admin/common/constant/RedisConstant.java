package org.yilena.myShortLink.admin.common.constant;

public class RedisConstant {

    // 查询用户名是否存在的分布式锁
    public static final String USERNAME_ALREADY_EXIST_LOCK = "lock:user:username_already_exist_lock:%s";
    // 用户注册分布式锁
    public static final String USER_REGISTER_LOCK = "lock:user:user_register_lock:%s";
    // 查找用户名单一用户限制
    public static final String USER_QUERY_ONE_USER_LIMIT = "limit:user:query_one_username_limit:%s";
    // 查找用户名集体限制
    public static final String USER_QUERY_USERNAMES_LIMIT = "limit:user:query_usernames_limit:%s";
    // 查找用户延迟分布锁
    public static final String USER_QUERY_LOCK = "lock:user:query_lock:%s";
    // 用户登录限制
    public static final String USER_LOGIN_LIMIT_KEY = "limit:user:login_limit:%s";
    // 用户登录分布式锁
    public static final String USER_LOGIN_LOCK = "lock:user:login_lock:%s";
    // 用户登录信息
    public static final String USER_LOGIN_KEY = "user:login:%s";
    // 用户更新信息分布式锁
    public static final String USER_UPDATE_LOCK = "lock:user:update_lock:%s";
}
