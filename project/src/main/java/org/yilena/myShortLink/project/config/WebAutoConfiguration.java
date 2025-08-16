

package org.yilena.myShortLink.project.config;


import org.springframework.context.annotation.Bean;
import org.yilena.myShortLink.project.common.web.GlobalExceptionHandler;

/**
 * Web 组件自动装配
 */
public class WebAutoConfiguration {

    /**
     * 构建全局异常拦截器组件 Bean
     */
    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
