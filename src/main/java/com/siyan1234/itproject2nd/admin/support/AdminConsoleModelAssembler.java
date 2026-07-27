package com.siyan1234.itproject2nd.admin.support;

import com.siyan1234.itproject2nd.admin.dto.AdminBoardDto;
import com.siyan1234.itproject2nd.admin.dto.AdminConsoleQuery;
import com.siyan1234.itproject2nd.admin.dto.AdminDashboardDto;
import com.siyan1234.itproject2nd.admin.dto.AdminVisitSummaryDto;
import com.siyan1234.itproject2nd.admin.dto.RecentChatRoomDto;
import com.siyan1234.itproject2nd.admin.service.AdminBoardService;
import com.siyan1234.itproject2nd.admin.service.AdminChatService;
import com.siyan1234.itproject2nd.admin.service.AdminDashboardService;
import com.siyan1234.itproject2nd.admin.service.AdminMemberService;
import com.siyan1234.itproject2nd.admin.service.AdminVisitService;
import com.siyan1234.itproject2nd.board.dto.BoardCommentDto;
import com.siyan1234.itproject2nd.board.service.BoardCommentService;
import com.siyan1234.itproject2nd.chat.dto.ChatRoomDto;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import java.util.List;

/**
 * 관리자 단일 콘솔 화면에 필요한 조회와 Model 속성 조립을 담당합니다.
 *
 * <p>Controller는 HTTP 파라미터와 로그인 사용자만 전달하고, 화면별 조건 조회와
 * Thymeleaf 속성 이름 관리는 이 조립기가 전담합니다.</p>
 */
@Component
@RequiredArgsConstructor
public class AdminConsoleModelAssembler {

    private final AdminDashboardService adminDashboardService;
    private final AdminMemberService adminMemberService;
    private final AdminChatService adminChatService;
    private final AdminBoardService adminBoardService;
    private final AdminVisitService adminVisitService;
    private final BoardCommentService boardCommentService;

    public void populate(Model model, AdminConsoleQuery query, MemberDto loginAdmin) {
        query.normalize();

        AdminView activeView = AdminView.from(query.getView());
        AdminDashboardDto dashboard = adminDashboardService.getDashboard();

        /* 현재 선택된 화면에 필요한 목록만 조회해 관리자 콘솔의 불필요한 DB 접근을 줄입니다. */
        List<MemberDto> adminMemberList = List.of();
        long memberTotalCount = 0L;
        if (activeView == AdminView.MEMBERS || activeView == AdminView.MEMBER_EDIT) {
            adminMemberList = adminMemberService.findMembers(
                    query.getMemberKeyword(),
                    query.getMemberPage(),
                    query.getMemberSize()
            );
            memberTotalCount = adminMemberService.countMembers(query.getMemberKeyword());
        }

        List<RecentChatRoomDto> adminChatRoomList = List.of();
        long chatTotalCount = 0L;
        if (activeView == AdminView.CHATS || activeView == AdminView.CHAT_ROOM) {
            adminChatRoomList = adminChatService.findRooms(
                    query.getChatStatus(),
                    query.getChatCategory(),
                    query.getChatKeyword(),
                    loginAdmin == null ? null : loginAdmin.getNo(),
                    query.getChatPage(),
                    query.getChatSize()
            );
            chatTotalCount = adminChatService.countRooms(
                    query.getChatStatus(),
                    query.getChatCategory(),
                    query.getChatKeyword()
            );
        }

        List<AdminBoardDto> adminBoardList = List.of();
        long boardTotalCount = 0L;
        if (activeView == AdminView.BOARDS) {
            adminBoardList = adminBoardService.findBoards(
                    query.getBoardCategory(),
                    query.getBoardKeyword(),
                    query.getBoardPage(),
                    query.getBoardSize()
            );
            boardTotalCount = adminBoardService.countBoards(
                    query.getBoardCategory(),
                    query.getBoardKeyword()
            );
        }

        List<AdminVisitSummaryDto> adminVisitSummaryList = List.of();
        long visitTotalCount = 0L;
        if (activeView == AdminView.VISITS) {
            adminVisitSummaryList = adminVisitService.findVisitSummaries(
                    query.getVisitKeyword(),
                    query.getVisitPage(),
                    query.getVisitSize()
            );
            visitTotalCount = adminVisitService.countVisitSummaries(query.getVisitKeyword());
        }

        MemberDto editMember = resolveEditMember(activeView, query.getEditMemberNo());
        ChatRoomDto activeChatRoom = resolveActiveChatRoom(activeView, query.getRoomNo(), loginAdmin);
        AdminBoardDto activeBoard = resolveActiveBoard(activeView, query.getBoardNo());
        AdminBoardDto boardForm = resolveBoardForm(activeView, activeBoard);
        List<BoardCommentDto> activeBoardCommentList = resolveActiveBoardComments(activeView, activeBoard);

        activeView = fallbackInvalidDetailView(activeView, editMember, activeChatRoom, activeBoard);

        addCommonAttributes(model, dashboard, loginAdmin, activeView);
        addMemberAttributes(model, query, adminMemberList, memberTotalCount, editMember);
        addChatAttributes(model, query, adminChatRoomList, chatTotalCount, activeChatRoom);
        addBoardAttributes(model, query, adminBoardList, boardTotalCount, activeBoard, boardForm, activeBoardCommentList);
        addVisitAttributes(model, query, adminVisitSummaryList, visitTotalCount);
    }

