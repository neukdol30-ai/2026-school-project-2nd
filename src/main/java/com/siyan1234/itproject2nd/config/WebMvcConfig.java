package com.siyan1234.itproject2nd.config;

import com.siyan1234.itproject2nd.config.security.SecurityPaths;
import com.siyan1234.itproject2nd.cookie.interceptor.VisitLogInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Spring MVC 설정: 방문 기록 Interceptor를 등록합니다. */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final VisitLogInterceptor visitLogInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(visitLogInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(SecurityPaths.VISIT_LOG_EXCLUDE_PATTERNS);
    }
}