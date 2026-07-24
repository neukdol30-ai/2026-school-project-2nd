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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


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

        BoardDto boardDto = new BoardDto();

        // 로그인 회원이면 작성자 번호 저장
        if (loginUser != null) {
            boardDto.setWriterNo(
                    loginUser.getMemberDto().getNo()
            );
        }

        model.addAttribute("boardDto", boardDto);

        return "board/write";
    }

    // 글쓰기 처리
    @PostMapping("/write")
    public String writeProcess(
            @Valid @ModelAttribute("boardDto") BoardDto boardDto,
            BindingResult bindingResult,
            @AuthenticationPrincipal CustomUserDetails loginUser,
            Model model
    ) {

        /*
         * 로그인 회원 정보 가져오기
         *
         * 비로그인 사용자는 loginUser가 null
         */
        MemberDto loginMember = null;

        if (loginUser != null) {
            loginMember = loginUser.getMemberDto();
        }

        /*
         * 공지사항은 관리자만 작성 가능
         */
        if ("NOTICE".equals(boardDto.getCategory())) {

            if (loginMember == null
                    || !"ADMIN".equals(loginMember.getRole())) {

                bindingResult.rejectValue(
                        "category",
                        "forbidden",
                        "공지사항은 관리자만 작성할 수 있습니다."
                );
            }
        }

        /*
         * 비회원은 문의글만 작성 가능
         */
        if (loginMember == null
                && !"QUESTION".equals(boardDto.getCategory())) {

            bindingResult.rejectValue(
                    "category",
                    "forbidden",
                    "비회원은 문의글만 작성할 수 있습니다."
            );
        }

        /*
         * 비회원 입력값 검사
         */
        if (loginMember == null) {

            if (boardDto.getGuestName() == null
                    || boardDto.getGuestName().trim().isEmpty()) {

                bindingResult.rejectValue(
                        "guestName",
                        "required",
                        "작성자 이름을 입력해주세요."
                );
            }

            if (boardDto.getGuestPassword() == null
                    || boardDto.getGuestPassword().trim().isEmpty()) {

                bindingResult.rejectValue(
                        "guestPassword",
                        "required",
                        "비밀번호를 입력해주세요."
                );
            }
        }

        /*
         * Validation 오류가 있으면 글쓰기 화면으로 이동
         */
        if (bindingResult.hasErrors()) {

            model.addAttribute(
                    "isLogin",
                    loginMember != null
            );

            model.addAttribute(
                    "isAdmin",
                    loginMember != null
                            && "ADMIN".equals(loginMember.getRole())
            );

            return "board/write";
        }

        /*
         * 회원 작성
         */
        if (loginMember != null) {

            boardDto.setWriterNo(
                    loginMember.getNo()
            );

            boardDto.setGuestAuthorNo(null);
            boardDto.setGuestName(null);
            boardDto.setGuestPassword(null);
        }

        /*
         * 비회원 작성
         */
        else {

            boardDto.setWriterNo(null);
            boardDto.setGuestAuthorNo(null);

            // guestName과 guestPassword는
            // HTML form에서 전달된 값을 그대로 사용
        }

        try {

            boardService.insert(boardDto);

        } catch (IllegalArgumentException e) {

            model.addAttribute(
                    "contentError",
                    e.getMessage()
            );

            model.addAttribute(
                    "isLogin",
                    loginMember != null
            );

            model.addAttribute(
                    "isAdmin",
                    loginMember != null
                            && "ADMIN".equals(loginMember.getRole())
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
        model.addAttribute(
                "pageDescription",
                "서비스의 주요 공지사항을 확인할 수 있습니다."
        );
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
        model.addAttribute(
                "pageDescription",
                "서비스 이용과 관련된 문의글을 확인할 수 있습니다."
        );
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


    // 회원 수정 GET
    @GetMapping("/update/{no}")
    public String update(
            @PathVariable Long no,
            Model model,
            @AuthenticationPrincipal CustomUserDetails loginUser
    ) {

        if (loginUser == null) {
            return "redirect:/member/login";
        }

        MemberDto loginMember =
                loginUser.getMemberDto();

        // 수정 화면 조회는 조회수 증가 없음
        BoardDto board =
                boardService.findByNo(no);

        if (board == null) {
            return "redirect:/board/list";
        }

        /*
         * 회원 게시글인지 먼저 확인한다.
         * 비회원 글은 writerNo가 null이므로
         * 바로 equals를 실행하지 않는다.
         */
        if (board.getWriterNo() == null) {
            return "redirect:/board/detail/" + no;
        }

        // 작성자만 수정 가능
        if (!loginMember.getNo()
                .equals(board.getWriterNo())) {

            return "redirect:/board/detail/" + no;
        }

        // 기존 update.html에서 사용 중인 이름
        model.addAttribute(
                "board",
                board
        );

        // 회원 수정 화면임을 구분
        model.addAttribute(
                "guestMode",
                false
        );

        // 회원 수정 요청 주소
        model.addAttribute(
                "formAction",
                "/board/update/" + no
        );

        return "board/update";
    }

    // 회원 수정 POST
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

            model.addAttribute(
                    "board",
                    boardDto
            );

            model.addAttribute(
                    "guestMode",
                    false
            );

            model.addAttribute(
                    "formAction",
                    "/board/update/" + no
            );

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
        if (originBoard.getWriterNo() == null
                || !loginMember.getNo().equals(
                originBoard.getWriterNo()
        )) {

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

            model.addAttribute(
                    "board",
                    boardDto
            );

            model.addAttribute(
                    "guestMode",
                    false
            );

            model.addAttribute(
                    "formAction",
                    "/board/update/" + no
            );

            return "board/update";
        }

        return "redirect:/board/detail/" + no;
    }


    // 회원 삭제 POST
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

        // 비회원 글이거나 작성자가 아니면 삭제 불가
        if (board.getWriterNo() == null
                || !loginMember.getNo().equals(
                board.getWriterNo()
        )) {

            return "redirect:/board/detail/" + no;
        }

        boardService.delete(no);

        return "redirect:/board/list";
    }
    //
    // 비회원 게시글
    //
    // 수정 비밀번호 입력
    @GetMapping("/guest/update/{no}")
    public String guestUpdatePasswordForm(
            @PathVariable Long no,
            Model model
    ) {

        BoardDto boardDto = boardService.findByNo(no);

        if (boardDto == null) {
            return "redirect:/board/list";
        }

        if (boardDto.getGuestAuthorNo() == null) {
            return "redirect:/board/detail/" + no;
        }

        model.addAttribute("board", boardDto);

        return "board/guest-update-password";
    }



    // 수정 비밀번호 확인
    @PostMapping("/guest/update/{no}/verify")
    public String verifyGuestUpdatePassword(
            @PathVariable Long no,
            @RequestParam String guestPassword,
            HttpSession session,
            Model model
    ) {

        BoardDto boardDto = boardService.findByNo(no);

        if (boardDto == null) {
            return "redirect:/board/list";
        }

        if (boardDto.getGuestAuthorNo() == null) {
            return "redirect:/board/detail/" + no;
        }

        boolean passwordMatches =
                boardService.checkGuestPassword(
                        no,
                        guestPassword
                );

        if (!passwordMatches) {
            model.addAttribute("board", boardDto);

            model.addAttribute(
                    "passwordError",
                    "비밀번호가 일치하지 않습니다."
            );

            return "board/guest-update-password";
        }

        /*
         * 이 브라우저 세션에서 해당 게시글의
         * 비밀번호 확인을 완료했다는 임시 정보
         */
        session.setAttribute(
                "guestUpdateVerifiedBoardNo",
                no
        );

        return "redirect:/board/guest/update/"
                + no
                + "/form";
    }



    // 수정 화면
    @GetMapping("/guest/update/{no}/form")
    public String guestUpdateForm(
            @PathVariable Long no,
            HttpSession session,
            Model model
    ) {

        // 세션에 저장된 비밀번호 확인 게시글 번호
        Object verifiedValue =
                session.getAttribute(
                        "guestUpdateVerifiedBoardNo"
                );

        /*
         * 비밀번호 확인을 하지 않았거나,
         * 다른 게시글의 비밀번호를 확인한 경우
         */
        if (!(verifiedValue instanceof Long verifiedBoardNo)
                || !verifiedBoardNo.equals(no)) {

            return "redirect:/board/guest/update/" + no;
        }

        BoardDto boardDto =
                boardService.findByNo(no);

        if (boardDto == null) {
            return "redirect:/board/list";
        }

        // 회원 게시글은 비회원 수정 기능으로 접근할 수 없음
        if (boardDto.getGuestAuthorNo() == null) {
            return "redirect:/board/detail/" + no;
        }

        model.addAttribute(
                "board",
                boardDto
        );

        /*
         * 기존 update.html에서 회원 수정인지
         * 비회원 수정인지 구분하기 위한 값
         */
        model.addAttribute(
                "guestMode",
                true
        );

        model.addAttribute(
                "formAction",
                "/board/guest/update/" + no
        );

        return "board/update";
    }


    // 실제 수정
    @PostMapping("/guest/update/{no}")
    public String guestUpdateProcess(
            @PathVariable Long no,
            @Valid @ModelAttribute("board") BoardDto boardDto,
            BindingResult bindingResult,
            HttpSession session,
            Model model
    ) {

        Object verifiedValue =
                session.getAttribute(
                        "guestUpdateVerifiedBoardNo"
                );

        /*
         * 비밀번호 확인을 하지 않았거나
         * 다른 게시글 번호로 수정 요청한 경우
         */
        if (!(verifiedValue instanceof Long verifiedBoardNo)
                || !verifiedBoardNo.equals(no)) {

            return "redirect:/board/guest/update/" + no;
        }

        BoardDto originBoard =
                boardService.findByNo(no);

        if (originBoard == null) {
            return "redirect:/board/list";
        }

        // 비회원 게시글이 아닌 경우
        if (originBoard.getGuestAuthorNo() == null) {
            return "redirect:/board/detail/" + no;
        }

        boardDto.setNo(no);

        /*
         * 비회원은 문의 게시판만 사용 가능하므로
         * category를 서버에서 강제로 지정
         */
        boardDto.setCategory("QUESTION");

        if (bindingResult.hasErrors()) {

            model.addAttribute(
                    "board",
                    boardDto
            );

            model.addAttribute(
                    "guestMode",
                    true
            );

            model.addAttribute(
                    "formAction",
                    "/board/guest/update/" + no
            );

            return "board/update";
        }

        try {

            boardService.updateGuestBoard(
                    boardDto
            );

        } catch (IllegalArgumentException e) {

            model.addAttribute(
                    "contentError",
                    e.getMessage()
            );

            model.addAttribute(
                    "board",
                    boardDto
            );

            model.addAttribute(
                    "guestMode",
                    true
            );

            model.addAttribute(
                    "formAction",
                    "/board/guest/update/" + no
            );

            return "board/update";
        }

        /*
         * 수정 완료 후 비밀번호 인증 정보 제거
         */
        session.removeAttribute(
                "guestUpdateVerifiedBoardNo"
        );

        return "redirect:/board/detail/" + no;
    }

    // 삭제 비밀번호 입력
    @GetMapping("/guest/delete/{no}")
    public String guestDeletePasswordForm(
            @PathVariable Long no,
            Model model
    ) {

        BoardDto boardDto =
                boardService.findByNo(no);

        if (boardDto == null) {
            return "redirect:/board/list";
        }

        // 회원 게시글은 비회원 삭제 기능 사용 불가
        if (boardDto.getGuestAuthorNo() == null) {
            return "redirect:/board/detail/" + no;
        }

        model.addAttribute(
                "board",
                boardDto
        );

        return "board/guest-delete-password";

    }

    //삭제 비밀번호 확인 POST
    @PostMapping("/guest/delete/{no}/verify")
    public String verifyGuestDeletePassword(
            @PathVariable Long no,
            @RequestParam String guestPassword,
            HttpSession session,
            Model model
    ) {

        BoardDto boardDto =
                boardService.findByNo(no);

        if (boardDto == null) {
            return "redirect:/board/list";
        }

        if (boardDto.getGuestAuthorNo() == null) {
            return "redirect:/board/detail/" + no;
        }

        boolean passwordMatches =
                boardService.checkGuestPassword(
                        no,
                        guestPassword
                );

        if (!passwordMatches) {

            model.addAttribute(
                    "board",
                    boardDto
            );

            model.addAttribute(
                    "passwordError",
                    "비밀번호가 일치하지 않습니다."
            );

            return "board/guest-delete-password";
        }

        session.setAttribute(
                "guestDeleteVerifiedBoardNo",
                no
        );

        return "redirect:/board/guest/delete/"
                + no
                + "/confirm";
    }


    //삭제 확인 화면
    @GetMapping("/guest/delete/{no}/confirm")
    public String guestDeleteConfirm(
            @PathVariable Long no,
            HttpSession session,
            Model model
    ) {

        Object verifiedValue =
                session.getAttribute(
                        "guestDeleteVerifiedBoardNo"
                );

        if (!(verifiedValue instanceof Long verifiedBoardNo)
                || !verifiedBoardNo.equals(no)) {

            return "redirect:/board/guest/delete/" + no;
        }

        BoardDto boardDto =
                boardService.findByNo(no);

        if (boardDto == null) {
            session.removeAttribute(
                    "guestDeleteVerifiedBoardNo"
            );

            return "redirect:/board/list";
        }

        if (boardDto.getGuestAuthorNo() == null) {
            session.removeAttribute(
                    "guestDeleteVerifiedBoardNo"
            );

            return "redirect:/board/detail/" + no;
        }

        model.addAttribute(
                "board",
                boardDto
        );

        return "board/guest-delete-confirm";
    }


    // 비회원 게시글 실제 삭제
    @PostMapping("/guest/delete/{no}")
    public String deleteGuestBoard(
            @PathVariable Long no,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {

        Object verifiedValue =
                session.getAttribute("guestDeleteVerifiedBoardNo");

        if (!(verifiedValue instanceof Long verifiedBoardNo)
                || !verifiedBoardNo.equals(no)) {

            return "redirect:/board/guest/delete/" + no;
        }

        boardService.delete(no);

        session.removeAttribute("guestDeleteVerifiedBoardNo");

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "게시글이 삭제되었습니다."
        );

        return "redirect:/board/list";
    }

}
