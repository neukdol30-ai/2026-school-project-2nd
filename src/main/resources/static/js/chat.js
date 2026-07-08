let socket = null;

const chatBody = document.getElementById("chatBody");
const messageInput = document.getElementById("messageInput");
const sendBtn = document.getElementById("sendBtn");

const renderedMessageKeys = new Set();

connectWebSocket();

function connectWebSocket() {
    socket = new WebSocket("ws://localhost:8080/ws/chat");

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

            if (Number(data.message.senderNo) !== Number(senderNo)) {
                sendReadEvent();
            }

            return;
        }

        if (data.type === "READ") {
            if (Number(data.viewerNo) !== Number(senderNo)) {
                markMyMessagesAsRead();
            }

            return;
        }

        if (data.type === "CLOSE") {
            if (data.message) {
                appendSystemMessage(data.message.messageContent, data.message.createdDate);
            } else {
                appendSystemMessage("상담이 종료되었습니다.", null);
            }

            disableChatInput();
            return;
        }
    };

    socket.onclose = function () {
        console.log("WebSocket 연결 종료");
    };

    socket.onerror = function (error) {
        console.log("WebSocket 에러", error);
    };
}

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

            if (typeof roomStatus !== "undefined" && roomStatus === "CLOSED") {
                disableChatInput();
            }
        });
}

function sendMessage() {
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

function appendMessage(message) {
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

    chatBody.scrollTop = chatBody.scrollHeight;
}

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

    chatBody.scrollTop = chatBody.scrollHeight;
}

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

function markMyMessagesAsRead() {
    const readStatusList = document.querySelectorAll(".message-row.me .read-status");

    readStatusList.forEach(readStatus => {
        readStatus.textContent = "읽음";
    });
}

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

function makeMessageKey(message) {
    const timeValue = getTimeValue(message.createdDate);

    return [
        message.roomNo,
        message.senderNo,
        message.messageContent,
        timeValue
    ].join("|");
}

sendBtn.addEventListener("click", sendMessage);

messageInput.addEventListener("keyup", function (event) {
    if (event.key === "Enter") {
        sendMessage();
    }
});