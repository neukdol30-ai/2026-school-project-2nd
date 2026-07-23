/*
    chat.js

    사용 위치:
    - chat.html
    - admin-chat-room.html

    역할:
    - WebSocket 연결
    - 채팅방 JOIN 이벤트 전송
    - 메시지 전송
    - 메시지 실시간 수신
    - 읽음/안읽음 처리
    - 상담 종료 시 입력창 비활성화

    HTML에서 필요한 전역 변수:
    - roomNo
    - senderNo
    - loginRole
    - roomUserNo
    - roomStatus
*/

let socket = null;
let isChatClosed = typeof roomStatus !== "undefined" && roomStatus === "CLOSED";

const chatBody = document.getElementById("chatBody");
const messageInput = document.getElementById("messageInput");
const sendBtn = document.getElementById("sendBtn");

/*
    중복 렌더링 방지용 Set

    Redis 메시지와 DB 메시지를 함께 가져오거나,
    WebSocket 수신과 fetch 결과가 겹칠 때 같은 메시지가 두 번 출력되는 것을 방지한다.
*/
const renderedMessageKeys = new Set();

connectWebSocket();

/**
 * WebSocket 연결
 *
 * localhost 고정 대신 현재 접속한 host 기준으로 WebSocket 주소를 만든다.
 * 배포 환경이나 포트 변경 시에도 그대로 동작하게 하기 위함이다.
 */
function connectWebSocket() {
    const protocol = window.location.protocol === "https:" ? "wss" : "ws";
    socket = new WebSocket(`${protocol}://${window.location.host}/ws/chat`);

    socket.onopen = function () {
        console.log("WebSocket 연결 성공");

        socket.send(JSON.stringify({
            type: "JOIN",
            roomNo: roomNo,
            viewerNo: senderNo
        }));

        loadMessages();
    };

    socket.onmessage = function (event) {
        const data = JSON.parse(event.data);

        if (data.type === "MESSAGE") {
            appendMessage(data.message);

            // 상대방 메시지를 받으면 읽음 이벤트 전송
            if (Number(data.message.senderNo) !== Number(senderNo)) {
                sendReadEvent();
            }

            return;
        }

        if (data.type === "READ") {
            // 상대방이 읽었을 때 내가 보낸 메시지를 읽음으로 변경
            if (Number(data.viewerNo) !== Number(senderNo)) {
                markMyMessagesAsRead();
            }

            return;
        }

        if (data.type === "CLOSE") {
            isChatClosed = true;

            if (data.message) {
                appendSystemMessage(data.message.messageContent, data.message.createdDate);
            } else {
                appendSystemMessage("상담이 종료되었습니다.", null);
            }

            disableChatInput();
        }
    };

    socket.onclose = function () {
        console.log("WebSocket 연결 종료");
    };

    socket.onerror = function (error) {
        console.log("WebSocket 에러", error);
    };
}

/**
 * 메시지 목록 조회
 *
 * /chat/{roomNo}/messages API는 Oracle DB 메시지와 Redis 메시지를 합쳐서 반환한다.
 */
function loadMessages() {
    fetch(`/chat/${roomNo}/messages`)
        .then(response => response.json())
        .then(messages => {
            chatBody.innerHTML = "";
            renderedMessageKeys.clear();

            messages.sort((a, b) => {
                return getTimeValue(a.createdDate) - getTimeValue(b.createdDate);
            });

            messages.forEach(message => {
                appendMessage(message);
            });

            sendReadEvent();

            if (isChatClosed) {
                disableChatInput();
            }
        })
        .catch(error => {
            console.log("메시지 목록 조회 실패", error);
        });
}

/**
 * 메시지 전송
 *
 * 실제 저장은 WebSocket 서버의 ChatHandler에서 Redis에 저장한다.
 */
function sendMessage() {
    if (isChatClosed) {
        alert("상담이 종료되어 메시지를 보낼 수 없습니다.");
        return;
    }

    const content = messageInput.value.trim();

    if (content === "") {
        return;
    }

    if (socket === null || socket.readyState !== WebSocket.OPEN) {
        alert("채팅 서버와 연결 중입니다. 잠시 후 다시 시도해주세요.");
        return;
    }

    const message = {
        roomNo: roomNo,
        senderNo: senderNo,
        messageContent: content,
        readYn: "N"
    };

    socket.send(JSON.stringify({
        type: "MESSAGE",
        message: message
    }));

    messageInput.value = "";
    messageInput.focus();
}

/**
 * 읽음 이벤트 전송
 */
function sendReadEvent() {
    if (socket === null || socket.readyState !== WebSocket.OPEN) {
        return;
    }

    socket.send(JSON.stringify({
        type: "READ",
        roomNo: roomNo,
        viewerNo: senderNo
    }));
}

/**
 * 일반 메시지 화면 출력
 */
