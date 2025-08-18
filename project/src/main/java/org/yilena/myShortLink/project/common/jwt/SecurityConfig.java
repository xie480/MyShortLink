package org.yilena.myShortLink.project.common.jwt;//package org.yilena.myShortLink.admin.common.jwt;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.CorsConfigurationSource;
//import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//import org.yilena.myShortLink.admin.common.biz.user.UserTransmitFilter;
//
//import java.util.List;
//
//@Configuration
//// 启用Spring Security的Web安全支持
//@EnableWebSecurity
//// 启用方法级安全控制
//@EnableMethodSecurity(prePostEnabled = true)
//@RequiredArgsConstructor
//public class SecurityConfig {
//
//    private final UserTransmitFilter userTransmitFilter;
//
//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//                .csrf().disable()
//                .cors().configurationSource(corsConfigurationSource()).and()
//                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
//                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers("/api/auth/**", "/api/short-link/admin/v1/user", "/swagger-ui/**").permitAll()
//                        .anyRequest().authenticated()
//                );
//
//        // 在用户名密码认证过滤器前添加JWT过滤器
//        http.addFilterBefore(userTransmitFilter, UsernamePasswordAuthenticationFilter.class);
//
//        // 安全头部强化配置
//        http.headers()
//                .contentSecurityPolicy("script-src 'self'")
//                .and()
//                .httpStrictTransportSecurity()
//                .includeSubDomains(true)
//                .maxAgeInSeconds(31536000);
//
//        return http.build();
//    }
//
//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration config = new CorsConfiguration();
//        // 允许所有源（生产环境应指定具体域名）
//        config.setAllowedOrigins(List.of("*"));
//        // 允许的HTTP方法
//        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
//        // 允许的请求头
//        config.setAllowedHeaders(List.of("Authorization", "X-New-Access-Token"));
//        // 暴露给客户端的响应头
//        config.setExposedHeaders(List.of("X-New-Access-Token"));
//        // 注册CORS配置应用到所有路径
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", config);
//        return source;
//    }
//
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }
//}