/*
 * admin-chat-list.js
 *
 * 이전 관리자 상담 목록(/chat/admin)에서 사용하던 레거시 스크립트입니다.
 * 현재 관리자 상담 관리는 /admin?view=chats 기준으로 통합되었습니다.
 *
 * 실제 실시간 상담 목록 갱신은 아래 파일에서 담당합니다.
 * - /static/js/admin/admin-chat-realtime.js
 */
(function () {
    const isLegacyAdminChatList = window.location.pathname === "/chat/admin";

    if (isLegacyAdminChatList) {
        window.location.replace("/admin?view=chats");
    }
})();
