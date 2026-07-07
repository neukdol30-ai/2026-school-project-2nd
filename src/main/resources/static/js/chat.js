let socket = null;

const chatBody = document.getElementById("chatBody");
const messageInput = document.getElementById("messageInput");
const sendBtn = document.getElementById("sendBtn");

connectWebSocket();
loadMessages();

function connectWebSocket() {
    socket = new WebSocket("ws://localhost:8080/ws/chat");

    socket.onopen = function () {
        console.log("WebSocket 연결 성공");
    };

    socket.onmessage = function (event) {
        const message = JSON.parse(event.data);
        appendMessage(message);
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

            messages.forEach(message => {
                appendMessage(message);
            });
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
        messageContent: content
    };

    socket.send(JSON.stringify(message));

    messageInput.value = "";
    messageInput.focus();
}

function appendMessage(message) {
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

    const bubble = document.createElement("div");
    bubble.classList.add("message-bubble");
    bubble.textContent = message.messageContent;

    const meta = document.createElement("div");
    meta.classList.add("message-meta");

    const time = document.createElement("span");
    time.classList.add("message-time");
    time.textContent = formatMessageTime(message.createDate);

    meta.appendChild(time);

    messageBox.appendChild(bubble);
    messageBox.appendChild(meta);

    row.appendChild(messageBox);
    chatBody.appendChild(row);

    chatBody.scrollTop = chatBody.scrollHeight;
}

function formatMessageTime(createDate) {
    if (!createDate) {
        return "";
    }
    let date;

    if (Array.isArray(createDate)) {
        const year = createDate[0];
        const month = createDate[1] - 1;
        const day = createDate[2];
        const hours = createDate[3] || 0;
        const minutes = createDate[4] || 0;
        const seconds = createDate[5] || 0;

        date = new Date(year, month, day, hours, minutes, seconds);
    } else {
        date = new Date(createDate);
    }
    if (isNaN(date.getTime())) {
        return "";
    }
    const hours = String(date.getHours()).padStart(2,"0");
    const minutes = String(date.getMinutes()).padStart(2,"0");

    return `${hours}:${minutes}`;
}
sendBtn.addEventListener("click", sendMessage);

messageInput.addEventListener("keyup", function (event) {
    if (event.key === "Enter") {
        sendMessage();
    }
});