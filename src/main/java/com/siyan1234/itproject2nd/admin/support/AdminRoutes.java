package com.siyan1234.itproject2nd.admin.support;

/** 관리자 콘솔에서 사용하는 redirect 경로를 한 곳에서 관리합니다. */
public final class AdminRoutes {

    public static final String ADMIN_LOGIN = "redirect:/admin/login";
    public static final String ADMIN_HOME = "redirect:/admin";
    public static final String ADMIN_CHATS = "redirect:/admin?view=chats";
    public static final String ADMIN_MEMBERS = "redirect:/admin?view=members";
    public static final String ADMIN_BOARDS = "redirect:/admin?view=boards";
    public static final String ADMIN_VISITS = "redirect:/admin?view=visits";

    private AdminRoutes() {
    }

    public static String view(AdminView view) {
        return "redirect:/admin?view=" + view.getCode();
    }

    public static String chatRoom(Integer roomNo) {
        return "redirect:/admin?view=chatRoom&roomNo=" + roomNo;
    }

    public static String boardCreate() {
        return "redirect:/admin?view=boardCreate";
    }

    public static String boardEdit(Long boardNo) {
        return "redirect:/admin?view=boardEdit&boardNo=" + boardNo;
    }

    public static String boardDetail(Long boardNo) {
        return "redirect:/admin?view=boardDetail&boardNo=" + boardNo;
    }
}
