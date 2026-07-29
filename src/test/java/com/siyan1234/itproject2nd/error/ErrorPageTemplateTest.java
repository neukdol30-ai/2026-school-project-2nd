package com.siyan1234.itproject2nd.error;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.IServletWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErrorPageTemplateTest {

    private SpringTemplateEngine templateEngine;
    private MockServletContext servletContext;
    private JakartaServletWebApplication webApplication;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        /*
         * 실제 Spring Boot 실행 환경과 동일하게 SpringTemplateEngine을 사용합니다.
         * 일반 TemplateEngine은 OGNL 기반 StandardDialect를 사용하기 때문에
         * Spring 전용 Thymeleaf 의존성 구성에서는 NoClassDefFoundError가 발생할 수 있습니다.
         */
        templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);

        /*
         * 에러 페이지에는 @{/...} 형식의 컨텍스트 상대 URL이 있으므로
         * 일반 Context가 아니라 Servlet 기반 WebContext를 준비합니다.
         */
        servletContext = new MockServletContext();
        webApplication = JakartaServletWebApplication.buildApplication(servletContext);
    }

    @Test
    void statusSpecificAndSeriesTemplatesRenderWithoutParsingErrors() {
        List<String> templates = List.of(
                "error/400",
                "error/401",
                "error/403",
                "error/404",
                "error/405",
                "error/500",
                "error/503",
                "error/4xx",
                "error/5xx",
                "error"
        );

        for (String template : templates) {
            WebContext context = createWebContext();
            context.setVariable("status", template.contains("5") ? 500 : 404);
            context.setVariable("path", "/template-test");

            String rendered = templateEngine.process(template, context);

            assertTrue(rendered.contains("IT Project 2nd"), template);
            assertTrue(rendered.contains("홈으로 이동"), template);
            assertFalse(rendered.contains("th:replace"), template);
        }
    }

    private WebContext createWebContext() {
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setContextPath("");
        request.setRequestURI("/template-test");

        MockHttpServletResponse response = new MockHttpServletResponse();

        IServletWebExchange exchange = webApplication.buildExchange(request, response);
        return new WebContext(exchange, Locale.KOREAN);
    }
}
