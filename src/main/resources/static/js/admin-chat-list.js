let adminListSocket = null;
let refreshTimer = null;
let lastRefreshRoomNo = null;

const roomListEl = document.getElementById("roomList");
const paginationEl = document.getElementById("pagination");
const checkAllClosed = document.getElementById("checkAllClosed");
const batchDeleteBtn = document.getElementById("batchDeleteBtn");

connectAdminListWebSocket();

function connectAdminListWebSocket() {
    adminListSocket = new WebSocket("ws://localhost:8080/ws/chat");

    adminListSocket.onopen = function () {
        console.log("관리자 상담 목록 WebSocket 연결 성공");

        adminListSocket.send(JSON.stringify({
            type: "ADMIN_LIST_JOIN",
            viewerNo: adminViewerNo
        }));
    };

    adminListSocket.onmessage = function (event) {
        const data = JSON.parse(event.data);

        if (data.type === "ADMIN_ROOM_REFRESH") {
            lastRefreshRoomNo = data.roomNo;

            showRealtimeToast("상담 목록이 갱신되었습니다.");
            requestAdminListRefresh();
        }
    };

    adminListSocket.onclose = function () {
        console.log("관리자 상담 목록 WebSocket 연결 종료");
    };

    adminListSocket.onerror = function (error) {
        console.log("관리자 상담 목록 WebSocket 에러", error);
    };
}

function requestAdminListRefresh() {
    if (refreshTimer !== null) {
        clearTimeout(refreshTimer);
    }

    refreshTimer = setTimeout(function () {
        loadAdminRoomList();
    }, 300);
}

function loadAdminRoomList() {
    const queryString = window.location.search;
    const url = "/chat/admin/rooms" + queryString;

    fetch(url)
        .then(response => response.json())
        .then(data => {
            renderRoomList(data.roomList || []);
            renderPagination(data);

            if (lastRefreshRoomNo !== null) {
                highlightRoom(lastRefreshRoomNo);
                lastRefreshRoomNo = null;
            }
        })
        .catch(error => {
            console.log("관리자 상담 목록 갱신 실패", error);
        });
}

function renderRoomList(roomList) {
    if (!roomListEl) {
        return;
    }

    if (roomList.length === 0) {
        roomListEl.innerHTML = `
            <div class="empty">
                조건에 맞는 채팅방이 없습니다.
            </div>
        `;

        updateBatchDeleteButton();
        return;
    }

    let html = "";

    roomList.forEach(room => {
        const roomNo = room.roomNo;
        const categoryIcon = getCategoryIcon(room.category);
        const categoryName = getCategoryName(room.category);
        const statusText = room.status === "OPEN" ? "진행중" : "종료";
        const statusClass = room.status === "OPEN" ? "open" : "closed";
        const closedClass = room.status === "CLOSED" ? "closed" : "";
        const userText = room.userNo !== null && room.userNo !== undefined ? room.userNo : "탈퇴 회원";
        const adminText = room.adminNo !== null && room.adminNo !== undefined ? room.adminNo : "미배정";
        const lastMessage = room.lastMessage ? room.lastMessage : "아직 메시지가 없습니다.";
        const unreadCount = room.unreadCount || 0;

        html += `
            <div class="room-card ${closedClass}">
                <div class="room-select">
                    ${
            room.status === "CLOSED"
                ? `<input type="checkbox"
                                      class="room-check"
                                      name="roomNoList"
                                      value="${escapeHtml(roomNo)}"
                                      form="batchDeleteForm">`
                : ""
        }
                </div>

                <a class="room-item ${closedClass}"
                   data-room-no="${escapeHtml(roomNo)}"
                   href="/chat/admin/${escapeHtml(roomNo)}">

                    <div class="top-line">
                        <span class="category">
                            <span>${categoryIcon}</span>
                            <span>${categoryName}</span>
                        </span>

                        <span class="room-title">
                            채팅방 #<span>${escapeHtml(roomNo)}</span>
                        </span>

                        ${
            unreadCount > 0
                ? `<span class="unread-badge">안읽음 ${escapeHtml(unreadCount)}</span>`
                : ""
        }

                        <span class="status ${statusClass}">
                            ${statusText}
                        </span>
                    </div>

                    <div class="meta">
                        사용자 번호:
                        <span>${escapeHtml(userText)}</span>
                        /
                        관리자 번호:
                        <span>${escapeHtml(adminText)}</span>
                    </div>

                    <div class="last-message">
                        마지막 메시지:
                        <span>${escapeHtml(lastMessage)}</span>
                    </div>
                </a>

                <div class="room-actions">
                    ${
            room.status === "CLOSED"
                ? `
                                <form action="/chat/admin/${escapeHtml(roomNo)}/delete"
                                      method="post"
                                      onsubmit="return confirm('상담방과 메시지가 모두 삭제됩니다. 정말 삭제하시겠습니까?');">
                                    <button type="submit" class="inline-delete-btn">삭제</button>
                                </form>
                              `
                : ""
        }
                </div>
            </div>
        `;
    });

    roomListEl.innerHTML = html;

    updateBatchDeleteButton();
}

