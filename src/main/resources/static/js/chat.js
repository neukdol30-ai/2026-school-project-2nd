/*
    chat.js

    사용 위치:
    - chat.html
    - admin/dashboard.html의 관리자 상담 상세 화면

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

/* 서버 ChatWebSocketEventType과 동일한 이벤트 이름을 사용합니다. */
const CHAT_WEBSOCKET_EVENT = Object.freeze({
    JOIN: "JOIN",
    READ: "READ",
    MESSAGE: "MESSAGE",
    CLOSE: "CLOSE",
    ERROR: "ERROR"
});

const chatBody = document.getElementById("chatBody");
const messageInput = document.getElementById("messageInput");
const sendBtn = document.getElementById("sendBtn");
const messageLengthCounter = document.getElementById("messageLengthCounter");
const messageInputStatus = document.getElementById("messageInputStatus");
const chatMessageMaxLength = Number(messageInput?.dataset.maxLength || 1000);

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
            type: CHAT_WEBSOCKET_EVENT.JOIN,
            roomNo: roomNo,
            viewerNo: senderNo
        }));

        loadMessages();
    };

    socket.onmessage = function (event) {
        let data;

        try {
            data = JSON.parse(event.data);
        } catch (error) {
            console.log("WebSocket 응답 해석 실패", error);
            return;
        }

        if (data.type === CHAT_WEBSOCKET_EVENT.ERROR) {
            setChatInputStatus(data.message || "메시지를 전송하지 못했습니다.", true);

            if (data.code === "CHAT_CLOSED") {
                isChatClosed = true;
                disableChatInput();
            }
            return;
        }

        if (data.type === CHAT_WEBSOCKET_EVENT.MESSAGE) {
            appendMessage(data.message);

            // 상대방 메시지를 받으면 읽음 이벤트 전송
            if (Number(data.message.senderNo) !== Number(senderNo)) {
                sendReadEvent();
            }

            return;
        }

        if (data.type === CHAT_WEBSOCKET_EVENT.READ) {
            // 상대방이 읽었을 때 내가 보낸 메시지를 읽음으로 변경
            if (Number(data.viewerNo) !== Number(senderNo)) {
                markMyMessagesAsRead();
            }

            return;
        }

        if (data.type === CHAT_WEBSOCKET_EVENT.CLOSE) {
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
    if (!messageInput) {
        return;
    }

    if (isChatClosed) {
        setChatInputStatus("상담이 종료되어 메시지를 보낼 수 없습니다.", true);
        return;
    }

    const content = messageInput.value.trim();
    const contentLength = getCharacterCount(content);

    if (content === "") {
        setChatInputStatus("메시지를 입력해 주세요.", true);
        return;
    }

    if (contentLength > chatMessageMaxLength) {
        setChatInputStatus(`메시지는 최대 ${chatMessageMaxLength.toLocaleString()}자까지 입력할 수 있습니다.`, true);
        return;
    }

    if (socket === null || socket.readyState !== WebSocket.OPEN) {
        setChatInputStatus("채팅 서버와 연결 중입니다. 잠시 후 다시 시도해 주세요.", true);
        return;
    }

    const message = {
        roomNo: roomNo,
        senderNo: senderNo,
        messageContent: content,
        readYn: "N"
    };

    socket.send(JSON.stringify({
        type: CHAT_WEBSOCKET_EVENT.MESSAGE,
        message: message
    }));

    messageInput.value = "";
    updateMessageLengthCounter();
    setChatInputStatus("");
    messageInput.focus();
}

/**
 * 사용자가 보는 글자 수와 서버의 Unicode 코드 포인트 계산 기준을 맞춥니다.
 */
function getCharacterCount(value) {
    return Array.from(value || "").length;
}

function truncateToMaxLength(value, maxLength) {
    const characters = Array.from(value || "");

    if (characters.length <= maxLength) {
        return value || "";
    }

    return characters.slice(0, maxLength).join("");
}

function updateMessageLengthCounter() {
    if (!messageInput || !messageLengthCounter) {
        return;
    }

    const currentLength = getCharacterCount(messageInput.value);
    messageLengthCounter.textContent = `${currentLength.toLocaleString()} / ${chatMessageMaxLength.toLocaleString()}`;
    messageLengthCounter.classList.toggle("limit-reached", currentLength >= chatMessageMaxLength);
}

function handleMessageInput() {
    if (!messageInput) {
        return;
    }

    const currentLength = getCharacterCount(messageInput.value);

    if (currentLength > chatMessageMaxLength) {
        messageInput.value = truncateToMaxLength(messageInput.value, chatMessageMaxLength);
        setChatInputStatus(`메시지는 최대 ${chatMessageMaxLength.toLocaleString()}자까지 입력할 수 있습니다.`, true);
    } else if (messageInputStatus?.dataset.validationError === "true") {
        setChatInputStatus("");
    }

    updateMessageLengthCounter();
}

function setChatInputStatus(message, isError = false) {
    if (!messageInputStatus) {
        if (message && isError) {
            alert(message);
        }
        return;
    }

    messageInputStatus.textContent = message || "";
    messageInputStatus.classList.toggle("error", Boolean(message) && isError);
    messageInputStatus.dataset.validationError = Boolean(message) && isError ? "true" : "false";
}

/**
 * 읽음 이벤트 전송
 */
function sendReadEvent() {
    if (socket === null || socket.readyState !== WebSocket.OPEN) {
        return;
    }

    socket.send(JSON.stringify({
        type: CHAT_WEBSOCKET_EVENT.READ,
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
    messageInput.addEventListener("input", handleMessageInput);
    messageInput.addEventListener("keydown", function (event) {
        // 한글 IME 조합 중 Enter가 전송으로 처리되는 문제를 방지합니다.
        if (event.key === "Enter" && !event.isComposing) {
            event.preventDefault();
            sendMessage();
        }
    });
    updateMessageLengthCounter();
}

if (isChatClosed) {
    disableChatInput();
}
