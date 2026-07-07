package com.siyan1234.itproject2nd.member.service;

import com.siyan1234.itproject2nd.member.dao.MemberDao;
import com.siyan1234.itproject2nd.member.dto.CustomUserDetails;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {
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

        return new CustomUserDetails(memberDto);
    }
}
