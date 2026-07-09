package com.siyan1234.itproject2nd.board.controller;

import com.siyan1234.itproject2nd.board.dto.BoardCommentDto;
import com.siyan1234.itproject2nd.board.service.BoardCommentService;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.siyan1234.itproject2nd.board.service.BoardService;
import com.siyan1234.itproject2nd.board.dto.BoardDto;

@Controller
@RequiredArgsConstructor
@RequestMapping("/board/comment")
public class BoardCommentController {

    private final BoardCommentService boardCommentService;
    private final BoardService boardService;

    // 댓글 등록
    @PostMapping("/write")
    public String write(BoardCommentDto commentDto,
                        HttpSession session) {

        MemberDto loginMember =
                (MemberDto) session.getAttribute("loginMember");

        if (loginMember == null) {
            return "redirect:/member/login";
        }

        BoardDto board = boardService.findByNo(commentDto.getBoardNo());

        if ("NOTICE".equals(board.getCategory())) {
            return "redirect:/board/detail/" + commentDto.getBoardNo();
        }

        commentDto.setWriterNo(loginMember.getNo());

        boardCommentService.insert(commentDto);

        return "redirect:/board/detail/" + commentDto.getBoardNo();
    }

    // 댓글 삭제
    @GetMapping("/delete/{no}")
    public String delete(@PathVariable Long no,
                         HttpSession session) {

        MemberDto loginMember =
                (MemberDto) session.getAttribute("loginMember");

        if (loginMember == null) {
            return "redirect:/member/login";
        }

        BoardCommentDto comment = boardCommentService.findByNo(no);

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
    public String updateProcess(@PathVariable Long no,
                                BoardCommentDto commentDto,
                                HttpSession session) {

        MemberDto loginMember =
                (MemberDto) session.getAttribute("loginMember");

        if (loginMember == null) {
            return "redirect:/member/login";
        }

        BoardCommentDto origin =
                boardCommentService.findByNo(no);

        if (!loginMember.getNo().equals(origin.getWriterNo())) {
            return "redirect:/board/detail/" + origin.getBoardNo();
        }

        commentDto.setNo(no);

        boardCommentService.update(commentDto);

        return "redirect:/board/detail/" + origin.getBoardNo();
    }

}
