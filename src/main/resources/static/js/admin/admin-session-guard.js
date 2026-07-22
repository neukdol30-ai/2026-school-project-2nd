/*
 * admin-session-guard.js
 * 관리자 콘솔이 열린 상태에서 권한이 ADMIN -> USER로 변경되었는지 주기적으로 확인합니다.
 */
(function (window, document) {
    const Admin = window.SecondProAdmin = window.SecondProAdmin || {};
    const CHECK_URL = "/admin/auth/status";
    const REDIRECT_URL = "/admin/login?error=role";
    const CHECK_INTERVAL_MS = 10000;

    let checking = false;
    let redirected = false;
    let intervalId = null;

    function initSessionGuard() {
        checkAdminStatus();

        intervalId = window.setInterval(checkAdminStatus, CHECK_INTERVAL_MS);

        window.addEventListener("focus", checkAdminStatus);
        document.addEventListener("visibilitychange", function () {
            if (!document.hidden) {
                checkAdminStatus();
            }
        });
    }

    function checkAdminStatus() {
        if (checking || redirected) {
            return;
        }

        checking = true;

        fetch(CHECK_URL, {
            method: "GET",
            headers: {
                "Accept": "application/json",
                "X-Requested-With": "XMLHttpRequest"
            },
            cache: "no-store",
            credentials: "same-origin"
        })
            .then(function (response) {
                if (!response.ok) {
                    return handleInvalidAdmin();
                }
                return response.json();
            })
            .then(function (data) {
                if (data && data.activeAdmin === false) {
                    handleInvalidAdmin();
                }
            })
            .catch(function () {
                // 일시적인 네트워크 오류는 화면을 강제로 내보내지 않습니다.
            })
            .finally(function () {
                checking = false;
            });
    }

    function handleInvalidAdmin() {
        if (redirected) {
            return;
        }

        redirected = true;

        if (intervalId) {
            window.clearInterval(intervalId);
        }

        alert("관리자 권한이 변경되었습니다. 관리자 로그인을 다시 진행해 주세요.");
        window.location.href = REDIRECT_URL;
    }

    Admin.SessionGuard = {
        init: initSessionGuard
    };
})(window, document);
