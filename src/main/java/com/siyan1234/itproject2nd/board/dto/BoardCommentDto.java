package com.siyan1234.itproject2nd.board.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class BoardCommentDto {

    // 댓글 번호
    private Long no;

    // 어떤 게시글의 댓글인지
    private Long boardNo;

    // 댓글 작성자 번호
    private Long writerNo;

    // 댓글 내용
    private String content;

    // 댓글 작성일
    private LocalDateTime createdDate;

    // 댓글 수정일
    private LocalDateTime modifiedDate;

    // 화면 출력용 작성자 닉네임
    private String writerNickname;
}
