package com.siyan1234.itproject2nd.member.service;

import com.siyan1234.itproject2nd.member.dao.MemberDao;
import com.siyan1234.itproject2nd.member.dto.CustomUserDetails;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {

    private static final String BANNED_MESSAGE_PREFIX = "BANNED|";

    // UserDetailsService: Security가 로그인 시 자동으로 호출하는 규격.
    private final MemberDao memberDao; // 회원 조회 DAO

    @Override // Security 로그인 버튼 처리 시 메서드 자동 호출. / 매개변수 username = 로그인 폼에서 넘어온 아이디(=memberId)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 입력한 아이디로 DB에서 회원 1명 조회
        MemberDto memberDto = memberDao.findByMemberId(username);

        // 없으면 예외 발생 -> Security가 "로그인 실패" 처리
        if (memberDto == null) {
            throw new UsernameNotFoundException("존재하지 않는 아이디입니다: " + username);
        }

        if (memberDto.isBanned()) {
            throw new LockedException(BANNED_MESSAGE_PREFIX + memberDto.displayBanReason());
        }

        return new CustomUserDetails(memberDto);
    }

    public static boolean isBannedLoginException(Exception exception) {
        return exception instanceof LockedException
                && exception.getMessage() != null
                && exception.getMessage().startsWith(BANNED_MESSAGE_PREFIX);
    }

    public static String extractBanReason(Exception exception) {
        if (!isBannedLoginException(exception)) {
            return "관리자에 의해 이용이 제한된 계정입니다.";
        }
        return exception.getMessage().substring(BANNED_MESSAGE_PREFIX.length());
    }
}
