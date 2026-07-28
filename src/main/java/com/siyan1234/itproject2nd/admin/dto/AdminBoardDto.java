package com.siyan1234.itproject2nd.admin.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 관리자 콘솔 게시글 관리 목록용 DTO입니다. */
@Getter
@Setter
public class AdminBoardDto {

    private Long no;

    // 회원 작성자 번호
    private Integer writerNo;

    // 비회원 작성자 번호
    private Integer guestAuthorNo;

    private String category;
    private String answerStatus;
    private String title;
    private String content;
    private Long viewCount;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;

    // 회원 정보
    private String writerName;
    private String writerNickname;

    // 비회원 이름
    private String guestName;

    private Integer answerCount;

    public String getCategoryName() {

        if ("NOTICE".equalsIgnoreCase(category)) {
            return "공지사항";
        }

        if ("QUESTION".equalsIgnoreCase(category)) {
            return "문의게시글";
        }

        return "일반게시글";
    }

    public String getAnswerStatusName() {

        if ("ANSWERED".equalsIgnoreCase(answerStatus)) {
            return "답변 완료";
        }

        if ("WAITING".equalsIgnoreCase(answerStatus)) {
            return "답변 대기";
        }

        return "-";
    }

    public String getDisplayWriter() {

        // 비회원 게시글
        if (guestName != null
                && !guestName.isBlank()) {

            return guestName;
        }

        // 회원 닉네임
        if (writerNickname != null
                && !writerNickname.isBlank()) {

            return writerNickname;
        }

        // 회원 이름
        if (writerName != null
                && !writerName.isBlank()) {

            return writerName;
        }

        return "탈퇴/미지정";
    }
}