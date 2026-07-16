/*
 * admin-dashboard.js
 * 관리자 콘솔 JS 진입점.
 * 실제 기능은 /js/admin/*.js 파일로 분리되어 있습니다.
 */
(function (window) {
    const Admin = window.SecondProAdmin;

    if (!Admin) {
        console.error("SecondProAdmin 공통 스크립트가 로드되지 않았습니다.");
        return;
    }

    Admin.ready(function () {
        Admin.Visual?.init();
        Admin.Modal?.init();
        Admin.Selection?.init();
        Admin.Delete?.init();
        Admin.MemberActions?.init();
        Admin.SessionGuard?.init();
        Admin.BrowserNotification?.init();
        Admin.ChatRealtime?.init();
    });
})(window);