function appendMessage(message) {
    if (!message) {
        return;
    }

    const messageKey = makeMessageKey(message);

    if (renderedMessageKeys.has(messageKey)) {
        return;
    }

    renderedMessageKeys.add(messageKey);

    const row = document.createElement("div");
    row.classList.add("message-row");

    const isMine = Number(message.senderNo) === Number(senderNo);

    if (isMine) {
        row.classList.add("me");
    } else {
        row.classList.add("other");
    }

    const messageBox = document.createElement("div");
    messageBox.classList.add("message-box");

    const label = document.createElement("div");
    label.classList.add("message-label");
    label.textContent = getSenderLabel(message, isMine);

    const bubble = document.createElement("div");
    bubble.classList.add("message-bubble");
    bubble.textContent = message.messageContent;

    const meta = document.createElement("div");
    meta.classList.add("message-meta");

    const time = document.createElement("span");
    time.classList.add("message-time");
    time.textContent = formatMessageTime(message.createdDate);

    meta.appendChild(time);

    if (isMine) {
        const read = document.createElement("span");
        read.classList.add("read-status");
        read.textContent = message.readYn === "Y" ? "읽음" : "안읽음";
        meta.appendChild(read);
    }

    messageBox.appendChild(label);
    messageBox.appendChild(bubble);
    messageBox.appendChild(meta);

    row.appendChild(messageBox);
    chatBody.appendChild(row);

    scrollToBottom();
}

/**
 * 상담 종료 등 시스템 메시지 출력
 */
function appendSystemMessage(content, createdDate) {
    const row = document.createElement("div");
    row.classList.add("system-row");

    const box = document.createElement("div");
    box.classList.add("system-message");

    const text = document.createElement("div");
    text.textContent = content;

    const time = document.createElement("div");
    time.classList.add("system-time");
    time.textContent = formatMessageTime(createdDate);

    box.appendChild(text);

    if (time.textContent !== "") {
        box.appendChild(time);
    }

    row.appendChild(box);
    chatBody.appendChild(row);

    scrollToBottom();
}

/**
 * 메시지 작성자 라벨
 *
 * 내가 보낸 메시지는 라벨을 표시하지 않고,
 * 상대방 메시지는 사용자/관리자를 구분해서 표시한다.
 */
function getSenderLabel(message, isMine) {
    if (isMine) {
        return "";
    }

    const messageSenderNo = Number(message.senderNo);
    const userNo = Number(roomUserNo);

    if (messageSenderNo === userNo) {
        return "사용자";
    }

    return "관리자";
}

/**
 * 내가 보낸 메시지의 안읽음 표시를 읽음으로 변경
 */
function markMyMessagesAsRead() {
    const readStatusList = document.querySelectorAll(".message-row.me .read-status");

    readStatusList.forEach(readStatus => {
        readStatus.textContent = "읽음";
    });
}

/**
 * 상담 종료 시 입력창과 전송 버튼 비활성화
 */
function disableChatInput() {
    if (messageInput) {
        messageInput.disabled = true;
        messageInput.placeholder = "상담이 종료되었습니다.";
    }

    if (sendBtn) {
        sendBtn.disabled = true;
        sendBtn.style.background = "#999";
        sendBtn.style.cursor = "not-allowed";
    }
}

/**
 * 메시지 시간을 HH:mm 형식으로 변환
 */
function formatMessageTime(createdDate) {
    if (!createdDate) {
        return "";
    }

    const date = parseCreatedDate(createdDate);

    if (!date || isNaN(date.getTime())) {
        return "";
    }

    const hours = String(date.getHours()).padStart(2, "0");
    const minutes = String(date.getMinutes()).padStart(2, "0");

    return `${hours}:${minutes}`;
}

/**
 * Java LocalDateTime이 배열 또는 문자열로 올 수 있어 둘 다 처리한다.
 */
function parseCreatedDate(createdDate) {
    if (Array.isArray(createdDate)) {
        const year = createdDate[0];
        const month = createdDate[1] - 1;
        const day = createdDate[2];
        const hours = createdDate[3] || 0;
        const minutes = createdDate[4] || 0;
        const seconds = createdDate[5] || 0;

        return new Date(year, month, day, hours, minutes, seconds);
    }

    if (typeof createdDate === "string") {
        return new Date(createdDate);
    }

    return null;
}

function getTimeValue(createdDate) {
    const date = parseCreatedDate(createdDate);

    if (!date || isNaN(date.getTime())) {
        return 0;
    }

    return date.getTime();
}

/**
 * 메시지 중복 출력 방지용 key 생성
 */
function makeMessageKey(message) {
    const timeValue = getTimeValue(message.createdDate);

    return [
        message.roomNo,
        message.senderNo,
        message.messageContent,
        timeValue
    ].join("|");
}

function scrollToBottom() {
    chatBody.scrollTop = chatBody.scrollHeight;
}

if (sendBtn) {
    sendBtn.addEventListener("click", sendMessage);
}

if (messageInput) {
    messageInput.addEventListener("keyup", function (event) {
        if (event.key === "Enter") {
            sendMessage();
        }
    });
}

if (isChatClosed) {
    disableChatInput();
}