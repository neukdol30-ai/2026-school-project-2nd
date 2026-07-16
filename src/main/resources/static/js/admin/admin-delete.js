/*
 * admin-delete.js
 * 관리자 콘솔 회원/상담 삭제 요청 담당.
 */
(function (window, document) {
    const Admin = window.SecondProAdmin = window.SecondProAdmin || {};

    function initDeleteActions() {
        document.addEventListener("click", function (event) {
            const deleteSelectedMembersButton = event.target.closest("#deleteSelectedMembers");
            const deleteSelectedChatsButton = event.target.closest("#deleteSelectedChats");
            const memberDeleteButton = event.target.closest("[data-member-delete-button]");
            const chatDeleteButton = event.target.closest("[data-chat-delete-button]");

            if (deleteSelectedMembersButton) {
                handleDeleteSelectedMembers();
                return;
            }

            if (deleteSelectedChatsButton) {
                handleDeleteSelectedChats();
                return;
            }

            if (memberDeleteButton) {
                handleDeleteOneMember(memberDeleteButton);
                return;
            }

            if (chatDeleteButton) {
                handleDeleteOneChat(chatDeleteButton);
            }
        });
    }

    function handleDeleteSelectedMembers() {
        const selectedValues = Admin.getCheckedValues(".member-row-checkbox");

        if (selectedValues.length === 0) {
            alert("삭제할 회원을 선택해 주세요.");
            return;
        }

        if (!confirm("선택한 회원 " + selectedValues.length + "명을 삭제하시겠습니까?\n현재 로그인 중인 관리자 본인은 삭제되지 않습니다.")) {
            return;
        }

        Admin.submitPostForm("/admin/members/delete", "memberNoList", selectedValues);
    }

    function handleDeleteSelectedChats() {
        const selectedValues = Admin.getCheckedValues(".chat-row-checkbox");

        if (selectedValues.length === 0) {
            alert("삭제할 종료 상담을 선택해 주세요. 진행 중 상담은 먼저 종료해야 삭제할 수 있습니다.");
            return;
        }

        if (!confirm("선택한 종료 상담 " + selectedValues.length + "건을 삭제하시겠습니까?")) {
            return;
        }

        Admin.submitPostForm("/admin/chats/delete", "roomNoList", selectedValues);
    }

    function handleDeleteOneMember(button) {
        const memberNo = button.dataset.memberNo;
        const memberId = button.dataset.memberId || memberNo;

        if (!memberNo) {
            return;
        }

        if (!confirm("회원 [" + memberId + "]을 삭제하시겠습니까?")) {
            return;
        }

        Admin.submitPostForm("/admin/members/" + encodeURIComponent(memberNo) + "/delete");
    }

    function handleDeleteOneChat(button) {
        const roomNo = button.dataset.roomNo;

        if (!roomNo) {
            return;
        }

        if (!confirm("종료된 상담방 #" + roomNo + "번을 삭제하시겠습니까?")) {
            return;
        }

        Admin.submitPostForm("/admin/chats/" + encodeURIComponent(roomNo) + "/delete");
    }

    Admin.Delete = {
        init: initDeleteActions
    };
})(window, document);
