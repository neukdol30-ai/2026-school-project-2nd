/*
 * admin-browser-notification.js
 * 관리자 콘솔 브라우저 알림 담당.
 *
 * 브라우저 알림은 사용자 동의가 필요한 기능입니다.
 * /admin?view=chats 화면에서 관리자가 "브라우저 알림 켜기"를 누르면 권한을 요청하고,
 * 이후 새 상담/새 메시지 도착 WebSocket 이벤트를 받을 때 데스크톱 알림과 화면 내부 토스트를 표시합니다.
 */
(function (window, document) {
    const Admin = window.SecondProAdmin = window.SecondProAdmin || {};
    const STORAGE_KEY = "SECONDPRO_ADMIN_CHAT_NOTIFICATION";

    const NOTIFICATION_COOLDOWN_MILLIS = 1500;

    let lastNotifiedAt = 0;
    let lastNotifiedKey = "";

    function initBrowserNotification() {
        updateNotificationButton();

        document.addEventListener("click", function (event) {
            const button = event.target.closest("[data-admin-notification-button]");

            if (!button) {
                return;
            }

            handlePermissionRequest();
        });
    }

    function handlePermissionRequest() {
        if (!isNotificationSupported()) {
            showToast("이 브라우저는 알림 기능을 지원하지 않습니다.", "error");
            updateNotificationButton("지원 안 됨");
            return;
        }

        if (Notification.permission === "granted") {
            localStorage.setItem(STORAGE_KEY, "Y");
            showToast("상담 알림이 이미 켜져 있습니다.", "success");
            updateNotificationButton();
            return;
        }

        if (Notification.permission === "denied") {
            localStorage.setItem(STORAGE_KEY, "N");
            showToast("브라우저 설정에서 알림 허용이 차단되어 있습니다.", "error");
            updateNotificationButton();
            return;
        }

        Notification.requestPermission().then(function (permission) {
            const allowed = permission === "granted";
            localStorage.setItem(STORAGE_KEY, allowed ? "Y" : "N");
            updateNotificationButton();

            if (allowed) {
                showToast("상담 알림이 켜졌습니다.", "success");
                showBrowserNotification("SecondPro 상담 알림", "새 상담이 오면 이곳에 알림을 표시합니다.");
            } else {
                showToast("상담 알림 권한이 허용되지 않았습니다.", "error");
            }
        });
    }

    function notifyChatEvent(roomNo) {
        if (isDuplicateNotification(roomNo)) {
            return;
        }

        const message = roomNo
            ? "상담방 #" + roomNo + "번에 새 상담 또는 새 메시지가 도착했습니다."
            : "새 상담 또는 새 메시지가 도착했습니다.";

        showToast(message, "success");

        if (!shouldUseBrowserNotification()) {
            return;
        }

        showBrowserNotification("SecondPro 상담 알림", message);
    }

    /**
     * 같은 상담방의 ADMIN_ROOM_REFRESH 이벤트가 짧은 시간 안에 여러 번 들어오면
     * 토스트가 2개 이상 겹쳐 보일 수 있습니다.
     * 화면 내부 토스트와 브라우저 알림 모두 같은 기준으로 중복 표시를 막습니다.
     */
    function isDuplicateNotification(roomNo) {
        const now = Date.now();
        const notificationKey = String(roomNo || "ALL");

        if (lastNotifiedKey === notificationKey && now - lastNotifiedAt < NOTIFICATION_COOLDOWN_MILLIS) {
            return true;
        }

        lastNotifiedKey = notificationKey;
        lastNotifiedAt = now;
        return false;
    }

    function shouldUseBrowserNotification() {
        return isNotificationSupported()
            && Notification.permission === "granted"
            && localStorage.getItem(STORAGE_KEY) === "Y";
    }

    function showBrowserNotification(title, body) {
        try {
            const notification = new Notification(title, {
                body: body,
                tag: "secondpro-admin-chat",
                renotify: true
            });

            notification.onclick = function () {
                window.focus();
                notification.close();
            };

            setTimeout(function () {
                notification.close();
            }, 5000);
        } catch (error) {
            console.log("관리자 브라우저 알림 표시 실패", error);
        }
    }

    function showToast(message, type) {
        const container = getToastContainer();
        const toast = document.createElement("div");
        toast.className = "admin-toast " + (type || "info");
        toast.textContent = message;

        container.appendChild(toast);

        setTimeout(function () {
            toast.classList.add("show");
        }, 10);

        setTimeout(function () {
            toast.classList.remove("show");
            setTimeout(function () {
                toast.remove();
            }, 250);
        }, 4200);
    }

    function getToastContainer() {
        let container = document.getElementById("adminToastContainer");

        if (container) {
            return container;
        }

        container = document.createElement("div");
        container.id = "adminToastContainer";
        container.className = "admin-toast-container";
        document.body.appendChild(container);
        return container;
    }

    function updateNotificationButton(customLabel) {
        const button = document.querySelector("[data-admin-notification-button]");

        if (!button) {
            return;
        }

        if (!isNotificationSupported()) {
            button.textContent = customLabel || "알림 미지원";
            button.disabled = true;
            button.classList.add("disabled");
            return;
        }

        button.disabled = false;
        button.classList.remove("disabled", "enabled", "blocked");

        if (Notification.permission === "granted" && localStorage.getItem(STORAGE_KEY) === "Y") {
            button.textContent = "🔔 알림 켜짐";
            button.classList.add("enabled");
            return;
        }

        if (Notification.permission === "denied") {
            button.textContent = "🔕 알림 차단됨";
            button.classList.add("blocked");
            return;
        }

        button.textContent = customLabel || "🔔 브라우저 알림 켜기";
    }

    function isNotificationSupported() {
        return "Notification" in window;
    }

    Admin.BrowserNotification = {
        init: initBrowserNotification,
        notifyChatEvent: notifyChatEvent,
        showToast: showToast
    };
})(window, document);
