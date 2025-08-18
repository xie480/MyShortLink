

package org.yilena.myShortLink.project.config;


import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.yilena.myShortLink.project.common.biz.user.UserTransmitFilter;

/**
 * 用户配置自动装配
 * 公众号：马丁玩编程，回复：加群，添加马哥微信（备注：link）获取项目资料
 */
@Configuration
@RequiredArgsConstructor
public class UserConfiguration {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 用户信息传递过滤器
     */
    @Bean
    public FilterRegistrationBean<UserTransmitFilter> globalUserTransmitFilter() {
        FilterRegistrationBean<UserTransmitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new UserTransmitFilter(stringRedisTemplate));
        registration.addUrlPatterns("/*");
        registration.setOrder(0);
        return registration;
    }

//    /**
//     * 用户操作流量风控过滤器
//     */
//    @Bean
//    @ConditionalOnProperty(name = "short-link.flow-limit.enable", havingValue = "true")
//    public FilterRegistrationBean<UserFlowRiskControlFilter> globalUserFlowRiskControlFilter(
//            StringRedisTemplate stringRedisTemplate,
//            UserFlowRiskControlConfiguration userFlowRiskControlConfiguration) {
//        FilterRegistrationBean<UserFlowRiskControlFilter> registration = new FilterRegistrationBean<>();
//        registration.setFilter(new UserFlowRiskControlFilter(stringRedisTemplate, userFlowRiskControlConfiguration));
//        registration.addUrlPatterns("/*");
//        registration.setOrder(10);
//        return registration;
//    }
}