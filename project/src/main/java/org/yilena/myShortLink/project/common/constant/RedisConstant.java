package org.yilena.myShortLink.project.common.constant;

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


    // 分组保存分布式锁
    public static final String GROUP_SAVE_LOCK = "lock:group:save_lock:%s";
    // 分组保存限制
    public static final String GROUP_SAVE_LIMIT = "limit:group:save_limit";
    // 分组查询限制
    public static final String GROUP_SELECT_LIMIT = "limit:group:select_limit";
    // 分组查询分布式锁
    public static final String GROUP_SELECT_LOCK = "lock:group:select_lock:%s";
    // 分组更新分布式锁
    public static final String GROUP_UPDATE_LOCK = "lock:group:update_lock:%s";
    // 分组删除分布式锁
    public static final String GROUP_DELETE_LOCK = "lock:group:delete_lock:%s";
    // 分组排序分布式锁
    public static final String GROUP_SORT_LOCK = "lock:group:sort_lock:%s";

    // 短链跳转映射缓存
    public static final String GOTO_SHORT_LINK_KEY = "goto:short_link:%s";
    // 短链跳转映射空缓存
    public static final String GOTO_IS_NULL_SHORT_LINK_KEY = "goto:is_null_short_link:%s";
    // 短链跳转映射缓存重建分布式锁
    public static final String GOTO_SHORT_LINK_LOCK = "lock:goto:short_link_lock:%s";
}
