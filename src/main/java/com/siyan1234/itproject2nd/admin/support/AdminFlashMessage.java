package com.siyan1234.itproject2nd.admin.support;

import com.siyan1234.itproject2nd.admin.dto.AdminDeleteResultDto;

/** 관리자 작업 처리 결과 메시지를 한 곳에서 관리합니다. */
public final class AdminFlashMessage {

    public static final String CHAT_ROOM_NOT_FOUND = "존재하지 않는 상담방입니다.";
    public static final String CHAT_ALREADY_CLOSED = "이미 종료된 상담입니다.";
    public static final String CHAT_DELETE_OPEN_DENIED = "진행 중 상담은 먼저 종료한 뒤 삭제할 수 있습니다.";
    public static final String CHAT_DELETE_NOT_SELECTED = "삭제할 상담방을 선택해 주세요.";
    public static final String CHAT_DELETE_NO_AVAILABLE_ROOM = "삭제 가능한 종료 상담이 없습니다. 진행 중 상담은 종료 후 삭제해 주세요.";

    public static final String MEMBER_DELETE_NOT_SELECTED = "삭제할 회원을 선택해 주세요.";
    public static final String MEMBER_DELETE_SELF_DENIED = "현재 로그인 중인 관리자 본인 계정은 삭제할 수 없습니다.";
    public static final String MEMBER_DELETE_NOT_FOUND = "삭제할 회원을 찾을 수 없습니다.";
    public static final String MEMBER_DELETE_NO_RESULT = "삭제된 회원이 없습니다. 현재 로그인 중인 관리자 본인은 삭제할 수 없습니다.";

    public static final String MEMBER_ROLE_SELF_DENIED = "현재 로그인 중인 관리자 본인의 권한은 변경할 수 없습니다.";
    public static final String MEMBER_ROLE_CHANGE_FAILED = "회원 권한 변경에 실패했습니다.";
    public static final String MEMBER_BAN_SELF_DENIED = "현재 로그인 중인 관리자 본인 계정은 정지할 수 없습니다.";
    public static final String MEMBER_BAN_FAILED = "회원 정지 처리에 실패했습니다.";
    public static final String MEMBER_UNBAN_FAILED = "회원 정지 해제에 실패했습니다.";

    private AdminFlashMessage() {
    }

    public static String chatClosed(Integer roomNo) {
        return "상담방 #" + roomNo + "번을 종료했습니다.";
    }

    public static String chatDeleted(Integer roomNo) {
        return "종료된 상담방 #" + roomNo + "번을 삭제했습니다.";
    }

    public static String selectedChatsDeleted(AdminDeleteResultDto result) {
        return "종료 상담 " + result.getDeletedCount() + "건을 삭제했습니다."
                + skippedSuffix(result, " 진행 중이거나 없는 상담 ", "건은 제외했습니다.");
    }

    public static String memberDeleted(Integer memberNo) {
        return "회원 #" + memberNo + "번을 삭제했습니다.";
    }

    public static String selectedMembersDeleted(AdminDeleteResultDto result) {
        return "회원 " + result.getDeletedCount() + "명을 삭제했습니다."
                + skippedSuffix(result, " 제외된 항목 ", "건이 있습니다.");
    }

    public static String memberPromoted(Integer memberNo) {
        return "회원 #" + memberNo + "번에 ADMIN 권한을 부여했습니다.";
    }

    public static String memberDemoted(Integer memberNo) {
        return "회원 #" + memberNo + "번을 USER 권한으로 변경했습니다.";
    }

    public static String memberBanned(Integer memberNo) {
        return "회원 #" + memberNo + "번을 정지 처리했습니다.";
    }

    public static String memberUnbanned(Integer memberNo) {
        return "회원 #" + memberNo + "번의 정지를 해제했습니다.";
    }

    private static String skippedSuffix(AdminDeleteResultDto result, String prefix, String suffix) {
        if (result.getSkippedCount() <= 0) {
            return "";
        }
        return prefix + result.getSkippedCount() + suffix;
    }
}
