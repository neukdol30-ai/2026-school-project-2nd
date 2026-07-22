package com.siyan1234.itproject2nd.board.dao;

import com.siyan1234.itproject2nd.board.dto.GuestAuthorDto;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GuestAuthorDao {

    // 비회원 작성자 저장
    int insertGuestAuthor(GuestAuthorDto guestAuthorDto);

    // 비회원 작성자 번호로 조회
    GuestAuthorDto findByNo(Long no);

    // 비회원 작성자 삭제
    int deleteByNo(Long no);
}
