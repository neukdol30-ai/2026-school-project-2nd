package com.siyan1234.itproject2nd.board.controller;

import com.siyan1234.itproject2nd.board.dto.BoardDto;
import com.siyan1234.itproject2nd.board.service.BoardService;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import jakarta.servlet.http.HttpSession;
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
    // =========================
    // 게시글 목록(페이징)
    // =========================
    //
    // page 파라미터를 받아
    // 해당 페이지의 게시글 목록을 조회
    //
    // 전체 게시글 개수로
    // 전체 페이지 수도 계산
    //
    // 예)
    // /board/list?page=1
    // /board/list?page=2
    //
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
    public String write(HttpSession session, Model model) {
        if (session.getAttribute("loginMember") == null) {
            return "redirect:/member/login";
        }

        model.addAttribute("boardDto", new BoardDto());

        return "board/write";
    }

    // 글쓰기 처리
    @PostMapping("/write")
    public String writeProcess(@Valid BoardDto boardDto,
                               BindingResult bindingResult,
                               HttpSession session) {

        if (bindingResult.hasErrors()) {
            return "board/write";
        }

        MemberDto loginMember =
                (MemberDto) session.getAttribute("loginMember");

        // 일반회원은 공지사항 작성 불가
        if ("NOTICE".equals(boardDto.getCategory())
                && !"ADMIN".equals(loginMember.getRole())) {

            return "redirect:/board/write";

        }

        if (loginMember == null) {
            return "redirect:/member/login";
        }

        boardDto.setWriterNo(loginMember.getNo());

        boardService.insert(boardDto);

        return "redirect:/board/list";
    }

    @GetMapping("/notice")
    public String notice(Model model) {
        model.addAttribute("boardList", boardService.findByCategory("NOTICE"));
        model.addAttribute("pageTitle", "공지사항");

        model.addAttribute("currentPage", 1);
        model.addAttribute("totalPage", 1);

        return "board/list";
    }

    @GetMapping("/free")
    public String free(Model model) {
        model.addAttribute("boardList", boardService.findByCategory("FREE"));
        model.addAttribute("pageTitle", "자유게시판");

        model.addAttribute("currentPage", 1);
        model.addAttribute("totalPage", 1);

        return "board/list";
    }



    // 게시글 검색
    // 사용자가 입력한 검색어를 받아
    // 제목, 내용, 작성자를 검색하여
    // 검색 결과를 게시글 목록 화면에 출력
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
    public String update(@PathVariable Long no,
                         Model model,
                         HttpSession session) {

        MemberDto loginMember =
                (MemberDto) session.getAttribute("loginMember");

        if (loginMember == null) {
            return "redirect:/member/login";
        }
        // 수정 화면은 조회수 증가 X
        BoardDto board = boardService.findByNo(no);

        if (!loginMember.getNo().equals(board.getWriterNo())) {
            return "redirect:/board/detail/" + no;
        }

        model.addAttribute("board", board);

        return "board/update";
    }

    // 수정 처리
    @PostMapping("/update/{no}")
    public String updateProcess(@PathVariable Long no,
                                @Valid BoardDto boardDto,
                                BindingResult bindingResult,
                                HttpSession session) {

        if (bindingResult.hasErrors()) {
            return "board/update";
        }

        MemberDto loginMember =
                (MemberDto) session.getAttribute("loginMember");

        if (loginMember == null) {
            return "redirect:/member/login";
        }

        BoardDto originBoard = boardService.findByNo(no);

        //url직접조작해서 공지사항적는것 막는용
        if ("NOTICE".equals(boardDto.getCategory())
                && !"ADMIN".equals(loginMember.getRole())) {

            return "redirect:/board/detail/" + no;

        }

        if (!loginMember.getNo().equals(originBoard.getWriterNo())) {
            return "redirect:/board/detail/" + no;
        }

        boardDto.setNo(no);
        boardDto.setWriterNo(loginMember.getNo());

        boardService.update(boardDto);

        return "redirect:/board/detail/" + no;
    }

    // 삭제
    @GetMapping("/delete/{no}")
    public String delete(@PathVariable Long no,
                         HttpSession session) {

        MemberDto loginMember =
                (MemberDto) session.getAttribute("loginMember");

        if (loginMember == null) {
            return "redirect:/member/login";
        }

        BoardDto board = boardService.findByNo(no);

        if (!loginMember.getNo().equals(board.getWriterNo())) {
            return "redirect:/board/detail/" + no;
        }

        boardService.delete(no);

        return "redirect:/board/list";
    }
}
