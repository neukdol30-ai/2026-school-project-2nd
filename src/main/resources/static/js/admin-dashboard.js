/*
 * admin-dashboard.js
 * 사용 위치: templates/admin/dashboard.html
 * 역할:
 * 1. 최근 7일 상담 추이 막대 높이 계산
 * 2. 카테고리별 상담 통계 progress bar 계산
 * 3. 관리자 프로필/회원 상세 모달 제어
 * 4. 회원관리 전체선택/부분선택 UI 제어
 * 5. 상담/회원 삭제 요청 폼 생성
 */
(function () {
    document.addEventListener("DOMContentLoaded", function () {
        renderDailyChatTrend();
        renderCategoryProgress();
        initModalEvents();
        initMemberSelection();
        initChatSelection();
        initMemberDetailModal();
        initDeleteActions();
    });

    function renderDailyChatTrend() {
        const bars = Array.from(document.querySelectorAll("#dailyChatTrend .trend-bar"));

        if (bars.length === 0) {
            return;
        }

        const counts = bars.map(function (bar) {
            return Number(bar.dataset.count || 0);
        });

        const maxCount = Math.max(...counts, 1);

        bars.forEach(function (bar) {
            const count = Number(bar.dataset.count || 0);
            const height = count === 0 ? 10 : Math.max(28, Math.round((count / maxCount) * 180));
            bar.style.height = height + "px";
        });
    }

    function renderCategoryProgress() {
        const progressBars = Array.from(document.querySelectorAll("#categoryStats .category-progress span"));

        if (progressBars.length === 0) {
            return;
        }

        const counts = progressBars.map(function (bar) {
            return Number(bar.dataset.count || 0);
        });

        const maxCount = Math.max(...counts, 1);

        progressBars.forEach(function (bar) {
            const count = Number(bar.dataset.count || 0);
            const width = count === 0 ? 3 : Math.max(8, Math.round((count / maxCount) * 100));
            bar.style.width = width + "%";
        });
    }

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

    function initMemberSelection() {
        const rowCheckboxes = Array.from(document.querySelectorAll(".member-row-checkbox"));
        const selectableCheckboxes = rowCheckboxes.filter(function (checkbox) {
            return !checkbox.disabled;
        });
        const headerCheckbox = document.getElementById("memberSelectAllCheckbox");
        const selectAllButton = document.getElementById("selectAllMembers");
        const clearButton = document.getElementById("clearMemberSelection");
        const selectedCount = document.getElementById("selectedMemberCount");

        if (selectableCheckboxes.length === 0) {
            updateSelectedCount();
            return;
        }

        if (headerCheckbox) {
            headerCheckbox.addEventListener("change", function () {
                selectableCheckboxes.forEach(function (checkbox) {
                    checkbox.checked = headerCheckbox.checked;
                });
                updateSelectedCount();
            });
        }

        if (selectAllButton) {
            selectAllButton.addEventListener("click", function () {
                selectableCheckboxes.forEach(function (checkbox) {
                    checkbox.checked = true;
                });
                if (headerCheckbox) {
                    headerCheckbox.checked = true;
                }
                updateSelectedCount();
            });
        }

        if (clearButton) {
            clearButton.addEventListener("click", function () {
                selectableCheckboxes.forEach(function (checkbox) {
                    checkbox.checked = false;
                });
                if (headerCheckbox) {
                    headerCheckbox.checked = false;
                }
                updateSelectedCount();
            });
        }

        selectableCheckboxes.forEach(function (checkbox) {
            checkbox.addEventListener("change", function () {
                if (headerCheckbox) {
                    headerCheckbox.checked = selectableCheckboxes.length > 0 && selectableCheckboxes.every(function (item) {
                        return item.checked;
                    });
                }
                updateSelectedCount();
            });
        });

        updateSelectedCount();

        function updateSelectedCount() {
            if (!selectedCount) {
                return;
            }

            const count = selectableCheckboxes.filter(function (checkbox) {
                return checkbox.checked;
            }).length;

            selectedCount.textContent = "선택 " + count + "명";
        }
    }

    function initChatSelection() {
        const rowCheckboxes = Array.from(document.querySelectorAll(".chat-row-checkbox"));
        const selectableCheckboxes = rowCheckboxes.filter(function (checkbox) {
            return !checkbox.disabled;
        });
        const headerCheckbox = document.getElementById("chatSelectAllCheckbox");
        const selectAllButton = document.getElementById("selectAllChats");
        const clearButton = document.getElementById("clearChatSelection");
        const selectedCount = document.getElementById("selectedChatCount");

        if (selectableCheckboxes.length === 0) {
            updateSelectedCount();
            return;
        }

        if (headerCheckbox) {
            headerCheckbox.addEventListener("change", function () {
                selectableCheckboxes.forEach(function (checkbox) {
                    checkbox.checked = headerCheckbox.checked;
                });
                updateSelectedCount();
            });
        }

        if (selectAllButton) {
            selectAllButton.addEventListener("click", function () {
                selectableCheckboxes.forEach(function (checkbox) {
                    checkbox.checked = true;
                });
                if (headerCheckbox) {
                    headerCheckbox.checked = true;
                }
                updateSelectedCount();
            });
        }

        if (clearButton) {
            clearButton.addEventListener("click", function () {
                selectableCheckboxes.forEach(function (checkbox) {
                    checkbox.checked = false;
                });
                if (headerCheckbox) {
                    headerCheckbox.checked = false;
                }
                updateSelectedCount();
            });
        }

        selectableCheckboxes.forEach(function (checkbox) {
            checkbox.addEventListener("change", function () {
                if (headerCheckbox) {
                    headerCheckbox.checked = selectableCheckboxes.length > 0 && selectableCheckboxes.every(function (item) {
                        return item.checked;
                    });
                }
                updateSelectedCount();
            });
        });

        updateSelectedCount();

        function updateSelectedCount() {
            if (!selectedCount) {
                return;
            }

            const count = selectableCheckboxes.filter(function (checkbox) {
                return checkbox.checked;
            }).length;

            selectedCount.textContent = "선택 " + count + "건";
        }
    }

    function initDeleteActions() {
        const deleteSelectedMembersButton = document.getElementById("deleteSelectedMembers");
        const deleteSelectedChatsButton = document.getElementById("deleteSelectedChats");

        if (deleteSelectedMembersButton) {
            deleteSelectedMembersButton.addEventListener("click", function () {
                const selectedValues = getCheckedValues(".member-row-checkbox");

                if (selectedValues.length === 0) {
                    alert("삭제할 회원을 선택해 주세요.");
                    return;
                }

                if (!confirm("선택한 회원 " + selectedValues.length + "명을 삭제하시겠습니까?\n현재 로그인 중인 관리자 본인은 삭제되지 않습니다.")) {
                    return;
                }

                submitPostForm("/admin/members/delete", "memberNoList", selectedValues);
            });
        }

        if (deleteSelectedChatsButton) {
            deleteSelectedChatsButton.addEventListener("click", function () {
                const selectedValues = getCheckedValues(".chat-row-checkbox");

                if (selectedValues.length === 0) {
                    alert("삭제할 종료 상담을 선택해 주세요. 진행 중 상담은 먼저 종료해야 삭제할 수 있습니다.");
                    return;
                }

                if (!confirm("선택한 종료 상담 " + selectedValues.length + "건을 삭제하시겠습니까?")) {
                    return;
                }

                submitPostForm("/admin/chats/delete", "roomNoList", selectedValues);
            });
        }

        document.querySelectorAll("[data-member-delete-button]").forEach(function (button) {
            button.addEventListener("click", function () {
                const memberNo = button.dataset.memberNo;
                const memberId = button.dataset.memberId || memberNo;

                if (!memberNo) {
                    return;
                }

                if (!confirm("회원 [" + memberId + "]을 삭제하시겠습니까?")) {
                    return;
                }

                submitPostForm("/admin/members/" + encodeURIComponent(memberNo) + "/delete");
            });
        });

        document.querySelectorAll("[data-chat-delete-button]").forEach(function (button) {
            button.addEventListener("click", function () {
                const roomNo = button.dataset.roomNo;

                if (!roomNo) {
                    return;
                }

                if (!confirm("종료된 상담방 #" + roomNo + "번을 삭제하시겠습니까?")) {
                    return;
                }

                submitPostForm("/admin/chats/" + encodeURIComponent(roomNo) + "/delete");
            });
        });
    }

    function getCheckedValues(selector) {
        return Array.from(document.querySelectorAll(selector + ":checked"))
            .filter(function (checkbox) {
                return !checkbox.disabled;
            })
            .map(function (checkbox) {
                return checkbox.value;
            })
            .filter(Boolean);
    }

    function submitPostForm(action, fieldName, values) {
        const form = document.createElement("form");
        form.method = "post";
        form.action = action;
        form.style.display = "none";

        if (fieldName && Array.isArray(values)) {
            values.forEach(function (value) {
                const input = document.createElement("input");
                input.type = "hidden";
                input.name = fieldName;
                input.value = value;
                form.appendChild(input);
            });
        }

        document.body.appendChild(form);
        form.submit();
    }

    function initMemberDetailModal() {
        const buttons = Array.from(document.querySelectorAll("[data-member-detail-button]"));

        buttons.forEach(function (button) {
            button.addEventListener("click", function () {
                setText("memberModalNo", button.dataset.memberNo || "-");
                setText("memberModalId", button.dataset.memberId || "-");
                setText("memberModalName", button.dataset.memberName || "-");
                setText("memberModalNickname", button.dataset.memberNickname || "-");
                setText("memberModalEmail", button.dataset.memberEmail || "미등록");
                setText("memberModalPhone", button.dataset.memberPhone || "미등록");
                setText("memberModalRole", button.dataset.memberRole || "-");
                setText("memberModalRegdate", button.dataset.memberRegdate || "-");
                setText("memberModalTitle", button.dataset.memberId || "회원 상세");
                setText("memberModalSubtitle", button.dataset.memberEmail || "회원 정보를 확인합니다.");

                const avatar = document.getElementById("memberModalAvatar");
                if (avatar) {
                    const nickname = button.dataset.memberNickname || button.dataset.memberId || "U";
                    avatar.textContent = nickname.substring(0, 1).toUpperCase();
                }

                openModal("memberDetailModal");
            });
        });
    }

    function setText(id, value) {
        const target = document.getElementById(id);
        if (target) {
            target.textContent = value;
        }
    }
})();
