/*
 * admin-modal.js
 * 관리자 프로필/회원 상세 모달 담당.
 */
(function (window, document) {
    const Admin = window.SecondProAdmin = window.SecondProAdmin || {};

    function initModalEvents() {
        document.querySelectorAll("[data-modal-open]").forEach(function (button) {
            button.addEventListener("click", function () {
                const modalId = button.getAttribute("data-modal-open");
                openModal(modalId);
            });
        });

        document.querySelectorAll("[data-modal-close]").forEach(function (button) {
            button.addEventListener("click", function () {
                closeAllModals();
            });
        });

        document.addEventListener("keydown", function (event) {
            if (event.key === "Escape") {
                closeAllModals();
            }
        });
    }

    function openModal(modalId) {
        const modal = document.getElementById(modalId);

        if (!modal) {
            return;
        }

        modal.classList.add("is-open");
        modal.setAttribute("aria-hidden", "false");
    }

    function closeAllModals() {
        document.querySelectorAll(".admin-modal.is-open").forEach(function (modal) {
            modal.classList.remove("is-open");
            modal.setAttribute("aria-hidden", "true");
        });
    }

    function initMemberDetailModal() {
        document.addEventListener("click", function (event) {
            const button = event.target.closest("[data-member-detail-button]");

            if (!button) {
                return;
            }

            Admin.setText("memberModalNo", button.dataset.memberNo || "-");
            Admin.setText("memberModalId", button.dataset.memberId || "-");
            Admin.setText("memberModalName", button.dataset.memberName || "-");
            Admin.setText("memberModalNickname", button.dataset.memberNickname || "-");
            Admin.setText("memberModalEmail", button.dataset.memberEmail || "미등록");
            Admin.setText("memberModalPhone", button.dataset.memberPhone || "미등록");
            Admin.setText("memberModalRole", button.dataset.memberRole || "-");
            Admin.setText("memberModalRegdate", button.dataset.memberRegdate || "-");
            Admin.setText("memberModalTitle", button.dataset.memberId || "회원 상세");
            Admin.setText("memberModalSubtitle", button.dataset.memberEmail || "회원 정보를 확인합니다.");

            const avatar = document.getElementById("memberModalAvatar");
            if (avatar) {
                const nickname = button.dataset.memberNickname || button.dataset.memberId || "U";
                avatar.textContent = nickname.substring(0, 1).toUpperCase();
            }

            openModal("memberDetailModal");
        });
    }

    Admin.Modal = {
        init: function () {
            initModalEvents();
            initMemberDetailModal();
        },
        open: openModal,
        closeAll: closeAllModals
    };
})(window, document);
