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

    if (message.senderNo === senderNo) {
        row.classList.add("me");
    } else {
        row.classList.add("other");
    }

    const bubble = document.createElement("div");
    bubble.classList.add("message-bubble");
    bubble.textContent = message.messageContent;

    row.appendChild(bubble);
    chatBody.appendChild(row);

    chatBody.scrollTop = chatBody.scrollHeight;
}

sendBtn.addEventListener("click", sendMessage);

messageInput.addEventListener("keyup", function (event) {
    if (event.key === "Enter") {
        sendMessage();
    }
});