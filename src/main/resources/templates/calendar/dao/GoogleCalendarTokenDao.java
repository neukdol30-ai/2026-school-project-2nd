package com.siyan1234.itproject2nd.calendar.dao;

import com.siyan1234.itproject2nd.calendar.dto.GoogleCalendarTokenDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GoogleCalendarTokenDao {

    // 회원 번호로 구글 토큰 조회
    GoogleCalendarTokenDto findByMemberNo(@Param("memberNo") Integer memberNo);

    // 구글 토큰 처음 저장
    int insertToken(GoogleCalendarTokenDto tokenDto);

    // 기존 구글 토큰 갱신
    int updateToken(GoogleCalendarTokenDto tokenDto);

    // 구글 연동 해제
    int deleteByMemberNo(@Param("memberNo") Integer memberNo);
}

