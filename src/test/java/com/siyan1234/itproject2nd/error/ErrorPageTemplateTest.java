package com.siyan1234.itproject2nd.error;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErrorPageTemplateTest {

    private TemplateEngine templateEngine;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(resolver);
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
            Context context = new Context(Locale.KOREAN);
            context.setVariable("status", template.contains("5") ? 500 : 404);
            context.setVariable("path", "/template-test");

            String rendered = templateEngine.process(template, context);

            assertTrue(rendered.contains("IT Project 2nd"), template);
            assertTrue(rendered.contains("홈으로 이동"), template);
            assertFalse(rendered.contains("th:replace"), template);
        }
    }
}
