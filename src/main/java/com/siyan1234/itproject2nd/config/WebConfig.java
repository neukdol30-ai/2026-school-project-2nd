package com.siyan1234.itproject2nd.config;

//(이미지삽입)이미지 URL 연결 설정
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.board-image-dir}")
    private String boardImageDir;

    @Override
    public void addResourceHandlers(
            ResourceHandlerRegistry registry
    ) {

        String location = Paths.get(boardImageDir)
                .toAbsolutePath()
                .normalize()
                .toUri()
                .toString();

        registry.addResourceHandler(
                        "/uploads/board-images/**"
                )
                .addResourceLocations(location);
    }
}