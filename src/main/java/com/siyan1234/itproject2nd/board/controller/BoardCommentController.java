package com.siyan1234.itproject2nd.board.controller;

import com.siyan1234.itproject2nd.board.dto.BoardCommentDto;
import com.siyan1234.itproject2nd.board.service.BoardCommentService;
import com.siyan1234.itproject2nd.member.dto.MemberDto;

import com.siyan1234.itproject2nd.member.dto.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;


import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import org.springframework.web.bind.annotation.*;
import com.siyan1234.itproject2nd.board.service.BoardService;
import com.siyan1234.itproject2nd.board.dto.BoardDto;

@Controller
@RequiredArgsConstructor
@RequestMapping("/board/comment")
public class BoardCommentController {

    private final BoardCommentService boardCommentService;
    private final BoardService boardService;

    // =========================
    // 댓글 등록
    // =========================
    @PostMapping("/write")
    public String write(
            BoardCommentDto commentDto,
            @AuthenticationPrincipal CustomUserDetails loginUser,
            RedirectAttributes redirectAttributes
    ) {

        // 로그인하지 않은 경우
        if (loginUser == null) {
            return "redirect:/member/login";
        }

        // Spring Security에 저장된 회원 정보 꺼내기
        MemberDto loginMember = loginUser.getMemberDto();

        // 댓글을 작성하려는 게시글 조회
        BoardDto board =
                boardService.findByNo(commentDto.getBoardNo());

        // 존재하지 않는 게시글
        if (board == null) {
            return "redirect:/board/list";
        }

        // 공지사항에는 댓글 작성 불가
        if ("NOTICE".equals(board.getCategory())) {
            return "redirect:/board/detail/" + commentDto.getBoardNo();
        }

        // 로그인한 회원 번호를 댓글 작성자 번호로 저장
        commentDto.setWriterNo(loginMember.getNo());

        try {
            boardCommentService.insert(commentDto);
        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "commentError",
                    e.getMessage()
            );

            return "redirect:/board/detail/" + commentDto.getBoardNo();
        }

        return "redirect:/board/detail/" + commentDto.getBoardNo();
    }

    // =========================
// 댓글 삭제 처리
// =========================
//
// 댓글 삭제도 POST 방식으로 처리
//
    @PostMapping("/delete/{no}")
    public String delete(
            @PathVariable Long no,
            @AuthenticationPrincipal CustomUserDetails loginUser
    ) {

        if (loginUser == null) {
            return "redirect:/member/login";
        }

        MemberDto loginMember = loginUser.getMemberDto();

        BoardCommentDto comment =
                boardCommentService.findByNo(no);

        if (comment == null) {
            return "redirect:/board/list";
        }

        // 댓글 작성자만 삭제 가능
        if (!loginMember.getNo().equals(comment.getWriterNo())) {
            return "redirect:/board/detail/" + comment.getBoardNo();
        }

        boardCommentService.delete(no);

        return "redirect:/board/detail/" + comment.getBoardNo();
    }

    // =========================
    // 댓글 수정 처리
    // =========================
    @PostMapping("/update/{no}")
    public String updateProcess(
            @PathVariable Long no,
            BoardCommentDto commentDto,
            @AuthenticationPrincipal CustomUserDetails loginUser,
            RedirectAttributes redirectAttributes
    ) {

        // 로그인하지 않은 경우
        if (loginUser == null) {
            return "redirect:/member/login";
        }

        MemberDto loginMember = loginUser.getMemberDto();

        // 수정 전 원본 댓글 조회
        BoardCommentDto origin =
                boardCommentService.findByNo(no);

        // 존재하지 않는 댓글
        if (origin == null) {
            return "redirect:/board/list";
        }

        // 댓글 작성자만 수정 가능
        if (!loginMember.getNo().equals(origin.getWriterNo())) {
            return "redirect:/board/detail/" + origin.getBoardNo();
        }

        // 수정할 댓글 번호 설정
        commentDto.setNo(no);

        try {
            boardCommentService.update(commentDto);
        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "commentError",
                    e.getMessage()
            );

            return "redirect:/board/detail/" + origin.getBoardNo();
        }

        return "redirect:/board/detail/" + origin.getBoardNo();
    }
}
