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
}
