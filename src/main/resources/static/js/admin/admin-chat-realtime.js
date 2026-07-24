/*
 * admin-chat-realtime.js
 * /admin?view=chats 상담 목록 실시간 갱신 담당.
 */
(function (window, document) {
    const Admin = window.SecondProAdmin = window.SecondProAdmin || {};
    let adminListSocket = null;
    let refreshTimer = null;

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
            Admin.BrowserNotification?.notifyChatEvent(data.roomNo);
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
                Admin.Selection.resetChatSelection();

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
            const createdDate = Admin.formatDateTime(room.createdDate);

            return ''
                + '<tr class="realtime-chat-row">'
                + '  <td>'
                + '    <input type="checkbox" class="chat-row-checkbox" value="' + Admin.escapeHtml(room.roomNo) + '" '
                + (closed ? 'title="삭제 선택"' : 'disabled title="진행 중 상담은 종료 후 삭제할 수 있습니다."')
                + ' aria-label="상담 선택">'
                + '  </td>'
                + '  <td>#' + Admin.escapeHtml(room.roomNo) + '</td>'
                + '  <td><span>' + Admin.escapeHtml(categoryIcon) + '</span> <span>' + Admin.escapeHtml(categoryName) + '</span></td>'
                + '  <td>' + Admin.escapeHtml(userNickname) + '</td>'
                + '  <td><span class="status-pill ' + (room.status === "OPEN" ? "open" : "closed") + '">' + Admin.escapeHtml(statusName) + '</span></td>'
                + '  <td>' + Admin.escapeHtml(room.unreadCount == null ? 0 : room.unreadCount) + '</td>'
                + '  <td class="ellipsis">' + Admin.escapeHtml(lastMessage) + '</td>'
                + '  <td>' + Admin.escapeHtml(createdDate) + '</td>'
                + '  <td>'
                + '    <div class="table-actions-inline">'
                + '      <a class="table-action" href="/admin/chats/' + encodeURIComponent(room.roomNo) + '">상담 열기</a>'
                + (closed
                    ? '      <button type="button" class="table-action danger-action button-like" data-chat-delete-button data-room-no="' + Admin.escapeHtml(room.roomNo) + '">삭제</button>'
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

        Admin.setText("metricTotalMembers", dashboard.totalMemberCount ?? 0);
        Admin.setText("metricTotalBoards", dashboard.totalBoardCount ?? 0);
        Admin.setText("metricOpenChats", dashboard.openChatRoomCount ?? 0);
        Admin.setText("metricUnreadChats", dashboard.unreadChatRoomCount ?? 0);
        Admin.setText("metricClosedChats", dashboard.closedChatRoomCount ?? 0);
        Admin.setText("metricTodayVisits", dashboard.todayVisitCount ?? 0);
        Admin.setText("metricTodayChatRooms", dashboard.todayChatRoomCount ?? 0);
        Admin.setText("metricTodayChatText", "오늘 접수 " + (dashboard.todayChatRoomCount ?? 0) + "건");
        Admin.setText("metricTotalChatText", "전체 상담 " + (dashboard.totalChatRoomCount ?? 0) + "건");
    }

    function updateChatResultCount(totalCount) {
        Admin.setText("adminChatResultCount", "검색 결과 " + totalCount + "건");
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
        Admin.setText("adminChatRealtimeMessage", message);
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

    Admin.ChatRealtime = {
        init: function () {
            initAdminChatRealtime();
            initChatPaginationEvents();
        },
        refresh: refreshAdminChatRooms
    };
})(window, document);
