package com.siyan1234.itproject2nd.admin.support;

/** 관리자 콘솔에서 사용하는 redirect 경로를 한 곳에서 관리합니다. */
public final class AdminRoutes {

    public static final String ADMIN_LOGIN = "redirect:/admin/login";
    public static final String ADMIN_HOME = "redirect:/admin";
    public static final String ADMIN_CHATS = "redirect:/admin?view=chats";
    public static final String ADMIN_MEMBERS = "redirect:/admin?view=members";

    private AdminRoutes() {
    }

    public static String view(AdminView view) {
        return "redirect:/admin?view=" + view.getCode();
    }

    public static String chatRoom(Integer roomNo) {
        return "redirect:/admin/chats/" + roomNo;
    }
}
