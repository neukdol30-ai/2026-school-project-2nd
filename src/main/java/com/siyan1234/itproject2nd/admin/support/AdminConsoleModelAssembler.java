package com.siyan1234.itproject2nd.admin.support;

import com.siyan1234.itproject2nd.admin.dto.AdminBoardDto;
import com.siyan1234.itproject2nd.admin.dto.AdminConsoleQuery;
import com.siyan1234.itproject2nd.admin.dto.AdminDashboardDto;
import com.siyan1234.itproject2nd.admin.dto.AdminVisitOverviewDto;
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
import com.siyan1234.itproject2nd.chat.support.ChatMessagePolicy;
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

    /**
     * 관리자 화면 데이터를 조립합니다.
     *
     * @return 주소창의 화면·페이지 값이 유효 범위를 벗어나 정규 URL로
     *         다시 이동해야 하면 {@code true}
     */
    public boolean populate(Model model, AdminConsoleQuery query, MemberDto loginAdmin) {
        int requestedMemberPage = query.getMemberPage();
        int requestedChatPage = query.getChatPage();
        int requestedBoardPage = query.getBoardPage();
        int requestedVisitPage = query.getVisitPage();

        query.normalize();

        AdminView activeView = AdminView.from(query.getView());

        // 존재하지 않는 view 값은 dashboard로 정규화하여 주소창과 실제 화면을 일치시킵니다.
        if (!activeView.getCode().equals(query.getView())) {
            query.setView(activeView.getCode());
            return true;
        }

        /* 현재 선택된 화면에 필요한 목록만 조회해 관리자 콘솔의 불필요한 DB 접근을 줄입니다. */
        List<MemberDto> adminMemberList = List.of();
        long memberTotalCount = 0L;
        if (activeView == AdminView.MEMBERS || activeView == AdminView.MEMBER_EDIT) {
            memberTotalCount = adminMemberService.countMembers(query.getMemberKeyword());

            if (activeView == AdminView.MEMBERS) {
                int safePage = AdminPagingHelper.clampPage(
                        query.getMemberPage(),
                        memberTotalCount
                );
                if (safePage != requestedMemberPage) {
                    query.setMemberPage(safePage);
                    return true;
                }
            }

            adminMemberList = adminMemberService.findMembers(
                    query.getMemberKeyword(),
                    query.getMemberPage()
            );
        }

        List<RecentChatRoomDto> adminChatRoomList = List.of();
        long chatTotalCount = 0L;
        if (activeView == AdminView.CHATS || activeView == AdminView.CHAT_ROOM) {
            chatTotalCount = adminChatService.countRooms(
                    query.getChatStatus(),
                    query.getChatCategory(),
                    query.getChatKeyword()
            );

            if (activeView == AdminView.CHATS) {
                int safePage = AdminPagingHelper.clampPage(
                        query.getChatPage(),
                        chatTotalCount
                );
                if (safePage != requestedChatPage) {
                    query.setChatPage(safePage);
                    return true;
                }
            }

            adminChatRoomList = adminChatService.findRooms(
                    query.getChatStatus(),
                    query.getChatCategory(),
                    query.getChatKeyword(),
                    loginAdmin == null ? null : loginAdmin.getNo(),
                    query.getChatPage()
            );
        }

        List<AdminBoardDto> adminBoardList = List.of();
        long boardTotalCount = 0L;
        if (activeView == AdminView.BOARDS) {
            boardTotalCount = adminBoardService.countBoards(
                    query.getBoardCategory(),
                    query.getBoardKeyword()
            );
            int safePage = AdminPagingHelper.clampPage(
                    query.getBoardPage(),
                    boardTotalCount
            );
            if (safePage != requestedBoardPage) {
                query.setBoardPage(safePage);
                return true;
            }

            adminBoardList = adminBoardService.findBoards(
                    query.getBoardCategory(),
                    query.getBoardKeyword(),
                    query.getBoardPage()
            );
        }

        List<AdminVisitSummaryDto> adminVisitSummaryList = List.of();
        AdminVisitOverviewDto adminVisitOverview = null;
        long visitTotalCount = 0L;
        if (activeView == AdminView.VISITS) {
            visitTotalCount = adminVisitService.countVisitSummaries(query.getVisitKeyword());
            int safePage = AdminPagingHelper.clampPage(
                    query.getVisitPage(),
                    visitTotalCount
            );
            if (safePage != requestedVisitPage) {
                query.setVisitPage(safePage);
                return true;
            }

            adminVisitSummaryList = adminVisitService.findVisitSummaries(
                    query.getVisitKeyword(),
                    query.getVisitPage()
            );
            adminVisitOverview = adminVisitService.getVisitOverview();
        }

        AdminDashboardDto dashboard = adminDashboardService.getDashboard();
        MemberDto editMember = resolveEditMember(activeView, query.getEditMemberNo());
        ChatRoomDto activeChatRoom = resolveActiveChatRoom(activeView, query.getRoomNo(), loginAdmin);
        AdminBoardDto activeBoard = resolveActiveBoard(activeView, query.getBoardNo());
        AdminBoardDto boardForm = resolveBoardForm(activeView, activeBoard);
        List<BoardCommentDto> activeBoardCommentList = resolveActiveBoardComments(activeView, activeBoard);

        AdminView resolvedView = fallbackInvalidDetailView(activeView, editMember, activeChatRoom, activeBoard);
        if (resolvedView != activeView) {
            query.setView(resolvedView.getCode());
            return true;
        }

        addCommonAttributes(model, dashboard, loginAdmin, resolvedView);
        addMemberAttributes(model, query, adminMemberList, memberTotalCount, editMember);
        addChatAttributes(model, query, adminChatRoomList, chatTotalCount, activeChatRoom);
        addBoardAttributes(model, query, adminBoardList, boardTotalCount, activeBoard, boardForm, activeBoardCommentList);
        addVisitAttributes(model, query, adminVisitSummaryList, visitTotalCount, adminVisitOverview);
        return false;
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
        model.addAttribute("chatMessageMaxLength", ChatMessagePolicy.MAX_LENGTH);
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
        int totalPages = AdminPagingHelper.calculateTotalPages(totalCount);
        model.addAttribute("memberTotalCount", totalCount);
        model.addAttribute("memberTotalPages", totalPages);
        model.addAttribute("memberPageNumbers", AdminPagingHelper.buildPageNumbers(query.getMemberPage(), totalPages));
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
        int totalPages = AdminPagingHelper.calculateTotalPages(totalCount);
        model.addAttribute("chatTotalCount", totalCount);
        model.addAttribute("chatTotalPages", totalPages);
        model.addAttribute("chatPageNumbers", AdminPagingHelper.buildPageNumbers(query.getChatPage(), totalPages));
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
        int totalPages = AdminPagingHelper.calculateTotalPages(totalCount);
        model.addAttribute("boardTotalCount", totalCount);
        model.addAttribute("boardTotalPages", totalPages);
        model.addAttribute("boardPageNumbers", AdminPagingHelper.buildPageNumbers(query.getBoardPage(), totalPages));
        model.addAttribute("boardDetail", activeBoard);
        model.addAttribute("boardForm", boardForm);
        model.addAttribute("boardCommentList", commentList);
    }

    private void addVisitAttributes(
            Model model,
            AdminConsoleQuery query,
            List<AdminVisitSummaryDto> visitList,
            long totalCount,
            AdminVisitOverviewDto visitOverview
    ) {
        model.addAttribute("adminVisitSummaryList", visitList);
        model.addAttribute("visitKeyword", query.getVisitKeyword());
        model.addAttribute("visitPage", query.getVisitPage());
        int totalPages = AdminPagingHelper.calculateTotalPages(totalCount);
        model.addAttribute("visitTotalCount", totalCount);
        model.addAttribute("visitTotalPages", totalPages);
        model.addAttribute("visitPageNumbers", AdminPagingHelper.buildPageNumbers(query.getVisitPage(), totalPages));
        model.addAttribute("visitOverview", visitOverview);
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
