package org.yilena.myShortLink.admin.common.biz.user;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
@Component
public class UserTransmitFilter implements Filter {

    private final StringRedisTemplate stringRedisTemplate;

    @SneakyThrows
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        String username = httpServletRequest.getHeader("username");
        UserInfoDTO userInfoDTO = new UserInfoDTO();
        if (StrUtil.isNotBlank(username)) {
            String userId = httpServletRequest.getHeader("userId");
            String realName = httpServletRequest.getHeader("realName");
            userInfoDTO = new UserInfoDTO(userId, username, realName);
        }
        /*
              这里使用嵌套作用域讲整个链放入作用域内，
              因为我们使用了ScopeValue，
              为什么使用这个，一是我们这种面向用户的业务一般都以阻塞操作，也就是IO集中型任务为主，所以我们讲tomcat的线程全部改成了虚拟线程，
              也就是说每个到这里的线程都是虚拟线程，所以我们不能使用ThreadLocal，
              除此之外，我们并不需要使用信号量限制并发，因为我们的线程池是tomcat，它本身就带一定的限制，
              接着，一般虚拟线程过多创建导致CPU占满或者JVM堆内存爆炸都是在自己创建虚拟线程池进行任务的时候才会的，比如下载文件
           */
            try {
                UserContext.runWithUser(userInfoDTO, () -> {
                    try {
                        filterChain.doFilter(servletRequest, servletResponse);
                    } catch (IOException | ServletException e) {
                        // 转换为非受检异常，确保能被全局处理器捕获，因为lambda表达式不支持直接俄抛出非受检异常，所以这里需要做个转换
                        throw new RuntimeException(e);
                    }
                });
            } catch (RuntimeException wrapperEx) {
                // 解包原始异常
                if (wrapperEx.getCause() instanceof IOException) {
                    throw (IOException) wrapperEx.getCause();
                } else if (wrapperEx.getCause() instanceof ServletException) {
                    throw (ServletException) wrapperEx.getCause();
                }
                throw wrapperEx;
            }
        }
    }
    /*
        下面这些是原本想整成双令牌认证方案的，但是这个项目的路径设置及其不合理，无法实现……
        想改的话大费周章，如果你有多余的精力的话可以参考我下面的代码进行改造……
        包括登录登出的接口也注释掉了一部分代码
     */

//    @SneakyThrows
//    @Override
//    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) {
//        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
//        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
//
//        try {
//            // 获取访问令牌
//            String token = extractToken(httpRequest);
//            // 过滤掉不需要token的请求
//            UserInfoDTO userInfo;
//            if (Boolean.FALSE.equals(isSkipTokenValidation(httpRequest.getRequestURI()))) {
//                // 验证访问令牌是否存在
//                if (!StrUtil.isNotBlank(token)) {
//                    throw new AuthenticationCredentialsNotFoundException("访问令牌不存在");
//                }
//                // 存在则解析出用户信息
//                userInfo = processTokenValidation(token, httpResponse);
//            }else{
//                userInfo = null;
//            }
//            // 执行后续过滤链
//            executeWithUserContext(userInfo, servletRequest, servletResponse, filterChain);
//
//        } catch (BadCredentialsException ex) {
//            // 访问令牌错误 - 返回401
//            handleAuthenticationError(httpResponse, ex, HttpServletResponse.SC_UNAUTHORIZED);
//        } catch (AuthenticationException | AuthenticationCredentialsNotFoundException ex) {
//            // 其他认证异常 - 返回402
//            handleAuthenticationError(httpResponse, ex, HttpServletResponse.SC_PAYMENT_REQUIRED);
//        } catch (Exception ex) {
//            // 其他未知异常 - 返回500
//            handleAuthenticationError(httpResponse, ex, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//        }
//    }

