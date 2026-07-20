// 이벤트 연결 여부
let eventsBound = false;
let draggedWidgetId = null;

// 이벤트 연결
function bindEvents() {
    if (eventsBound) {
        return;
    }

    const app = document.querySelector("#app");

    app.addEventListener("click", handleAction);
    app.addEventListener("input", handleInput);

    eventsBound = true;

    app.addEventListener("dragstart", handleWidgetDragStart);
    app.addEventListener("dragend", handleWidgetDragEnd);
    app.addEventListener("dragover", handleWidgetDragOver);
    app.addEventListener("drop", handleWidgetDrop);
}

// 입력 이벤트 처리
function handleInput(event) {
    if (event.target.matches("#memoInput")) {
        state.memoText = event.target.value;
        return;
    }

    if (event.target.matches("#loginUsername")) {
        state.loginForm.username = event.target.value;
        return;
    }

    if (event.target.matches("#loginPassword")) {
        state.loginForm.password = event.target.value;
    }
}

// 버튼 액션 처리
function handleAction(event) {
    const button = event.target.closest("[data-action]");

    if (!button) {
        return;
    }

    const action = button.dataset.action;
    const id = Number(button.dataset.id);
    const value = button.dataset.value;

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

    if (action === "append-calc") {
        state.calculatorText += value;
        refreshWidgetContent(6);
        return;
    }

    if (action === "clear-calc") {
        state.calculatorText = "";
        refreshWidgetContent(6);
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

// 드래그 시작
function handleWidgetDragStart(event) {
    const dragHandle = event.target.closest("[data-drag-handle]");

    if (!dragHandle || !state.isEditMode) {
        return;
    }

    const widgetCard = dragHandle.closest("[data-widget-id]");

    if (!widgetCard) {
        return;
    }

    draggedWidgetId = Number(widgetCard.dataset.widgetId);

    event.dataTransfer.effectAllowed = "move";
    event.dataTransfer.setData("text/plain", String(draggedWidgetId));

    requestAnimationFrame(() => {
        widgetCard.classList.add("is-dragging");
    });
}

// 드래그 종료 후 현재 카드 순서를 저장
function handleWidgetDragEnd() {
    const draggedCard = document.querySelector(".is-dragging");
    const widgetList = draggedCard?.closest("[data-widget-list]");

    if (widgetList) {
        const zone = widgetList.dataset.widgetList;

        saveWidgetOrderFromDom(zone);

        // 기존 위/아래 버튼의 disabled 상태도 현재 순서에 맞게 갱신
        syncWidgetList(zone);
    }

    document.querySelectorAll(".is-dragging, .is-drag-over")
        .forEach((element) => {
            element.classList.remove("is-dragging", "is-drag-over");
        });

    draggedWidgetId = null;
}

// 브라우저의 기본 drop 동작 방지
function handleWidgetDrop(event) {
    event.preventDefault();
}

// 드래그 중 다른 위젯 위에 올라왔을 때 즉시 순서 변경
function handleWidgetDragOver(event) {
    if (draggedWidgetId === null) {
        return;
    }

    const targetCard = event.target.closest("[data-widget-id]");
    const draggedCard = document.querySelector(".is-dragging");

    if (!targetCard || !draggedCard || targetCard === draggedCard) {
        return;
    }

    const draggedList = draggedCard.closest("[data-widget-list]");
    const targetList = targetCard.closest("[data-widget-list]");

    // 메인과 보조 영역 사이 이동은 막음
    if (draggedList !== targetList) {
        return;
    }

    event.preventDefault();
    event.dataTransfer.dropEffect = "move";

    const targetRect = targetCard.getBoundingClientRect();
    const shouldInsertBefore =
        event.clientY < targetRect.top + targetRect.height / 2;

    const referenceCard = shouldInsertBefore
        ? targetCard
        : targetCard.nextElementSibling;

    // 이미 같은 위치라면 다시 이동하지 않음
    if (referenceCard === draggedCard) {
        return;
    }

    const prevPositions = captureWidgetPositions();

    // 드래그 중인 카드를 대상의 위 또는 아래로 즉시 이동
    targetList.insertBefore(draggedCard, referenceCard);

    animateWidgetChanges(prevPositions);

    document.querySelectorAll(".is-drag-over").forEach((element) => {
        element.classList.remove("is-drag-over");
    });

    targetCard.classList.add("is-drag-over");
}