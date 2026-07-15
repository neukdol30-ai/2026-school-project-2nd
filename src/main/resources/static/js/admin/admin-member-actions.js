/*
 * admin-member-actions.js
 * 관리자 콘솔 회원 권한/정지 상태 변경 담당.
 */
(function (window, document) {
    const Admin = window.SecondProAdmin = window.SecondProAdmin || {};

    function initMemberActions() {
        document.addEventListener("click", function (event) {
            const promoteButton = event.target.closest("[data-member-promote-button]");
            const demoteButton = event.target.closest("[data-member-demote-button]");
            const banButton = event.target.closest("[data-member-ban-button]");
            const unbanButton = event.target.closest("[data-member-unban-button]");

            if (promoteButton) {
                handlePromote(promoteButton);
                return;
            }

            if (demoteButton) {
                handleDemote(demoteButton);
                return;
            }

            if (banButton) {
                handleBan(banButton);
                return;
            }

            if (unbanButton) {
                handleUnban(unbanButton);
            }
        });
    }

    function handlePromote(button) {
        const memberNo = button.dataset.memberNo;
        const memberId = button.dataset.memberId || memberNo;

        if (!memberNo) {
            return;
        }

        if (!confirm("회원 [" + memberId + "]에게 ADMIN 권한을 부여하시겠습니까?")) {
            return;
        }

        Admin.submitPostForm("/admin/members/" + encodeURIComponent(memberNo) + "/role/admin");
    }

    function handleDemote(button) {
        const memberNo = button.dataset.memberNo;
        const memberId = button.dataset.memberId || memberNo;

        if (!memberNo) {
            return;
        }

        if (!confirm("관리자 [" + memberId + "]을 USER 권한으로 변경하시겠습니까?")) {
            return;
        }

        Admin.submitPostForm("/admin/members/" + encodeURIComponent(memberNo) + "/role/user");
    }

    function handleBan(button) {
        const memberNo = button.dataset.memberNo;
        const memberId = button.dataset.memberId || memberNo;

        if (!memberNo) {
            return;
        }

        const reason = prompt(
            "회원 [" + memberId + "]을 정지 처리합니다.\n로그인 시 사용자에게 표시할 정지 사유를 입력해 주세요.",
            "관리자에 의해 이용이 제한되었습니다."
        );

        if (reason === null) {
            return;
        }

        if (reason.trim().length === 0) {
            alert("정지 사유를 입력해 주세요.");
            return;
        }

        Admin.submitPostFormFields("/admin/members/" + encodeURIComponent(memberNo) + "/ban", {
            banReason: reason.trim()
        });
    }

    function handleUnban(button) {
        const memberNo = button.dataset.memberNo;
        const memberId = button.dataset.memberId || memberNo;

        if (!memberNo) {
            return;
        }

        if (!confirm("회원 [" + memberId + "]의 정지를 해제하시겠습니까?")) {
            return;
        }

        Admin.submitPostForm("/admin/members/" + encodeURIComponent(memberNo) + "/unban");
    }

    Admin.MemberActions = {
        init: initMemberActions
    };
})(window, document);
