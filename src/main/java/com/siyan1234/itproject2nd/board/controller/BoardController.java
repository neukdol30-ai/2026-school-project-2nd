package com.siyan1234.itproject2nd.board.controller;

import com.siyan1234.itproject2nd.board.dto.BoardDto;
import com.siyan1234.itproject2nd.board.service.BoardService;

import com.siyan1234.itproject2nd.member.dto.MemberDto;
import jakarta.servlet.http.HttpSession;

import com.siyan1234.itproject2nd.member.dto.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import com.siyan1234.itproject2nd.board.service.BoardCommentService;

@Controller
@RequiredArgsConstructor
@RequestMapping("/board")
public class BoardController {

    private final BoardService boardService;
    private final BoardCommentService boardCommentService;

    // 게시글 목록(페이징)
    @GetMapping("/list")
    public String list(@RequestParam(defaultValue = "1") int page,
                       Model model) {

        int pageSize = 10;
        int totalCount = boardService.countAll();
        int totalPage = (int) Math.ceil((double) totalCount / pageSize);

        model.addAttribute("boardList", boardService.findPage(page));
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPage", totalPage);
        model.addAttribute("pageTitle", "전체 게시글");

        // 페이지 번호 클릭 시 이동할 기본 주소
        model.addAttribute("pageUrl", "/board/list");

        return "board/list";
    }

    // 게시글 상세
    @GetMapping("/detail/{no}")
    public String detail(@PathVariable Long no,
                         Model model,
                         HttpSession session) {

        String viewKey = "viewed_board_" + no;

        if (session.getAttribute(viewKey) == null) {
            boardService.increaseViewCount(no);
            session.setAttribute(viewKey, true);
        }

        model.addAttribute("board", boardService.findByNo(no));

        model.addAttribute("commentList",
                boardCommentService.findByBoardNo(no));

        return "board/detail";
    }

    // 글쓰기 화면
    @GetMapping("/write")
    public String write(
            @AuthenticationPrincipal CustomUserDetails loginUser,
            Model model
    ) {
        if (loginUser == null) {
            return "redirect:/member/login";
        }

        model.addAttribute("boardDto", new BoardDto());

        return "board/write";
    }

    // 글쓰기 처리
    @PostMapping("/write")
    public String writeProcess(
            @Valid BoardDto boardDto,
            BindingResult bindingResult,
            @AuthenticationPrincipal CustomUserDetails loginUser,
            Model model
    ) {

        if (bindingResult.hasErrors()) {
            return "board/write";
        }

        // Spring Security 로그인 여부 확인
        if (loginUser == null) {
            return "redirect:/member/login";
        }

        // CustomUserDetails 안에서 실제 회원 정보 꺼내기
        MemberDto loginMember = loginUser.getMemberDto();

        // 일반 회원은 공지사항 작성 불가
        if ("NOTICE".equals(boardDto.getCategory())
                && !"ADMIN".equals(loginMember.getRole())) {

            return "redirect:/board/write";
        }

        // 게시글 작성자 번호 저장
        boardDto.setWriterNo(loginMember.getNo());

        try {
            boardService.insert(boardDto);
        } catch (IllegalArgumentException e) {

            model.addAttribute(
                    "contentError",
                    e.getMessage()
            );

            return "board/write";
        }

        return "redirect:/board/list";
    }

    // 공지사항 목록
    @GetMapping("/notice")
    public String notice(
            @RequestParam(defaultValue = "1") int page,
            Model model
    ) {

        int pageSize = 10;

        int totalCount =
                boardService.countByCategory("NOTICE");

        int totalPage =
                (int) Math.ceil((double) totalCount / pageSize);

        model.addAttribute(
                "boardList",
                boardService.findPageByCategory(page, "NOTICE")
        );

        model.addAttribute("pageTitle", "공지사항");
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPage", totalPage);

        // 공지사항 페이지 번호의 이동 주소
        model.addAttribute("pageUrl", "/board/notice");

        return "board/list";
    }

