package com.siyan1234.itproject2nd.board.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class GuestAuthorDto {

    // guest_author 테이블의 기본키
    private Long no;

    // 비회원이 입력한 이름
    private String guestName;

    /*
     * 사용자가 화면에서 입력한 비밀번호 원문
     * DB에는 저장하지 않고 암호화할 때만 사용
     */
    @ToString.Exclude
    private String password;

    /*
     * 암호화된 비밀번호
     * guest_author.password_hash 컬럼에 저장
     */
    @ToString.Exclude
    private String passwordHash;

    // 작성자 정보 생성일
    private LocalDateTime createdDate;
}