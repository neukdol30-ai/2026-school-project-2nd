package com.siyan1234.itproject2nd.board.controller;

import com.siyan1234.itproject2nd.board.dto.BoardCommentDto;
import com.siyan1234.itproject2nd.board.dto.BoardDto;
import com.siyan1234.itproject2nd.board.service.BoardCommentService;
import com.siyan1234.itproject2nd.board.service.BoardService;
import com.siyan1234.itproject2nd.member.dto.CustomUserDetails;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/board/comment")
public class BoardCommentController {

    private final BoardCommentService boardCommentService;
    private final BoardService boardService;

    // =========================
    // 답변 등록 처리
    // =========================
    //
    // 문의 게시글 상세 화면에서 작성한 답변을 저장한다.
    // 로그인한 회원만 답변을 등록할 수 있다.
    // 공지사항에는 답변을 등록할 수 없다.
    //
    @PostMapping("/write")
    public String write(
            BoardCommentDto commentDto,
            @AuthenticationPrincipal CustomUserDetails loginUser,
            RedirectAttributes redirectAttributes
    ) {

        // 로그인하지 않은 사용자는 로그인 화면으로 이동
        if (loginUser == null) {
            return "redirect:/member/login";
        }

        // Spring Security에 저장된 실제 회원 정보 조회
        MemberDto loginMember = loginUser.getMemberDto();

        // 답변을 작성하려는 원본 게시글 조회
        BoardDto board =
                boardService.findByNo(commentDto.getBoardNo());

        // 존재하지 않는 게시글이면 목록으로 이동
        if (board == null) {
            return "redirect:/board/list";
        }

        // 공지사항에는 답변을 작성할 수 없음
        if ("NOTICE".equals(board.getCategory())) {
            return "redirect:/board/detail/"
                    + commentDto.getBoardNo();
        }

        // 현재 로그인한 회원 번호를 답변 작성자 번호로 설정
        commentDto.setWriterNo(loginMember.getNo());

        try {
            // 답변 내용 검증 후 DB에 저장
            boardCommentService.insert(commentDto);

        } catch (IllegalArgumentException e) {

            // 빈 답변 등의 검증 오류 메시지를 다음 요청에 전달
            redirectAttributes.addFlashAttribute(
                    "commentError",
                    e.getMessage()
            );

            return "redirect:/board/detail/"
                    + commentDto.getBoardNo()
                    + "#comments";
        }

        // 등록 성공 후 답변 목록 아래쪽으로 이동
        return "redirect:/board/detail/"
                + commentDto.getBoardNo()
                + "#comments-bottom";
    }

    // =========================
    // 답변 삭제 처리
    // =========================
    //
    // 주소 접속만으로 삭제되지 않도록 POST 방식으로 처리한다.
    // 답변 작성자 본인만 삭제할 수 있다.
    //
    @PostMapping("/delete/{no}")
    public String delete(
            @PathVariable Long no,
            @AuthenticationPrincipal CustomUserDetails loginUser
    ) {

        // 로그인하지 않은 사용자는 로그인 화면으로 이동
        if (loginUser == null) {
            return "redirect:/member/login";
        }

        MemberDto loginMember = loginUser.getMemberDto();

        // 삭제할 답변 조회
        BoardCommentDto comment =
                boardCommentService.findByNo(no);

        // 존재하지 않는 답변이면 게시글 목록으로 이동
        if (comment == null) {
            return "redirect:/board/list";
        }

        // 답변 작성자 본인만 삭제 가능
        if (!loginMember.getNo().equals(comment.getWriterNo())) {
            return "redirect:/board/detail/"
                    + comment.getBoardNo()
                    + "#comments";
        }

        // 답변 삭제
        boardCommentService.delete(no);

        // 삭제 후 답변 영역으로 이동
        return "redirect:/board/detail/"
                + comment.getBoardNo()
                + "#comments";
    }

    // =========================
    // 답변 수정 처리
    // =========================
    //
    // TOAST UI 수정 에디터에서 전송된 HTML 내용을 저장한다.
    // 답변 작성자 본인만 수정할 수 있다.
    //
    @PostMapping("/update/{no}")
    public String updateProcess(
            @PathVariable Long no,
            BoardCommentDto commentDto,
            @AuthenticationPrincipal CustomUserDetails loginUser,
            RedirectAttributes redirectAttributes
    ) {

        // 로그인하지 않은 사용자는 로그인 화면으로 이동
        if (loginUser == null) {
            return "redirect:/member/login";
        }

        MemberDto loginMember = loginUser.getMemberDto();

        // 수정할 원본 답변 조회
        BoardCommentDto origin =
                boardCommentService.findByNo(no);

        // 존재하지 않는 답변이면 게시글 목록으로 이동
        if (origin == null) {
            return "redirect:/board/list";
        }

        // 답변 작성자 본인만 수정 가능
        if (!loginMember.getNo().equals(origin.getWriterNo())) {
            return "redirect:/board/detail/"
                    + origin.getBoardNo()
                    + "#comments";
        }

        // URL에서 받은 답변 번호를 수정 DTO에 설정
        commentDto.setNo(no);

        try {
            // 답변 내용 검증 후 수정
            boardCommentService.update(commentDto);

        } catch (IllegalArgumentException e) {

            // 빈 답변 등의 검증 오류 메시지를 다음 요청에 전달
            redirectAttributes.addFlashAttribute(
                    "commentError",
                    e.getMessage()
            );

            return "redirect:/board/detail/"
                    + origin.getBoardNo()
                    + "#comments";
        }

        // 수정 성공 후 답변 영역으로 이동
        return "redirect:/board/detail/"
                + origin.getBoardNo()
                + "#comments";
    }
}