//    private Boolean isSkipTokenValidation(String url) {
//        /*
//            这个项目的接口路径设计完全无法做到分辨公开和私有，因为userController的所有接口都包含/api/short-link/admin/v1/user，所以我们无法在不改动原有路径的基础上
//            过滤掉公开接口，所以我们就写死让所有接口都变成私有，但如果要调用登录接口获取token就得手动注释掉验证代码了
//         */
//        return true;
//    }
//
//    /**
//     * 处理令牌验证和刷新逻辑
//     */
//    private UserInfoDTO processTokenValidation(String token, HttpServletResponse response) throws AuthenticationException {
//        // 获取JTI并检查黑名单
//        String jti = tokenProvider.getJtiFromToken(token);
//        if (tokenSessionService.isJtiBlacklisted(jti)) {
//            throw new AuthenticationCredentialsNotFoundException("令牌已失效");
//        }
//
//        // 验证访问令牌有效性
//        if (!tokenProvider.validateToken(token)) {
//            throw new BadCredentialsException("访问令牌无效");
//        }
//
//        // 解析访问令牌
//        Claims claims = tokenProvider.parseToken(token);
//        String username = claims.getSubject();
//        String tokenVersion = claims.get("tokenVersion", String.class);
//
//        // 验证刷新令牌有效性
//        if (!tokenSessionService.isValidToken(username, tokenVersion)) {
//            throw new AuthenticationException("刷新令牌无效");
//        }
//
//        // 检查是否需要刷新令牌
//        if (tokenProvider.shouldRefreshToken(token)) {
//            refreshToken(token, jti, username, tokenVersion, response);
//        }
//
//        // 构建用户信息对象
//        return buildUserInfo(claims, username);
//    }
//
//    /**
//     * 刷新访问令牌
//     */
//    private void refreshToken(String oldToken, String jti, String username, String tokenVersion, HttpServletResponse response) {
//        // 生成新的访问令牌
//        String newToken = tokenProvider.generateToken(username, tokenVersion);
//        response.setHeader("X-New-Access-Token", newToken);
//
//        // 将旧令牌加入短期黑名单
//        long ttl = tokenProvider.getRemainingTime(oldToken);
//        tokenSessionService.addToShortBlacklist(jti, ttl);
//
//        log.info("令牌已刷新: username={}, jti={}", username, jti);
//    }
//
//    /**
//     * 构建用户信息对象
//     */
//    private UserInfoDTO buildUserInfo(Claims claims, String username) {
//        return UserInfoDTO.builder()
//                .userId(claims.get("userId", String.class))
//                .username(username)
//                .build();
//    }
//
//    /**
//     * 在用户上下文中执行过滤器链
//     */
//    private void executeWithUserContext(UserInfoDTO userInfo,
//                                        ServletRequest request,
//                                        ServletResponse response,
//                                        FilterChain chain) throws IOException, ServletException {
//          /*
//              这里使用嵌套作用域讲整个链放入作用域内，
//              因为我们使用了ScopeValue，
//              为什么使用这个，一是我们这种面向用户的业务一般都以阻塞操作，也就是IO集中型任务为主，所以我们讲tomcat的线程全部改成了虚拟线程，
//              也就是说每个到这里的线程都是虚拟线程，所以我们不能使用ThreadLocal，
//              除此之外，我们并不需要使用信号量限制并发，因为我们的线程池是tomcat，它本身就带一定的限制，
//              接着，一般虚拟线程过多创建导致CPU占满或者JVM堆内存爆炸都是在自己创建虚拟线程池进行任务的时候才会的，比如下载文件
//           */
//        try {
//            UserContext.runWithUser(userInfo, () -> {
//                try {
//                    chain.doFilter(request, response);
//                } catch (IOException | ServletException e) {
//                    // 转换为非受检异常，确保能被全局处理器捕获，因为lambda表达式不支持直接俄抛出非受检异常，所以这里需要做个转换
//                    throw new RuntimeException(e);
//                }
//            });
//        } catch (RuntimeException wrapperEx) {
//            // 解包原始异常
//            if (wrapperEx.getCause() instanceof IOException) {
//                throw (IOException) wrapperEx.getCause();
//            } else if (wrapperEx.getCause() instanceof ServletException) {
//                throw (ServletException) wrapperEx.getCause();
//            }
//            throw wrapperEx;
//        }
//    }
//
//    /**
//     * 统一处理认证错误
//     */
//    private void handleAuthenticationError(HttpServletResponse response,
//                                           Exception ex,
//                                           int statusCode) throws IOException {
//
//        // 设置响应状态和内容类型
//        response.setStatus(statusCode);
//        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
//
//        // 构建安全错误响应
//        String errorMessage = sanitizeErrorMessage(ex.getMessage());
//        String jsonResponse = String.format(
//                "{\"code\": %d, \"message\": \"%s\", \"errorType\": \"AUTHENTICATION_FAILURE\"}",
//                statusCode, errorMessage
//        );
//
//        response.getWriter().write(jsonResponse);
//        log.warn("认证失败: status={}, error={}", statusCode, errorMessage, ex);
//    }
//
//    /**
//     * 过滤敏感错误信息
//     */
//    private String sanitizeErrorMessage(String rawMessage) {
//        // 实际项目中应根据业务需求定制
//        if (rawMessage.contains("令牌已失效")) {
//            return "令牌已过期，请重新登录";
//        } else if (rawMessage.contains("访问令牌无效")) {
//            return "无效的访问令牌";
//        } else if (rawMessage.contains("刷新令牌无效")) {
//            return "会话已过期，请重新登录";
//        }
//        return "认证失败";
//    }
//
//    /**
//     * 从请求头提取令牌
//     */
//    private String extractToken(HttpServletRequest request) {
//        String header = request.getHeader("Authorization");
//        return header;
//    }