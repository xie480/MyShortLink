

package org.yilena.myShortLink.admin.config;


import org.springframework.context.annotation.Configuration;
import org.yilena.myShortLink.admin.common.web.GlobalExceptionHandler;

/**
 * Web 组件自动装配
 */
@Configuration
public class WebAutoConfiguration {

    /**
     * 构建全局异常拦截器组件 Bean
     */
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
