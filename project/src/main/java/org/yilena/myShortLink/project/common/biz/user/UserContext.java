package org.yilena.myShortLink.project.common.biz.user;


import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * 用户上下文 (基于 Java 21 ScopedValue)
 * 公众号：马丁玩编程，回复：加群，添加马哥微信（备注：link）获取项目资料
 */
public final class UserContext {

    /**
     * ScopedValue
     */
    private static final ScopedValue<UserInfoDTO> USER_SCOPED_VALUE = ScopedValue.newInstance();

    /**
     * 在用户上下文中执行任务（推荐入口点）
     *
     * @param user 用户信息
     * @param task 待执行的任务
     */
    public static void runWithUser(UserInfoDTO user, Runnable task) {
        ScopedValue.where(USER_SCOPED_VALUE, user).run(task);
    }

    /**
     * 在用户上下文中执行带返回值的任务（推荐入口点）
     *
     * @param user 用户信息
     * @param task 待执行的任务
     * @param <T>  返回值类型
     * @return 任务执行结果
     */
    public static <T> T callWithUser(UserInfoDTO user, Callable<T> task) throws Exception {
        return ScopedValue.where(USER_SCOPED_VALUE, user).call(task);
    }

    /**
     * 嵌套绑定用户上下文（用于子作用域）
     *
     * @param user 用户信息
     * @param task 待执行的任务
     */
    public static void runWithNestedUser(UserInfoDTO user, Runnable task) {
        // 创建嵌套绑定 - 优先使用外层作用域的值
        ScopedValue.Carrier carrier = ScopedValue.where(USER_SCOPED_VALUE, user);
        // 运行任务
        carrier.run(task);
    }

    /**
     * 获取上下文中用户详情
     *
     * @return 用户详情对象 (可能为 null)
     */
    public static UserInfoDTO getUser() {
        // 在当前作用域内获取值
        return USER_SCOPED_VALUE.get();
    }

    /**
     * 获取上下文中用户 ID
     *
     * @return 用户 ID (可能为 null)
     */
    public static String getUserId() {
        return Optional.ofNullable(USER_SCOPED_VALUE.get())
                .map(UserInfoDTO::getUserId)
                .orElse(null);
    }

    /**
     * 获取上下文中用户名称
     *
     * @return 用户名称 (可能为 null)
     */
    public static String getUsername() {
        return Optional.ofNullable(USER_SCOPED_VALUE.get())
                .map(UserInfoDTO::getUsername)
                .orElse(null);
    }

    /**
     * 获取上下文中用户真实姓名
     *
     * @return 用户真实姓名 (可能为 null)
     */
    public static String getRealName() {
        return Optional.ofNullable(USER_SCOPED_VALUE.get())
                .map(UserInfoDTO::getRealName)
                .orElse(null);
    }

    // 注意：不再需要 removeUser() 方法，作用域结束自动清理
}