    private void addCommonAttributes(
            Model model,
            AdminDashboardDto dashboard,
            MemberDto loginAdmin,
            AdminView activeView
    ) {
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("loginAdmin", loginAdmin);
        model.addAttribute("loginUser", loginAdmin);
        model.addAttribute("activeView", activeView.getCode());
        model.addAttribute("activeViewEyebrow", activeView.getEyebrow());
        model.addAttribute("activeViewTitle", activeView.getTitle());
    }

    private void addMemberAttributes(
            Model model,
            AdminConsoleQuery query,
            List<MemberDto> memberList,
            long totalCount,
            MemberDto editMember
    ) {
        model.addAttribute("adminMemberList", memberList);
        model.addAttribute("memberKeyword", query.getMemberKeyword());
        model.addAttribute("memberPage", query.getMemberPage());
        model.addAttribute("memberSize", query.getMemberSize());
        model.addAttribute("memberTotalCount", totalCount);
        model.addAttribute("memberTotalPages", AdminPagingHelper.calculateTotalPages(totalCount, query.getMemberSize()));
        model.addAttribute("editMember", editMember);
    }

    private void addChatAttributes(
            Model model,
            AdminConsoleQuery query,
            List<RecentChatRoomDto> roomList,
            long totalCount,
            ChatRoomDto activeChatRoom
    ) {
        model.addAttribute("adminChatRoomList", roomList);
        model.addAttribute("chatStatus", query.getChatStatus());
        model.addAttribute("chatCategory", query.getChatCategory());
        model.addAttribute("chatKeyword", query.getChatKeyword());
        model.addAttribute("chatPage", query.getChatPage());
        model.addAttribute("chatSize", query.getChatSize());
        model.addAttribute("chatTotalCount", totalCount);
        model.addAttribute("chatTotalPages", AdminPagingHelper.calculateTotalPages(totalCount, query.getChatSize()));
        model.addAttribute("chatRoom", activeChatRoom);
    }

    private void addBoardAttributes(
            Model model,
            AdminConsoleQuery query,
            List<AdminBoardDto> boardList,
            long totalCount,
            AdminBoardDto activeBoard,
            AdminBoardDto boardForm,
            List<BoardCommentDto> commentList
    ) {
        model.addAttribute("adminBoardList", boardList);
        model.addAttribute("boardCategory", query.getBoardCategory());
        model.addAttribute("boardKeyword", query.getBoardKeyword());
        model.addAttribute("boardPage", query.getBoardPage());
        model.addAttribute("boardSize", query.getBoardSize());
        model.addAttribute("boardTotalCount", totalCount);
        model.addAttribute("boardTotalPages", AdminPagingHelper.calculateTotalPages(totalCount, query.getBoardSize()));
        model.addAttribute("boardDetail", activeBoard);
        model.addAttribute("boardForm", boardForm);
        model.addAttribute("boardCommentList", commentList);
    }

    private void addVisitAttributes(
            Model model,
            AdminConsoleQuery query,
            List<AdminVisitSummaryDto> visitList,
            long totalCount
    ) {
        model.addAttribute("adminVisitSummaryList", visitList);
        model.addAttribute("visitKeyword", query.getVisitKeyword());
        model.addAttribute("visitPage", query.getVisitPage());
        model.addAttribute("visitSize", query.getVisitSize());
        model.addAttribute("visitTotalCount", totalCount);
        model.addAttribute("visitTotalPages", AdminPagingHelper.calculateTotalPages(totalCount, query.getVisitSize()));
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

        // 상담방을 처음 연 관리자를 담당자로 지정하되, 이미 배정된 방은 기존 담당자를 유지합니다.
        return adminChatService.assignAdminIfEmpty(roomNo, loginAdmin.getNo());
    }

    private AdminBoardDto resolveActiveBoard(AdminView activeView, Long boardNo) {
        if ((activeView != AdminView.BOARD_DETAIL && activeView != AdminView.BOARD_EDIT) || boardNo == null) {
            return null;
        }
        return adminBoardService.findByNo(boardNo);
    }

    private AdminBoardDto resolveBoardForm(AdminView activeView, AdminBoardDto activeBoard) {
        if (activeView == AdminView.BOARD_CREATE) {
            AdminBoardDto boardForm = new AdminBoardDto();
            boardForm.setCategory("QUESTION");
            boardForm.setAnswerStatus("WAITING");
            return boardForm;
        }

        if (activeView == AdminView.BOARD_EDIT) {
            return activeBoard;
        }

        return null;
    }

    private List<BoardCommentDto> resolveActiveBoardComments(AdminView activeView, AdminBoardDto activeBoard) {
        if (activeView != AdminView.BOARD_DETAIL || activeBoard == null || activeBoard.getNo() == null) {
            return List.of();
        }
        return boardCommentService.findByBoardNo(activeBoard.getNo());
    }

    private AdminView fallbackInvalidDetailView(
            AdminView activeView,
            MemberDto editMember,
            ChatRoomDto activeChatRoom,
            AdminBoardDto activeBoard
    ) {
        if (activeView == AdminView.MEMBER_EDIT && editMember == null) {
            return AdminView.MEMBERS;
        }

        if (activeView == AdminView.CHAT_ROOM && activeChatRoom == null) {
            return AdminView.CHATS;
        }

        if ((activeView == AdminView.BOARD_DETAIL || activeView == AdminView.BOARD_EDIT) && activeBoard == null) {
            return AdminView.BOARDS;
        }

        return activeView;
    }
}
