package com.siyan1234.itproject2nd.admin.controller;

import com.siyan1234.itproject2nd.admin.dto.AdminBoardDto;
import com.siyan1234.itproject2nd.admin.dto.AdminDashboardDto;
import com.siyan1234.itproject2nd.admin.dto.AdminVisitSummaryDto;
import com.siyan1234.itproject2nd.admin.dto.RecentChatRoomDto;
import com.siyan1234.itproject2nd.admin.service.AdminBoardService;
import com.siyan1234.itproject2nd.admin.service.AdminChatService;
import com.siyan1234.itproject2nd.admin.service.AdminDashboardService;
import com.siyan1234.itproject2nd.admin.service.AdminMemberService;
import com.siyan1234.itproject2nd.admin.service.AdminVisitService;
import com.siyan1234.itproject2nd.admin.support.AdminPagingHelper;
import com.siyan1234.itproject2nd.admin.support.AdminView;
import com.siyan1234.itproject2nd.board.dto.BoardCommentDto;
import com.siyan1234.itproject2nd.board.service.BoardCommentService;
import com.siyan1234.itproject2nd.chat.dto.ChatRoomDto;
import com.siyan1234.itproject2nd.config.security.LoginMemberResolver;
import com.siyan1234.itproject2nd.member.dto.CustomUserDetails;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 관리자 단일 콘솔 화면 Controller입니다.
 *
 * 이 Controller는 /admin 하나의 화면 안에서 필요한 Model을 조립합니다.
 * 상세/수정 화면도 별도 관리자 페이지로 분리하지 않고 좌측 사이드바가 유지되는 콘솔 안에서 표시합니다.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private static final int DEFAULT_MEMBER_SIZE = 10;
    private static final int DEFAULT_CHAT_SIZE = 10;
    private static final int DEFAULT_BOARD_SIZE = 10;
    private static final int DEFAULT_VISIT_SIZE = 10;

    private final AdminDashboardService adminDashboardService;
    private final AdminMemberService adminMemberService;
    private final AdminChatService adminChatService;
    private final AdminBoardService adminBoardService;
    private final AdminVisitService adminVisitService;
    private final BoardCommentService boardCommentService;
    private final LoginMemberResolver loginMemberResolver;

    @GetMapping({"", "/", "/dashboard"})
    public String adminDashboard(
            @RequestParam(value = "view", defaultValue = "dashboard") String view,

            @RequestParam(value = "memberKeyword", required = false) String memberKeyword,
            @RequestParam(value = "memberPage", defaultValue = "1") int memberPage,
            @RequestParam(value = "memberSize", defaultValue = "10") int memberSize,
            @RequestParam(value = "editMemberNo", required = false) Integer editMemberNo,

            @RequestParam(value = "chatStatus", required = false) String chatStatus,
            @RequestParam(value = "chatCategory", required = false) String chatCategory,
            @RequestParam(value = "chatKeyword", required = false) String chatKeyword,
            @RequestParam(value = "chatPage", defaultValue = "1") int chatPage,
            @RequestParam(value = "chatSize", defaultValue = "10") int chatSize,
            @RequestParam(value = "roomNo", required = false) Integer roomNo,

            @RequestParam(value = "boardCategory", required = false) String boardCategory,
            @RequestParam(value = "boardKeyword", required = false) String boardKeyword,
            @RequestParam(value = "boardPage", defaultValue = "1") int boardPage,
            @RequestParam(value = "boardSize", defaultValue = "10") int boardSize,
            @RequestParam(value = "boardNo", required = false) Long boardNo,

            @RequestParam(value = "visitKeyword", required = false) String visitKeyword,
            @RequestParam(value = "visitPage", defaultValue = "1") int visitPage,
            @RequestParam(value = "visitSize", defaultValue = "10") int visitSize,

            Model model,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        AdminView activeView = AdminView.from(view);
        MemberDto loginAdmin = loginMemberResolver.fromPrincipal(customUserDetails);
        AdminDashboardDto dashboard = adminDashboardService.getDashboard();

        memberPage = AdminPagingHelper.normalizePage(memberPage);
        chatPage = AdminPagingHelper.normalizePage(chatPage);
        boardPage = AdminPagingHelper.normalizePage(boardPage);
        visitPage = AdminPagingHelper.normalizePage(visitPage);

        memberSize = AdminPagingHelper.normalizeSize(memberSize, DEFAULT_MEMBER_SIZE);
        chatSize = AdminPagingHelper.normalizeSize(chatSize, DEFAULT_CHAT_SIZE);
        boardSize = AdminPagingHelper.normalizeSize(boardSize, DEFAULT_BOARD_SIZE);
        visitSize = AdminPagingHelper.normalizeSize(visitSize, DEFAULT_VISIT_SIZE);

        String cleanMemberKeyword = AdminPagingHelper.cleanText(memberKeyword);
        String cleanChatStatus = AdminPagingHelper.cleanText(chatStatus);
        String cleanChatCategory = AdminPagingHelper.cleanText(chatCategory);
        String cleanChatKeyword = AdminPagingHelper.cleanText(chatKeyword);
        String cleanBoardCategory = AdminPagingHelper.cleanText(boardCategory);
        String cleanBoardKeyword = AdminPagingHelper.cleanText(boardKeyword);
        String cleanVisitKeyword = AdminPagingHelper.cleanText(visitKeyword);

        List<MemberDto> adminMemberList = List.of();
        long memberTotalCount = 0L;
        if (activeView == AdminView.MEMBERS || activeView == AdminView.MEMBER_EDIT) {
            adminMemberList = adminMemberService.findMembers(cleanMemberKeyword, memberPage, memberSize);
            memberTotalCount = adminMemberService.countMembers(cleanMemberKeyword);
        }

        List<RecentChatRoomDto> adminChatRoomList = List.of();
        long chatTotalCount = 0L;
        if (activeView == AdminView.CHATS || activeView == AdminView.CHAT_ROOM) {
            adminChatRoomList = adminChatService.findRooms(
                    cleanChatStatus,
                    cleanChatCategory,
                    cleanChatKeyword,
                    loginAdmin == null ? null : loginAdmin.getNo(),
                    chatPage,
                    chatSize
            );
            chatTotalCount = adminChatService.countRooms(cleanChatStatus, cleanChatCategory, cleanChatKeyword);
        }

        List<AdminBoardDto> adminBoardList = List.of();
        long boardTotalCount = 0L;
        if (activeView == AdminView.BOARDS) {
            adminBoardList = adminBoardService.findBoards(
                    cleanBoardCategory,
                    cleanBoardKeyword,
                    boardPage,
                    boardSize
            );
            boardTotalCount = adminBoardService.countBoards(cleanBoardCategory, cleanBoardKeyword);
        }

        List<AdminVisitSummaryDto> adminVisitSummaryList = List.of();
        long visitTotalCount = 0L;
        if (activeView == AdminView.VISITS) {
            adminVisitSummaryList = adminVisitService.findVisitSummaries(
                    cleanVisitKeyword,
                    visitPage,
                    visitSize
            );
            visitTotalCount = adminVisitService.countVisitSummaries(cleanVisitKeyword);
        }

        MemberDto editMember = resolveEditMember(activeView, editMemberNo);
        ChatRoomDto activeChatRoom = resolveActiveChatRoom(activeView, roomNo, loginAdmin);
        AdminBoardDto activeBoard = resolveActiveBoard(activeView, boardNo);
        List<BoardCommentDto> activeBoardCommentList = resolveActiveBoardComments(activeView, activeBoard);

        if (activeView == AdminView.MEMBER_EDIT && editMember == null) {
            activeView = AdminView.MEMBERS;
        }

        if (activeView == AdminView.CHAT_ROOM && activeChatRoom == null) {
            activeView = AdminView.CHATS;
        }

        if (activeView == AdminView.BOARD_DETAIL && activeBoard == null) {
            activeView = AdminView.BOARDS;
        }

        model.addAttribute("dashboard", dashboard);
        model.addAttribute("loginAdmin", loginAdmin);
        model.addAttribute("loginUser", loginAdmin);
        model.addAttribute("activeView", activeView.getCode());
        model.addAttribute("activeViewEyebrow", activeView.getEyebrow());
        model.addAttribute("activeViewTitle", activeView.getTitle());

        model.addAttribute("adminMemberList", adminMemberList);
        model.addAttribute("memberKeyword", cleanMemberKeyword);
        model.addAttribute("memberPage", memberPage);
        model.addAttribute("memberSize", memberSize);
        model.addAttribute("memberTotalCount", memberTotalCount);
        model.addAttribute("memberTotalPages", AdminPagingHelper.calculateTotalPages(memberTotalCount, memberSize));
        model.addAttribute("editMember", editMember);

        model.addAttribute("adminChatRoomList", adminChatRoomList);
        model.addAttribute("chatStatus", cleanChatStatus);
        model.addAttribute("chatCategory", cleanChatCategory);
        model.addAttribute("chatKeyword", cleanChatKeyword);
        model.addAttribute("chatPage", chatPage);
        model.addAttribute("chatSize", chatSize);
        model.addAttribute("chatTotalCount", chatTotalCount);
        model.addAttribute("chatTotalPages", AdminPagingHelper.calculateTotalPages(chatTotalCount, chatSize));
        model.addAttribute("chatRoom", activeChatRoom);

        model.addAttribute("adminBoardList", adminBoardList);
        model.addAttribute("boardCategory", cleanBoardCategory);
        model.addAttribute("boardKeyword", cleanBoardKeyword);
        model.addAttribute("boardPage", boardPage);
        model.addAttribute("boardSize", boardSize);
        model.addAttribute("boardTotalCount", boardTotalCount);
        model.addAttribute("boardTotalPages", AdminPagingHelper.calculateTotalPages(boardTotalCount, boardSize));
        model.addAttribute("boardDetail", activeBoard);
        model.addAttribute("boardCommentList", activeBoardCommentList);

        model.addAttribute("adminVisitSummaryList", adminVisitSummaryList);
        model.addAttribute("visitKeyword", cleanVisitKeyword);
        model.addAttribute("visitPage", visitPage);
        model.addAttribute("visitSize", visitSize);
        model.addAttribute("visitTotalCount", visitTotalCount);
        model.addAttribute("visitTotalPages", AdminPagingHelper.calculateTotalPages(visitTotalCount, visitSize));

        return "admin/dashboard";
    }

    private MemberDto resolveEditMember(AdminView activeView, Integer editMemberNo) {
        if (activeView != AdminView.MEMBER_EDIT || editMemberNo == null) {
            return null;
        }
        return adminMemberService.findByNo(editMemberNo);
    }

    private ChatRoomDto resolveActiveChatRoom(AdminView activeView, Integer roomNo, MemberDto loginAdmin) {
        if (activeView != AdminView.CHAT_ROOM || roomNo == null || loginAdmin == null) {
            return null;
        }
        return adminChatService.assignAdminIfEmpty(roomNo, loginAdmin.getNo());
    }

    private AdminBoardDto resolveActiveBoard(AdminView activeView, Long boardNo) {
        if (activeView != AdminView.BOARD_DETAIL || boardNo == null) {
            return null;
        }
        return adminBoardService.findByNo(boardNo);
    }

    private List<BoardCommentDto> resolveActiveBoardComments(AdminView activeView, AdminBoardDto activeBoard) {
        if (activeView != AdminView.BOARD_DETAIL || activeBoard == null || activeBoard.getNo() == null) {
            return List.of();
        }
        return boardCommentService.findByBoardNo(activeBoard.getNo());
    }
}
