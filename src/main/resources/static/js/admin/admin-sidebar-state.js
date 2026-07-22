/*
 * admin-sidebar-state.js
 * 역할: 관리자 사이드바 운영 팁 접기/펼치기 상태를 화면 이동 후에도 유지합니다.
 */
(function (window) {
    const Admin = window.SecondProAdmin || (window.SecondProAdmin = {});
    const STORAGE_KEY = "secondpro.admin.sidebar.tip.open";

    function init() {
        const tipCard = document.querySelector(".sidebar-tip-card");
        if (!tipCard) {
            return;
        }

        const savedState = localStorage.getItem(STORAGE_KEY);
        if (savedState === "closed") {
            tipCard.removeAttribute("open");
        }
        if (savedState === "open") {
            tipCard.setAttribute("open", "open");
        }

        tipCard.addEventListener("toggle", function () {
            localStorage.setItem(STORAGE_KEY, tipCard.open ? "open" : "closed");
        });
    }

    Admin.SidebarState = { init };
})(window);
