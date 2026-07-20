package com.siyan1234.itproject2nd.admin.controller;

import com.siyan1234.itproject2nd.admin.dto.AdminDeleteResultDto;
import com.siyan1234.itproject2nd.admin.service.AdminBoardService;
import com.siyan1234.itproject2nd.admin.support.AdminFlashMessage;
import com.siyan1234.itproject2nd.admin.support.AdminRoutes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/{boardNo}")
    public String boardDetail(
            @PathVariable("boardNo") Long boardNo,
            RedirectAttributes redirectAttributes
    ) {
        redirectAttributes.addAttribute("view", "boards");
        redirectAttributes.addAttribute("focusBoardNo", boardNo);
        return AdminRoutes.ADMIN_HOME;
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