function renderPagination(data) {
    if (!paginationEl) {
        return;
    }

    const totalPage = Number(data.totalPage || 1);
    const page = Number(data.page || 1);
    const size = Number(data.size || 10);
    const startPageNo = Number(data.startPageNo || 1);
    const endPageNo = Number(data.endPageNo || 1);

    if (totalPage <= 1) {
        paginationEl.innerHTML = "";
        return;
    }

    let html = "";

    html += `
        <a class="page-link ${page <= 1 ? "disabled" : ""}"
           href="${makePageUrl(page - 1, size)}">
            이전
        </a>
    `;

    for (let pageNum = startPageNo; pageNum <= endPageNo; pageNum++) {
        html += `
            <a class="page-link ${pageNum === page ? "active" : ""}"
               href="${makePageUrl(pageNum, size)}">
                ${pageNum}
            </a>
        `;
    }

    html += `
        <a class="page-link ${page >= totalPage ? "disabled" : ""}"
           href="${makePageUrl(page + 1, size)}">
            다음
        </a>
    `;

    paginationEl.innerHTML = html;
}

function makePageUrl(page, size) {
    const params = new URLSearchParams(window.location.search);

    params.set("page", page);
    params.set("size", size);

    return "/chat/admin?" + params.toString();
}

function highlightRoom(roomNo) {
    const target = document.querySelector(`.room-item[data-room-no="${roomNo}"]`);

    if (!target) {
        return;
    }

    target.classList.add("realtime-highlight");

    setTimeout(function () {
        target.classList.remove("realtime-highlight");
    }, 1300);
}

function showRealtimeToast(message) {
    let toast = document.querySelector(".realtime-toast");

    if (!toast) {
        toast = document.createElement("div");
        toast.classList.add("realtime-toast");
        document.body.appendChild(toast);
    }

    toast.textContent = message;
    toast.classList.add("show");

    setTimeout(function () {
        toast.classList.remove("show");
    }, 1800);
}

function getCategoryName(category) {
    if (category === "MAIL") {
        return "메일 문의";
    }

    if (category === "MAP") {
        return "지도 문의";
    }

    if (category === "STOCK") {
        return "증권 문의";
    }

    if (category === "NEWS") {
        return "뉴스 문의";
    }

    if (category === "WEATHER") {
        return "날씨 문의";
    }

    if (category === "CALENDAR") {
        return "캘린더 문의";
    }

    if (category === "ETC") {
        return "기타 문의";
    }

    return "일반 문의";
}

function getCategoryIcon(category) {
    if (category === "MAIL") {
        return "📧";
    }

    if (category === "MAP") {
        return "🗺";
    }

    if (category === "STOCK") {
        return "📈";
    }

    if (category === "NEWS") {
        return "📰";
    }

    if (category === "WEATHER") {
        return "🌤";
    }

    if (category === "CALENDAR") {
        return "📅";
    }

    if (category === "ETC") {
        return "💬";
    }

    return "💬";
}

function escapeHtml(value) {
    if (value === null || value === undefined) {
        return "";
    }

    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

if (checkAllClosed) {
    checkAllClosed.addEventListener("change", function () {
        const roomChecks = document.querySelectorAll(".room-check");

        roomChecks.forEach(check => {
            check.checked = checkAllClosed.checked;
        });

        updateBatchDeleteButton();
    });
}

document.addEventListener("change", function (event) {
    if (event.target.classList.contains("room-check")) {
        updateBatchDeleteButton();
        syncCheckAllState();
    }
});

function updateBatchDeleteButton() {
    if (!batchDeleteBtn) {
        return;
    }

    const checkedList = document.querySelectorAll(".room-check:checked");

    batchDeleteBtn.disabled = checkedList.length === 0;
}

function syncCheckAllState() {
    if (!checkAllClosed) {
        return;
    }

    const roomChecks = document.querySelectorAll(".room-check");

    if (roomChecks.length === 0) {
        checkAllClosed.checked = false;
        return;
    }

    const checkedList = document.querySelectorAll(".room-check:checked");

    checkAllClosed.checked = roomChecks.length === checkedList.length;
}

function confirmBatchDelete() {
    const checkedList = document.querySelectorAll(".room-check:checked");

    if (checkedList.length === 0) {
        alert("삭제할 종료 상담방을 선택해주세요.");
        return false;
    }

    return confirm(`선택한 종료 상담방 ${checkedList.length}개를 삭제하시겠습니까?\n삭제된 상담방과 메시지는 복구할 수 없습니다.`);
}