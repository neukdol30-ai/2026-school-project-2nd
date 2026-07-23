package com.siyan1234.itproject2nd.board.dto;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Getter
@Setter
public class BoardDto {

    private Long no;

    private Integer writerNo;

    // 문의글 답변 상태
    // WAITING: 답변대기
    // ANSWERED: 답변완료
    private String answerStatus;

    // 해당 문의글에 등록된 답변 개수
    private Integer answerCount;

    private String category;

    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 200, message = "제목은 200자 이하로 입력해주세요.")
    private String title;

    @NotBlank(message = "내용을 입력해주세요.")
    private String content;

    private Long viewCount;

    private LocalDateTime createdDate;

    private LocalDateTime modifiedDate;

    // 목록/상세에서 작성자 이름 또는 닉네임 보여주기용
    private String writerName;

    private String writerNickname;

    // 비회원 작성자 번호
    private Long guestAuthorNo;

    // 목록/상세에서 출력할 비회원 이름
    private String guestName;

    // 비회원이 화면에서 입력하는 원본 비밀번호
    // guest_author 테이블에는 직접 저장하지 않음
    private String guestPassword;
}