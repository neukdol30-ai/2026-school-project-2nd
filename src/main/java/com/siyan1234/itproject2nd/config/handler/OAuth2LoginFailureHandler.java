package com.siyan1234.itproject2nd.config.handler;

import com.siyan1234.itproject2nd.config.security.SecurityPaths;
import com.siyan1234.itproject2nd.member.service.OAuth2DetailsService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

@Slf4j
@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    // 화이트 리스트에 없는 오류일 때 화면 기본 문구
    private static final String DEFAULT_MESSAGE = "소셜 로그인에 실패했습니다. 잠시 후 다시 시도해 주세요.";

    // 프레임워크가 만든 오류는 설명란에 영문 스택과 내부 API 주소 통째로 포함. 화면에 뜨면 X
    private static final Set<String> ALLOWED_ERROR_CODES = Set.of(
            OAuth2DetailsService.ERROR_EMAIL_ALREADY_REGISTERED, // STEP 1에서 사용
            OAuth2DetailsService.ERROR_MEMBER_BANNED, // STEP 2에서 사용 예정
            OAuth2DetailsService.ERROR_MEMBER_NOT_FOUND // STEP 2에서 사용 예정
    );

    // 매개변수 3개는 Security가 넘겨주는 값. / 소셜 로그인 처리 중 예외 터지면 Security가 이 메서드 호출.
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, // 실패한 요청 (예 : /login/oauth2/code/kakao)
                                        HttpServletResponse response, // 이 객체로 리다이렉트를 지시
                                        AuthenticationException exception // 실패 원인. OAuth2DetailService가 던진 예외가 여기 온다.
    ) throws IOException, ServletException {

        String message = resolveMessage(exception); // 화면에 띄울 문구 결정 (아래 메서드)

        HttpSession session = request.getSession(); // 없으면 새로 만듦
        session.setAttribute(
                CustomLoginFailureHandler.LOGIN_ERROR_MESSAGE_SESSION_KEY, // 상수 값 재사용
                message); // MemberController.loginForm()이 이 값을 꺼내 모델에 담아 화면으로 보냄

        // 로그인 화면으로 되돌려 보냄. withContextPath = 배포 경로가 붙어도 주소가 깨지지 않게 보정
        response.sendRedirect(
                SecurityPaths.withContextPath(request, SecurityPaths.MEMBER_LOGIN + "?error=social"));
    }

    // 오류 코드가 화이트 리스트에 있을 때만 우리가 쓴 문구를 쓰고, 나머지는 전부 기본 문구로 덮음
    private String resolveMessage(AuthenticationException exception) {
        // instanceof + 변수 선언을 한 번에 하는 문법(패턴 매칭) - 소셜 실패 예외가 아니면 볼 것 없음
        if (!(exception instanceof OAuth2AuthenticationException oauth2Exception)) {
            return DEFAULT_MESSAGE;
        }

        OAuth2Error error = oauth2Exception.getError(); // 오류 코드 + 설명문
        if (error == null) { // 방어적 확인
            return DEFAULT_MESSAGE;
        }

        String errorCode = error.getErrorCode(); // 예 : "email_already_registered"

        if (errorCode == null || !ALLOWED_ERROR_CODES.contains(errorCode)) { // 우리가 만든 오류 아님
            // 원인은 개발자만 보게 콘솔에만 남김. 화면에는 절대 X
            log.warn("소셜 로그인 실패(비공개 처리) errorCode={}, description={}",
                    errorCode, error.getDescription());
            return DEFAULT_MESSAGE;
        }

        String description = error.getDescription(); // 우리가 직접 써 넣은 한국어 안내 문구
        if (description == null || description.isBlank()) { // 비어 있으면 기본 문구로
            return DEFAULT_MESSAGE;
        }

        return description; // 화이트 리스트 통과 -> 우리 문구 그대로 사용
    }
}