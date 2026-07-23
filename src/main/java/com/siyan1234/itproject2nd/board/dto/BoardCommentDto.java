package com.siyan1234.itproject2nd.board.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class BoardCommentDto {


    // 답변 번호
    private Long no;

    // 답변이 작성된 문의 게시글 번호
    private Long boardNo;

    // 답변 작성자 회원 번호
    private Integer writerNo;

    // TOAST UI Editor에서 작성한 답변 HTML
    // DB의 board_comment.content CLOB 컬럼과 연결
    private String content;


    // 답변 작성일
    private LocalDateTime createdDate;

    // 답변 최종 수정일
    private LocalDateTime modifiedDate;


    // 문의 작성자가 채택한 답변인지 여부
    private String acceptedYn;

    // 답변 작성자의 닉네임
    private String writerNickname;
}