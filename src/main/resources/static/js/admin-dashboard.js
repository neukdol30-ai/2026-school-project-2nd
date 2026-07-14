/*
 * admin-dashboard.js
 * 사용 위치: templates/admin/dashboard.html
 * 역할:
 * 1. 최근 7일 상담 추이 막대 높이 계산
 * 2. 카테고리별 상담 통계 progress bar 계산
 * 3. 관리자 프로필/회원 상세 모달 제어
 * 4. 회원/상담 선택 UI와 삭제 요청 처리
 * 5. /admin?view=chats 관리자 콘솔 내부 상담 목록 실시간 갱신
 *
 * 유지보수 기준:
 * - 관리자 콘솔 상담 목록 갱신은 /admin/chats/rooms API만 사용합니다.
 * - /chat/admin/rooms API를 호출하지 않아 /chat 영역과 /admin 영역을 분리합니다.
 * - WebSocket은 기존 채팅 인프라(/ws/chat)를 사용하되, 화면 갱신은 관리자 전용 API로 처리합니다.
 */
(function () {
    let adminListSocket = null;
    let refreshTimer = null;

    document.addEventListener("DOMContentLoaded", function () {
        renderDailyChatTrend();
        renderCategoryProgress();
        initModalEvents();
        initSelectionControls();
        initMemberDetailModal();
        initDeleteActions();
        initAdminChatRealtime();
        initChatPaginationEvents();
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

    function initSelectionControls() {
        bindSelectionGroup({
            rowSelector: ".member-row-checkbox",
            headerId: "memberSelectAllCheckbox",
            selectAllId: "selectAllMembers",
            clearId: "clearMemberSelection",
            countId: "selectedMemberCount",
            label: "명"
        });

        bindSelectionGroup({
            rowSelector: ".chat-row-checkbox",
            headerId: "chatSelectAllCheckbox",
            selectAllId: "selectAllChats",
            clearId: "clearChatSelection",
            countId: "selectedChatCount",
            label: "건"
        });
    }

    function bindSelectionGroup(options) {
        const headerCheckbox = document.getElementById(options.headerId);
        const selectAllButton = document.getElementById(options.selectAllId);
        const clearButton = document.getElementById(options.clearId);

        if (headerCheckbox) {
            headerCheckbox.addEventListener("change", function () {
                getSelectableCheckboxes(options.rowSelector).forEach(function (checkbox) {
                    checkbox.checked = headerCheckbox.checked;
                });
                updateSelectedCount(options);
            });
        }

        if (selectAllButton) {
            selectAllButton.addEventListener("click", function () {
                const selectableCheckboxes = getSelectableCheckboxes(options.rowSelector);
                selectableCheckboxes.forEach(function (checkbox) {
                    checkbox.checked = true;
                });
                if (headerCheckbox) {
                    headerCheckbox.checked = selectableCheckboxes.length > 0;
                }
                updateSelectedCount(options);
            });
        }

        if (clearButton) {
            clearButton.addEventListener("click", function () {
                getSelectableCheckboxes(options.rowSelector).forEach(function (checkbox) {
                    checkbox.checked = false;
                });
                if (headerCheckbox) {
                    headerCheckbox.checked = false;
                }
                updateSelectedCount(options);
            });
        }

        document.addEventListener("change", function (event) {
            if (!event.target.matches(options.rowSelector)) {
                return;
            }

            const selectableCheckboxes = getSelectableCheckboxes(options.rowSelector);

            if (headerCheckbox) {
                headerCheckbox.checked = selectableCheckboxes.length > 0 && selectableCheckboxes.every(function (item) {
                    return item.checked;
                });
            }

            updateSelectedCount(options);
        });

        updateSelectedCount(options);
    }

    function getSelectableCheckboxes(selector) {
        return Array.from(document.querySelectorAll(selector)).filter(function (checkbox) {
            return !checkbox.disabled;
        });
    }

    function updateSelectedCount(options) {
        const selectedCount = document.getElementById(options.countId);

        if (!selectedCount) {
            return;
        }

        const count = getSelectableCheckboxes(options.rowSelector).filter(function (checkbox) {
            return checkbox.checked;
        }).length;

        selectedCount.textContent = "선택 " + count + options.label;
    }

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
        const selectedValues = getCheckedValues(".member-row-checkbox");

        if (selectedValues.length === 0) {
            alert("삭제할 회원을 선택해 주세요.");
            return;
        }

        if (!confirm("선택한 회원 " + selectedValues.length + "명을 삭제하시겠습니까?\n현재 로그인 중인 관리자 본인은 삭제되지 않습니다.")) {
            return;
        }

        submitPostForm("/admin/members/delete", "memberNoList", selectedValues);
    }

    function handleDeleteSelectedChats() {
        const selectedValues = getCheckedValues(".chat-row-checkbox");

        if (selectedValues.length === 0) {
            alert("삭제할 종료 상담을 선택해 주세요. 진행 중 상담은 먼저 종료해야 삭제할 수 있습니다.");
            return;
        }

        if (!confirm("선택한 종료 상담 " + selectedValues.length + "건을 삭제하시겠습니까?")) {
            return;
        }

        submitPostForm("/admin/chats/delete", "roomNoList", selectedValues);
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

        submitPostForm("/admin/members/" + encodeURIComponent(memberNo) + "/delete");
    }

    function handleDeleteOneChat(button) {
        const roomNo = button.dataset.roomNo;

        if (!roomNo) {
            return;
        }

        if (!confirm("종료된 상담방 #" + roomNo + "번을 삭제하시겠습니까?")) {
            return;
        }

        submitPostForm("/admin/chats/" + encodeURIComponent(roomNo) + "/delete");
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
        document.addEventListener("click", function (event) {
            const button = event.target.closest("[data-member-detail-button]");

            if (!button) {
                return;
            }

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
    }

    function initAdminChatRealtime() {
        const chatView = document.getElementById("chatManageView");

        if (!chatView) {
            return;
        }

        const protocol = window.location.protocol === "https:" ? "wss" : "ws";
        const socketUrl = protocol + "://" + window.location.host + "/ws/chat";

        try {
            adminListSocket = new WebSocket(socketUrl);
        } catch (error) {
            setChatLiveStatus("error", "실시간 연결 실패");
            return;
        }

        adminListSocket.onopen = function () {
            setChatLiveStatus("connected", "실시간 연결됨");
            adminListSocket.send(JSON.stringify({ type: "ADMIN_LIST_JOIN" }));
        };

        adminListSocket.onmessage = function (event) {
            let data;

            try {
                data = JSON.parse(event.data);
            } catch (error) {
                return;
            }

            if (data.type !== "ADMIN_ROOM_REFRESH") {
                return;
            }

            setChatLiveStatus("syncing", "새 상담 반영 중");
            setRealtimeMessage("새 상담 또는 새 메시지가 도착해 목록을 갱신합니다.");
            scheduleAdminChatRefresh();
        };

        adminListSocket.onclose = function () {
            setChatLiveStatus("closed", "실시간 연결 종료");
        };

        adminListSocket.onerror = function () {
            setChatLiveStatus("error", "실시간 연결 오류");
        };

        window.addEventListener("beforeunload", function () {
            if (adminListSocket && adminListSocket.readyState === WebSocket.OPEN) {
                adminListSocket.close();
            }
        });
    }

    function scheduleAdminChatRefresh() {
        if (refreshTimer) {
            clearTimeout(refreshTimer);
        }

        refreshTimer = setTimeout(function () {
            refreshAdminChatRooms();
        }, 250);
    }

    function refreshAdminChatRooms(targetPage) {
        const chatView = document.getElementById("chatManageView");

        if (!chatView) {
            return;
        }

        const params = getCurrentChatParams(targetPage);

        fetch("/admin/chats/rooms?" + params.toString(), {
            headers: {
                "Accept": "application/json"
            }
        })
            .then(function (response) {
                if (!response.ok) {
                    throw new Error("HTTP " + response.status);
                }

                return response.json();
            })
            .then(function (data) {
                if (!data.success) {
                    setChatLiveStatus("error", data.message || "상담 목록 갱신 실패");
                    return;
                }

                chatView.dataset.chatPage = String(data.chatPage || 1);
                chatView.dataset.chatSize = String(data.chatSize || 10);

                renderAdminChatRows(data.roomList || []);
                renderAdminChatPagination(data);
                updateAdminDashboardMetrics(data.dashboard);
                updateChatResultCount(data.chatTotalCount || 0);
                resetChatSelection();

                setChatLiveStatus("connected", "실시간 동기화 완료");
                setRealtimeMessage("방금 상담 목록을 최신 상태로 갱신했습니다.");
                flashChatTable();
            })
            .catch(function (error) {
                console.log("관리자 상담 목록 갱신 실패", error);
                setChatLiveStatus("error", "목록 갱신 실패");
                setRealtimeMessage("상담 목록 갱신 중 오류가 발생했습니다. 새로고침 후 다시 확인해 주세요.");
            });
    }

    function getCurrentChatParams(targetPage) {
        const chatView = document.getElementById("chatManageView");
        const form = document.querySelector("#chatManageView .admin-filter-form");
        const params = new URLSearchParams();

        params.set("chatStatus", getFormValue(form, "chatStatus", chatView?.dataset.chatStatus || ""));
        params.set("chatCategory", getFormValue(form, "chatCategory", chatView?.dataset.chatCategory || ""));
        params.set("chatKeyword", getFormValue(form, "chatKeyword", chatView?.dataset.chatKeyword || ""));
        params.set("chatPage", String(targetPage || chatView?.dataset.chatPage || 1));
        params.set("chatSize", String(chatView?.dataset.chatSize || 10));

        return params;
    }

    function getFormValue(form, name, fallback) {
        if (!form) {
            return fallback || "";
        }

        const field = form.querySelector("[name='" + name + "']");

        if (!field) {
            return fallback || "";
        }

        return field.value || "";
    }

    function renderAdminChatRows(roomList) {
        const tbody = document.getElementById("adminChatTableBody");

        if (!tbody) {
            return;
        }

        if (!roomList || roomList.length === 0) {
            tbody.innerHTML = '<tr><td colspan="9" class="empty-cell">조회된 상담이 없습니다.</td></tr>';
            return;
        }

        tbody.innerHTML = roomList.map(function (room) {
            const closed = room.status === "CLOSED";
            const statusName = room.statusName || (room.status === "OPEN" ? "진행 중" : "종료");
            const categoryIcon = room.categoryIcon || "💬";
            const categoryName = room.categoryName || "일반 문의";
            const userNickname = room.userNickname || "탈퇴/미지정";
            const lastMessage = room.lastMessage && String(room.lastMessage).trim() !== "" ? room.lastMessage : "메시지 없음";
            const createdDate = formatDateTime(room.createdDate);

            return ''
                + '<tr class="realtime-chat-row">'
                + '  <td>'
                + '    <input type="checkbox" class="chat-row-checkbox" value="' + escapeHtml(room.roomNo) + '" '
                + (closed ? 'title="삭제 선택"' : 'disabled title="진행 중 상담은 종료 후 삭제할 수 있습니다."')
                + ' aria-label="상담 선택">'
                + '  </td>'
                + '  <td>#' + escapeHtml(room.roomNo) + '</td>'
                + '  <td><span>' + escapeHtml(categoryIcon) + '</span> <span>' + escapeHtml(categoryName) + '</span></td>'
                + '  <td>' + escapeHtml(userNickname) + '</td>'
                + '  <td><span class="status-pill ' + (room.status === "OPEN" ? "open" : "closed") + '">' + escapeHtml(statusName) + '</span></td>'
                + '  <td>' + escapeHtml(room.unreadCount == null ? 0 : room.unreadCount) + '</td>'
                + '  <td class="ellipsis">' + escapeHtml(lastMessage) + '</td>'
                + '  <td>' + escapeHtml(createdDate) + '</td>'
                + '  <td>'
                + '    <div class="table-actions-inline">'
                + '      <a class="table-action" href="/admin/chats/' + encodeURIComponent(room.roomNo) + '">상담 열기</a>'
                + (closed
                    ? '      <button type="button" class="table-action danger-action button-like" data-chat-delete-button data-room-no="' + escapeHtml(room.roomNo) + '">삭제</button>'
                    : '      <span class="table-muted-action">종료 후 삭제</span>')
                + '    </div>'
                + '  </td>'
                + '</tr>';
        }).join("");
    }

    function renderAdminChatPagination(data) {
        const pagination = document.getElementById("adminChatPagination");

        if (!pagination) {
            return;
        }

        const currentPage = Number(data.chatPage || 1);
        const totalPages = Number(data.chatTotalPages || 1);

        if (totalPages <= 1) {
            pagination.innerHTML = "";
            pagination.style.display = "none";
            return;
        }

        pagination.style.display = "flex";

        let html = "";

        if (currentPage > 1) {
            html += '<a href="' + buildAdminChatsUrl(currentPage - 1) + '" data-chat-page-link="' + (currentPage - 1) + '">이전</a>';
        }

        for (let pageNo = 1; pageNo <= totalPages; pageNo++) {
            html += '<a href="' + buildAdminChatsUrl(pageNo) + '" data-chat-page-link="' + pageNo + '" class="' + (pageNo === currentPage ? 'active' : '') + '">' + pageNo + '</a>';
        }

        if (currentPage < totalPages) {
            html += '<a href="' + buildAdminChatsUrl(currentPage + 1) + '" data-chat-page-link="' + (currentPage + 1) + '">다음</a>';
        }

        pagination.innerHTML = html;
    }

    function buildAdminChatsUrl(pageNo) {
        const params = getCurrentChatParams(pageNo);
        params.set("view", "chats");
        return "/admin?" + params.toString();
    }

    function initChatPaginationEvents() {
        document.addEventListener("click", function (event) {
            const link = event.target.closest("#adminChatPagination a");

            if (!link) {
                return;
            }

            const pageNo = Number(link.dataset.chatPageLink || 0);

            if (!pageNo) {
                return;
            }

            event.preventDefault();
            const chatView = document.getElementById("chatManageView");
            if (chatView) {
                chatView.dataset.chatPage = String(pageNo);
            }
            refreshAdminChatRooms(pageNo);
        });
    }

    function updateAdminDashboardMetrics(dashboard) {
        if (!dashboard) {
            return;
        }

        setText("metricTotalMembers", dashboard.totalMemberCount ?? 0);
        setText("metricTotalBoards", dashboard.totalBoardCount ?? 0);
        setText("metricOpenChats", dashboard.openChatRoomCount ?? 0);
        setText("metricUnreadChats", dashboard.unreadChatRoomCount ?? 0);
        setText("metricClosedChats", dashboard.closedChatRoomCount ?? 0);
        setText("metricTodayVisits", dashboard.todayVisitCount ?? 0);
        setText("metricTodayChatRooms", dashboard.todayChatRoomCount ?? 0);
        setText("metricTodayChatText", "오늘 접수 " + (dashboard.todayChatRoomCount ?? 0) + "건");
        setText("metricTotalChatText", "전체 상담 " + (dashboard.totalChatRoomCount ?? 0) + "건");
    }

    function updateChatResultCount(totalCount) {
        setText("adminChatResultCount", "검색 결과 " + totalCount + "건");
    }

    function resetChatSelection() {
        const headerCheckbox = document.getElementById("chatSelectAllCheckbox");
        if (headerCheckbox) {
            headerCheckbox.checked = false;
        }
        updateSelectedCount({
            rowSelector: ".chat-row-checkbox",
            countId: "selectedChatCount",
            label: "건"
        });
    }

    function setChatLiveStatus(status, message) {
        const target = document.getElementById("adminChatLiveStatus");

        if (!target) {
            return;
        }

        target.classList.remove("connected", "syncing", "closed", "error");
        target.classList.add(status);

        const label = target.querySelector("strong");
        if (label) {
            label.textContent = message;
        }
    }

    function setRealtimeMessage(message) {
        setText("adminChatRealtimeMessage", message);
    }

    function flashChatTable() {
        const tableWrap = document.querySelector("#chatManageView .admin-table-wrap");

        if (!tableWrap) {
            return;
        }

        tableWrap.classList.remove("realtime-updated");
        void tableWrap.offsetWidth;
        tableWrap.classList.add("realtime-updated");
    }

    function formatDateTime(value) {
        if (!value) {
            return "-";
        }

        let date;

        if (Array.isArray(value)) {
            date = new Date(
                Number(value[0]),
                Number(value[1]) - 1,
                Number(value[2]),
                Number(value[3] || 0),
                Number(value[4] || 0),
                Number(value[5] || 0)
            );
        } else {
            date = new Date(value);
        }

        if (Number.isNaN(date.getTime())) {
            return String(value);
        }

        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, "0");
        const day = String(date.getDate()).padStart(2, "0");
        const hour = String(date.getHours()).padStart(2, "0");
        const minute = String(date.getMinutes()).padStart(2, "0");

        return year + "-" + month + "-" + day + " " + hour + ":" + minute;
    }

    function escapeHtml(value) {
        if (value == null) {
            return "";
        }

        return String(value)
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#039;");
    }

    function setText(id, value) {
        const target = document.getElementById(id);
        if (target) {
            target.textContent = value;
        }
    }
})();
