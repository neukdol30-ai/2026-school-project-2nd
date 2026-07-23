package com.siyan1234.itproject2nd.admin.controller;

import com.siyan1234.itproject2nd.admin.dto.AdminBoardDto;
import com.siyan1234.itproject2nd.admin.dto.AdminDeleteResultDto;
import com.siyan1234.itproject2nd.admin.service.AdminBoardService;
import com.siyan1234.itproject2nd.admin.support.AdminFlashMessage;
import com.siyan1234.itproject2nd.admin.support.AdminRoutes;
import com.siyan1234.itproject2nd.board.dto.BoardCommentDto;
import com.siyan1234.itproject2nd.member.dto.CustomUserDetails;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/** 관리자 콘솔 게시글 관리 Controller입니다. */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/boards")
public class AdminBoardController {

    private final AdminBoardService adminBoardService;

    @GetMapping
    public String boardList() {
        return AdminRoutes.ADMIN_BOARDS;
    }

    @GetMapping("/new")
    public String boardCreateForm() {
        return AdminRoutes.boardCreate();
    }

    @PostMapping("/new")
    public String createBoard(
            @ModelAttribute AdminBoardDto boardDto,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            RedirectAttributes redirectAttributes
    ) {
        MemberDto loginAdmin = customUserDetails == null ? null : customUserDetails.getMemberDto();
        Integer writerNo = loginAdmin == null ? null : loginAdmin.getNo();

        try {
            int insertedCount = adminBoardService.createBoard(boardDto, writerNo);
            if (insertedCount == 0) {
                redirectAttributes.addFlashAttribute("adminErrorMessage", AdminFlashMessage.BOARD_SAVE_FAILED);
                return AdminRoutes.boardCreate();
            }
            redirectAttributes.addFlashAttribute("adminMessage", AdminFlashMessage.boardCreated());
            return AdminRoutes.ADMIN_BOARDS;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", e.getMessage());
            return AdminRoutes.boardCreate();
        }
    }

    @PostMapping("/save")
    public String saveBoard(
            @ModelAttribute AdminBoardDto boardDto,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            RedirectAttributes redirectAttributes
    ) {
        if (boardDto.getNo() == null) {
            return createBoard(boardDto, customUserDetails, redirectAttributes);
        }
        return updateBoard(boardDto.getNo(), boardDto, redirectAttributes);
    }

    @GetMapping("/{boardNo}")
    public String boardDetail(
            @PathVariable("boardNo") Long boardNo
    ) {
        return AdminRoutes.boardDetail(boardNo);
    }

    @GetMapping("/{boardNo}/edit")
    public String boardEditForm(
            @PathVariable("boardNo") Long boardNo
    ) {
        return AdminRoutes.boardEdit(boardNo);
    }

    @PostMapping("/{boardNo}/edit")
    public String updateBoard(
            @PathVariable("boardNo") Long boardNo,
            @ModelAttribute AdminBoardDto boardDto,
            RedirectAttributes redirectAttributes
    ) {
        boardDto.setNo(boardNo);

        try {
            int updatedCount = adminBoardService.updateBoard(boardDto);
            if (updatedCount == 0) {
                redirectAttributes.addFlashAttribute("adminErrorMessage", AdminFlashMessage.BOARD_UPDATE_NOT_FOUND);
                return AdminRoutes.ADMIN_BOARDS;
            }
            redirectAttributes.addFlashAttribute("adminMessage", AdminFlashMessage.boardUpdated(boardNo));
            return AdminRoutes.boardDetail(boardNo);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", e.getMessage());
            return AdminRoutes.boardEdit(boardNo);
        }
    }


    @PostMapping("/{boardNo}/answers")
    public String createAnswer(
            @PathVariable("boardNo") Long boardNo,
            @ModelAttribute BoardCommentDto commentDto,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            RedirectAttributes redirectAttributes
    ) {
        MemberDto loginAdmin = customUserDetails == null ? null : customUserDetails.getMemberDto();
        Integer writerNo = loginAdmin == null ? null : loginAdmin.getNo();

        try {
            int insertedCount = adminBoardService.createAnswer(boardNo, commentDto, writerNo);
            if (insertedCount == 0) {
                redirectAttributes.addFlashAttribute("adminErrorMessage", AdminFlashMessage.BOARD_ANSWER_SAVE_FAILED);
            } else {
                redirectAttributes.addFlashAttribute("adminMessage", AdminFlashMessage.boardAnswerCreated(boardNo));
            }
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", e.getMessage());
        }

        return AdminRoutes.boardDetail(boardNo);
    }

    @PostMapping("/{boardNo}/answers/{answerNo}/edit")
    public String updateAnswer(
            @PathVariable("boardNo") Long boardNo,
            @PathVariable("answerNo") Long answerNo,
            @ModelAttribute BoardCommentDto commentDto,
            RedirectAttributes redirectAttributes
    ) {
        try {
            int updatedCount = adminBoardService.updateAnswer(boardNo, answerNo, commentDto);
            if (updatedCount == 0) {
                redirectAttributes.addFlashAttribute("adminErrorMessage", AdminFlashMessage.BOARD_ANSWER_NOT_FOUND);
            } else {
                redirectAttributes.addFlashAttribute("adminMessage", AdminFlashMessage.boardAnswerUpdated(answerNo));
            }
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", e.getMessage());
        }

        return AdminRoutes.boardDetail(boardNo);
    }

    @PostMapping("/{boardNo}/answers/{answerNo}/delete")
    public String deleteAnswer(
            @PathVariable("boardNo") Long boardNo,
            @PathVariable("answerNo") Long answerNo,
            RedirectAttributes redirectAttributes
    ) {
        try {
            int deletedCount = adminBoardService.deleteAnswer(boardNo, answerNo);
            if (deletedCount == 0) {
                redirectAttributes.addFlashAttribute("adminErrorMessage", AdminFlashMessage.BOARD_ANSWER_NOT_FOUND);
            } else {
                redirectAttributes.addFlashAttribute("adminMessage", AdminFlashMessage.boardAnswerDeleted(answerNo));
            }
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", e.getMessage());
        }

        return AdminRoutes.boardDetail(boardNo);
    }

    @PostMapping("/{boardNo}/delete")
    public String deleteBoard(
            @PathVariable("boardNo") Long boardNo,
            RedirectAttributes redirectAttributes
    ) {
        int deletedCount = adminBoardService.deleteBoard(boardNo);

        if (deletedCount == 0) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", AdminFlashMessage.BOARD_DELETE_NOT_FOUND);
        } else {
            redirectAttributes.addFlashAttribute("adminMessage", AdminFlashMessage.boardDeleted(boardNo));
        }

        return AdminRoutes.ADMIN_BOARDS;
    }

    @PostMapping("/delete")
    public String deleteSelectedBoards(
            @RequestParam(value = "boardNoList", required = false) List<Long> boardNoList,
            RedirectAttributes redirectAttributes
    ) {
        AdminDeleteResultDto result = adminBoardService.deleteBoards(boardNoList);

        if (result.getRequestedCount() == 0) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", AdminFlashMessage.BOARD_DELETE_NOT_SELECTED);
            return AdminRoutes.ADMIN_BOARDS;
        }

        if (!result.hasDeletedItem()) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", AdminFlashMessage.BOARD_DELETE_NO_RESULT);
        } else {
            redirectAttributes.addFlashAttribute("adminMessage", AdminFlashMessage.selectedBoardsDeleted(result));
        }

        return AdminRoutes.ADMIN_BOARDS;
    }
}
