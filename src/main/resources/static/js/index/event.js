//이벤트 연결
function bindEvents() {
    document.querySelectorAll("[data-action]").forEach((element) => {
        element.addEventListener("click", handleAction);
    });

    const memoInput = document.querySelector("#memoInput");
    if (memoInput) {
        memoInput.addEventListener("input", (event) => {
            state.memoText = event.target.value;
            render();
        });
    }

    const loginUsername = document.querySelector("#loginUsername");
    if (loginUsername) {
        loginUsername.addEventListener("input", (event) => {
            state.loginForm.username = event.target.value;
        });
    }

    const loginPassword = document.querySelector("#loginPassword");
    if (loginPassword) {
        loginPassword.addEventListener("input", (event) => {
            state.loginForm.password = event.target.value;
        });
    }
}

//버튼 액션 처리
function handleAction(event) {
    const action = event.currentTarget.dataset.action;
    const id = Number(event.currentTarget.dataset.id);
    const value = event.currentTarget.dataset.value;

    if (action === "toggle-edit") {
        state.isEditMode = !state.isEditMode;
        render();
        return;
    }

    if (action === "toggle-widget") {
        toggleWidget(id);
        return;
    }

    if (action === "toggle-collapse") {
        toggleCollapse(id);
        return;
    }

    if (action === "move-up") {
        moveWidget(id, "up");
        return;
    }

    if (action === "move-down") {
        moveWidget(id, "down");
        return;
    }

    if (action === "append-calc") {
        state.calculatorText += value;
        render();
        return;
    }

    if (action === "clear-calc") {
        state.calculatorText = "";
        render();
        return;
    }

    if (action === "calculate") {
        calculate();
        return;
    }

    if (action === "login") {
        login();
        return;
    }

    if (action === "logout") {
        state.currentUser = null;
        render();
    }
}