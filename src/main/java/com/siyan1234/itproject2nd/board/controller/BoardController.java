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


    //게시글상세
    @GetMapping("/detail/{no}")
    public String detail(
            @PathVariable Long no,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {

        /*
         * 게시글을 먼저 조회한다.
         * 삭제됐거나 존재하지 않는 게시글이면 null이 반환된다.
         */
        BoardDto board = boardService.findByNo(no);

        if (board == null) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "존재하지 않거나 삭제된 게시글입니다."
            );

            return "redirect:/board/question";
        }

        /*
         * 존재하는 게시글일 때만 조회수를 증가시킨다.
         */
        String viewKey = "viewed_board_" + no;

        if (session.getAttribute(viewKey) == null) {

            boardService.increaseViewCount(no);

            session.setAttribute(
                    viewKey,
                    true
            );
        }

        model.addAttribute(
                "board",
                board
        );

        model.addAttribute(
                "commentList",
                boardCommentService.findByBoardNo(no)
        );

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

            // 비회원 이름 검사
            if (boardDto.getGuestName() == null
                    || boardDto.getGuestName().isBlank()) {

                bindingResult.rejectValue(
                        "guestName",
                        "required",
                        "작성자 이름을 입력해주세요."
                );
            }

            String guestPassword =
                    boardDto.getGuestPassword();

            // 비밀번호 입력 여부 검사
            if (guestPassword == null
                    || guestPassword.isBlank()) {

                bindingResult.rejectValue(
                        "guestPassword",
                        "required",
                        "비밀번호를 입력해주세요."
                );
            }

            // 비밀번호 길이 검사
            else if (guestPassword.length() < 4
                    || guestPassword.length() > 20) {

                bindingResult.rejectValue(
                        "guestPassword",
                        "size",
                        "비밀번호는 4자 이상 20자 이하로 입력해주세요."
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

        /*
         * 작성한 게시글 종류에 맞는 목록 주소로 이동한다.
         * 이동한 주소의 Controller 메서드가
         * 최종적으로 board/list.html을 보여준다.
         */

        if ("NOTICE".equals(boardDto.getCategory())) {
            return "redirect:/board/notice";
        }

        return "redirect:/board/question";
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

        // 검색할 게시판 종류
        model.addAttribute("category", "NOTICE");


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

        // 검색할 게시판 종류
        model.addAttribute("category", "QUESTION");

        return "board/list";
    }



    // 게시글 검색 결과 페이징
    @GetMapping("/search")
    public String search(
            @RequestParam(
                    name = "keyword",
                    defaultValue = ""
            ) String keyword,

            @RequestParam(
                    name = "category",
                    required = false
            ) String category,

            @RequestParam(
                    name = "page",
                    defaultValue = "1"
            ) int page,

            Model model
    ) {

        /*
         * 검색어와 카테고리 앞뒤 공백 제거
         */
        String cleanKeyword =
                keyword.trim();

        String cleanCategory =
                category == null
                        ? ""
                        : category.trim();

        /*
         * 검색어가 비어 있다면
         * 검색하지 않고 원래 게시판으로 이동
         */
        if (cleanKeyword.isBlank()) {

            if ("NOTICE".equals(cleanCategory)) {
                return "redirect:/board/notice";
            }

            if ("QUESTION".equals(cleanCategory)) {
                return "redirect:/board/question";
            }

            return "redirect:/board/list";
        }

        /*
         * 전체 검색 결과 개수를 먼저 조회한다.
         */
        int totalCount;

        if (!cleanCategory.isBlank()) {

            totalCount =
                    boardService.countSearchByCategory(
                            cleanKeyword,
                            cleanCategory
                    );

        } else {

            totalCount =
                    boardService.countSearch(
                            cleanKeyword
                    );
        }

        /*
         * 전체 페이지 수 계산
         */
        int totalPage =
                boardService.calculateTotalPage(
                        totalCount
                );

        /*
         * 잘못된 페이지 번호 보정
         *
         * page가 0 이하이면 1페이지,
         * 전체 페이지보다 크면 마지막 페이지
         */
        int currentPage =
                Math.max(page, 1);

        currentPage =
                Math.min(
                        currentPage,
                        totalPage
                );

        /*
         * 현재 페이지에 표시할 검색 결과 조회
         */
        if (!cleanCategory.isBlank()) {

            model.addAttribute(
                    "boardList",
                    boardService.searchByCategory(
                            cleanKeyword,
                            cleanCategory,
                            currentPage
                    )
            );

        } else {

            model.addAttribute(
                    "boardList",
                    boardService.search(
                            cleanKeyword,
                            currentPage
                    )
            );
        }

        /*
         * 검색한 게시판에 맞는 제목과
         * 카테고리 메뉴 활성화 주소 설정
         */
        if ("NOTICE".equals(cleanCategory)) {

            model.addAttribute(
                    "pageTitle",
                    "공지사항 검색 결과"
            );

            model.addAttribute(
                    "pageDescription",
                    "공지사항에서 검색한 결과입니다."
            );

            /*
             * 카테고리 메뉴에서
             * 공지사항을 활성화하기 위한 값
             */
            model.addAttribute(
                    "pageUrl",
                    "/board/notice"
            );

        } else if ("QUESTION".equals(cleanCategory)) {

            model.addAttribute(
                    "pageTitle",
                    "문의 게시판 검색 결과"
            );

            model.addAttribute(
                    "pageDescription",
                    "문의 게시판에서 검색한 결과입니다."
            );

            /*
             * 카테고리 메뉴에서
             * 문의 게시판을 활성화하기 위한 값
             */
            model.addAttribute(
                    "pageUrl",
                    "/board/question"
            );

        } else {

            model.addAttribute(
                    "pageTitle",
                    "전체 검색 결과"
            );

            model.addAttribute(
                    "pageDescription",
                    "전체 게시글에서 검색한 결과입니다."
            );

            model.addAttribute(
                    "pageUrl",
                    "/board/list"
            );
        }

        /*
         * 검색 조건과 페이징 정보를 화면으로 전달
         */
        model.addAttribute(
                "keyword",
                cleanKeyword
        );

        model.addAttribute(
                "category",
                cleanCategory
        );

        model.addAttribute(
                "currentPage",
                currentPage
        );

        model.addAttribute(
                "totalPage",
                totalPage
        );

        model.addAttribute(
                "totalCount",
                totalCount
        );

        /*
         * 일반 목록과 검색 결과의
         * 페이지 링크를 구분하기 위한 값
         */
        model.addAttribute(
                "searchMode",
                true
        );

        return "board/list";
    }


    // 회원 수정 GET
    @GetMapping("/update/{no}")
    public String update(
            @PathVariable Long no,
            @RequestParam(required = false) String category,
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

            if ("NOTICE".equals(category)) {
                return "redirect:/board/notice";
            }

            if ("QUESTION".equals(category)) {
                return "redirect:/board/question";
            }

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
            return "redirect:/board/question";
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
            @RequestParam(required = false) String category,
            @AuthenticationPrincipal CustomUserDetails loginUser,
            RedirectAttributes redirectAttributes
    ) {

        // 1. 로그인 여부 확인
        if (loginUser == null) {
            return "redirect:/member/login";
        }

        MemberDto loginMember = loginUser.getMemberDto();

        // 2. 게시글 조회
        BoardDto board = boardService.findByNo(no);

        /*
         * 3. 이미 삭제된 게시글인 경우
         *
         * DB에서는 카테고리를 확인할 수 없으므로
         * detail.html에서 함께 전송한 category 값을 사용한다.
         */
        if (board == null) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "이미 삭제되었거나 존재하지 않는 게시글입니다."
            );

            if ("NOTICE".equals(category)) {
                return "redirect:/board/notice";
            }

            return "redirect:/board/question";
        }

        // 4. 회원 게시글인지 및 작성자인지 확인
        if (board.getWriterNo() == null
                || !loginMember.getNo().equals(board.getWriterNo())) {

            return "redirect:/board/detail/" + no;
        }

        /*
         * 5. 실제 DB에 있는 카테고리 저장
         *
         * 게시글이 존재할 때는 사용자가 전송한 category보다
         * DB에서 조회한 category를 사용하는 것이 안전하다.
         */
        String savedCategory = board.getCategory();

        // 6. 게시글 삭제
        boardService.delete(no);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "게시글이 삭제되었습니다."
        );

        // 7. 원래 게시판으로 이동
        if ("NOTICE".equals(savedCategory)) {
            return "redirect:/board/notice";
        }

        return "redirect:/board/question";
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
            return "redirect:/board/question";
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
            return "redirect:/board/question";
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
            return "redirect:/board/question";
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
            return "redirect:/board/question";
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
            return "redirect:/board/question";
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
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        BoardDto boardDto =
                boardService.findByNo(no);

        if (boardDto == null) {

            session.removeAttribute(
                    "guestDeleteVerifiedBoardNo"
            );

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "이미 삭제되었거나 존재하지 않는 게시글입니다."
            );

            return "redirect:/board/question";
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

            return "redirect:/board/question";
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

        /*
         * 게시글이 이미 삭제됐는지 먼저 확인
         */
        BoardDto boardDto = boardService.findByNo(no);

        if (boardDto == null) {

            session.removeAttribute(
                    "guestDeleteVerifiedBoardNo"
            );

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "이미 삭제되었거나 존재하지 않는 게시글입니다."
            );

            return "redirect:/board/question";
        }

        /*
         * 비밀번호 확인 세션 검사
         */
        Object verifiedValue =
                session.getAttribute(
                        "guestDeleteVerifiedBoardNo"
                );

        if (!(verifiedValue instanceof Long verifiedBoardNo)
                || !verifiedBoardNo.equals(no)) {

            return "redirect:/board/guest/delete/" + no;
        }

        /*
         * 게시글 삭제
         */
        boardService.delete(no);

        session.removeAttribute(
                "guestDeleteVerifiedBoardNo"
        );

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "게시글이 삭제되었습니다."
        );

        return "redirect:/board/question";
    }

}
