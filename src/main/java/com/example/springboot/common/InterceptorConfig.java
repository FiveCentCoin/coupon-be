package com.example.springboot.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

@Configuration
public class InterceptorConfig extends WebMvcConfigurationSupport {

    @Override
    protected void addInterceptors(InterceptorRegistry registry) {
        // 配置jwt的拦截器规则
        registry.addInterceptor(jwtInterceptor())
                // 拦截所有的请求路径
                .addPathPatterns("/**")
                // 放行以下接口
                .excludePathPatterns("/login", "/register", "/password", "/user/**", "/type/**", "/goods/**", "/carousel/**", "/orders/**", "/notice/**");
        super.addInterceptors(registry);
    }

    @Bean
    public JwtInterceptor jwtInterceptor() {
        return new JwtInterceptor();
    }

}
