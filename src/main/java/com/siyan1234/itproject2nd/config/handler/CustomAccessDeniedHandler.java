package com.siyan1234.itproject2nd.config.handler;

import com.siyan1234.itproject2nd.config.security.SecurityPaths;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** 로그인은 되어 있지만 권한이 부족한 요청의 이동 경로를 결정합니다. */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {
        String redirectPath = SecurityPaths.isAdminArea(request)
                ? SecurityPaths.ADMIN_LOGIN + "?error=role"
                : SecurityPaths.MEMBER_LOGIN + "?error=denied";

        response.sendRedirect(SecurityPaths.withContextPath(request, redirectPath));
    }
}