    // 문의 게시판 목록
    @GetMapping("/question")
    public String question(
            @RequestParam(defaultValue = "1") int page,
            Model model
    ) {
        int pageSize = 10;

        int totalCount =
                boardService.countByCategory("QUESTION");

        int totalPage =
                (int) Math.ceil((double) totalCount / pageSize);

        model.addAttribute(
                "boardList",
                boardService.findPageByCategory(page, "QUESTION")
        );

        model.addAttribute("pageTitle", "문의 게시판");
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPage", totalPage);
        model.addAttribute("pageUrl", "/board/question");

        return "board/list";
    }



    // 게시글 검색
    @GetMapping("/search")
    public String search(@RequestParam String keyword,
                         Model model) {

        model.addAttribute("boardList", boardService.search(keyword));
        model.addAttribute("pageTitle", "검색 결과");
        model.addAttribute("keyword", keyword);

        // list.html의 페이징 부분에서 필요
        model.addAttribute("currentPage", 1);
        model.addAttribute("totalPage", 1);



        return "board/list";
    }


    // 수정 화면
    @GetMapping("/update/{no}")
    public String update(
            @PathVariable Long no,
            Model model,
            @AuthenticationPrincipal CustomUserDetails loginUser
    ) {

        if (loginUser == null) {
            return "redirect:/member/login";
        }

        MemberDto loginMember = loginUser.getMemberDto();

        // 수정 화면 조회는 조회수 증가 없음
        BoardDto board = boardService.findByNo(no);

        if (board == null) {
            return "redirect:/board/list";
        }

        // 작성자만 수정 가능
        if (!loginMember.getNo().equals(board.getWriterNo())) {
            return "redirect:/board/detail/" + no;
        }

        model.addAttribute("board", board);

        return "board/update";
    }

    // 수정 처리
    @PostMapping("/update/{no}")
    public String updateProcess(
            @PathVariable Long no,
            @Valid @ModelAttribute("board") BoardDto boardDto,
            BindingResult bindingResult,
            @AuthenticationPrincipal CustomUserDetails loginUser,
            Model model
    ) {

        boardDto.setNo(no);

        if (bindingResult.hasErrors()) {
            boardDto.setNo(no);
            return "board/update";
        }

        if (loginUser == null) {
            return "redirect:/member/login";
        }

        MemberDto loginMember = loginUser.getMemberDto();

        BoardDto originBoard = boardService.findByNo(no);

        if (originBoard == null) {
            return "redirect:/board/list";
        }

        // 작성자만 수정 가능
        if (!loginMember.getNo().equals(originBoard.getWriterNo())) {
            return "redirect:/board/detail/" + no;
        }

        // 일반 회원은 공지사항으로 변경 불가
        if ("NOTICE".equals(boardDto.getCategory())
                && !"ADMIN".equals(loginMember.getRole())) {

            return "redirect:/board/detail/" + no;
        }


        boardDto.setWriterNo(loginMember.getNo());

        try {
            boardService.update(boardDto);
        } catch (IllegalArgumentException e) {

            model.addAttribute(
                    "contentError",
                    e.getMessage()
            );

            model.addAttribute("board", boardDto);

            return "board/update";
        }

        return "redirect:/board/detail/" + no;
    }


    // 게시글 삭제 처리
    // GET이 아니라 POST로 처리하여
    // 주소 접속만으로 삭제되는 문제를 방지
    @PostMapping("/delete/{no}")
    public String delete(
            @PathVariable Long no,
            @AuthenticationPrincipal CustomUserDetails loginUser
    ) {

        if (loginUser == null) {
            return "redirect:/member/login";
        }

        MemberDto loginMember = loginUser.getMemberDto();

        BoardDto board = boardService.findByNo(no);

        if (board == null) {
            return "redirect:/board/list";
        }

        // 작성자만 삭제 가능
        if (!loginMember.getNo().equals(board.getWriterNo())) {
            return "redirect:/board/detail/" + no;
        }

        boardService.delete(no);

        return "redirect:/board/list";
    }
}
