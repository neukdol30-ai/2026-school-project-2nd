// 이벤트 연결 상태
let eventsBound = false;

// 더블클릭으로 선택한 배치 항목
let widgetMoveSession = null;

function bindEvents() {
    if (eventsBound) {
        return;
    }

    const app = document.querySelector("#app");

    if (!app) {
        return;
    }

    app.addEventListener(
        "click",
        handleEditModeClick,
        true
    );

    app.addEventListener(
        "dblclick",
        handleWidgetDoubleClick
    );

    app.addEventListener(
        "click",
        handleAction
    );

    app.addEventListener(
        "input",
        handleInput
    );

    app.addEventListener(
        "keydown",
        handleWorldTimeKeydown
    );

    app.addEventListener(
        "dragstart",
        preventNativeWidgetDrag
    );

    document.addEventListener(
        "pointermove",
        handleWidgetMovePointer
    );

    document.addEventListener(
        "keydown",
        handleWidgetMoveKeydown
    );

    syncLayoutEditClass();
    eventsBound = true;
}

function syncLayoutEditClass() {
    const app = document.querySelector("#app");

    if (!app) {
        return;
    }

    app.classList.toggle(
        "is-layout-edit-mode",
        state.isEditMode
    );
}

function getMovableLayoutItem(target) {
    const item = target.closest(
        "[data-layout-item]"
    );

    if (!item || item.dataset.layoutFixed) {
        return null;
    }

    if (item.dataset.layoutPlaceholder === "true") {
        return null;
    }

    return item;
}

function handleEditModeClick(event) {
    if (!state.isEditMode) {
        return;
    }

    if (widgetMoveSession) {
        const board = event.target.closest(
            "[data-dashboard-board]"
        );

        if (board === widgetMoveSession.board) {
            updateMovePlaceholder(
                event.clientX,
                event.clientY
            );
            finishWidgetMove(true);

            event.preventDefault();
            event.stopPropagation();
            return;
        }

        const actionButton = event.target.closest(
            "[data-action]"
        );

        if (actionButton) {
            return;
        }

        event.preventDefault();
        event.stopPropagation();
        return;
    }

    const layoutItem = getMovableLayoutItem(
        event.target
    );

    if (!layoutItem) {
        return;
    }

    const hideButton = event.target.closest(
        '[data-action="toggle-widget"]'
    );

    if (hideButton) {
        return;
    }

    event.preventDefault();
    event.stopPropagation();
}

function handleWidgetDoubleClick(event) {
    if (!state.isEditMode) {
        return;
    }

    if (
        event.target.closest(
            '[data-action="toggle-widget"]'
        )
    ) {
        return;
    }

    const layoutItem = getMovableLayoutItem(
        event.target
    );

    if (!layoutItem) {
        return;
    }

    event.preventDefault();
    event.stopPropagation();

    if (widgetMoveSession) {
        if (widgetMoveSession.item === layoutItem) {
            finishWidgetMove(false);
            return;
        }

        finishWidgetMove(false);
    }

    startWidgetMove(
        layoutItem,
        event.clientX,
        event.clientY
    );
}

function startWidgetMove(
    layoutItem,
    pointerX,
    pointerY
) {
    const board = layoutItem.closest(
        "[data-dashboard-board]"
    );

    if (!board) {
        return;
    }

    const rect = layoutItem.getBoundingClientRect();
    const span = Number(
        layoutItem.dataset.layoutSpan
    ) === 2 ? 2 : 1;
    const originalColumn = Number(
        layoutItem.dataset.layoutColumn
    ) || 1;
    const originalNextSibling =
        layoutItem.nextElementSibling;

    const placeholder = document.createElement("div");
    placeholder.className =
        "widget-move-placeholder dashboard-layout-item";
    placeholder.dataset.layoutItem = "";
    placeholder.dataset.layoutPlaceholder = "true";
    placeholder.dataset.layoutKey = "move-placeholder";
    placeholder.dataset.layoutSpan = String(span);
    placeholder.dataset.layoutColumn =
        String(originalColumn);
    placeholder.style.height = `${rect.height}px`;
    placeholder.setAttribute("aria-hidden", "true");

    board.insertBefore(
        placeholder,
        layoutItem
    );

    widgetMoveSession = {
        item: layoutItem,
        board,
        placeholder,
        span,
        originalColumn,
        originalNextSibling,
        offsetX: pointerX - rect.left,
        offsetY: pointerY - rect.top
    };

    layoutItem.dataset.layoutFloating = "true";
    layoutItem.classList.add("is-move-selected");
    layoutItem.style.position = "fixed";
    layoutItem.style.left = `${rect.left}px`;
    layoutItem.style.top = `${rect.top}px`;
    layoutItem.style.width = `${rect.width}px`;
    layoutItem.style.height = `${rect.height}px`;
    layoutItem.style.margin = "0";
    layoutItem.style.zIndex = "1200";
    layoutItem.style.pointerEvents = "none";
    layoutItem.style.transition = "none";
    layoutItem.style.transform = "none";

    board.classList.add("is-widget-move-board");
    document.body.classList.add("is-widget-move-mode");

    updateMovePlaceholder(pointerX, pointerY);
    scheduleDashboardLayout();
}

function handleWidgetMovePointer(event) {
    if (!widgetMoveSession) {
        return;
    }

    const session = widgetMoveSession;
    const left = event.clientX - session.offsetX;
    const top = event.clientY - session.offsetY;

    session.item.style.left = `${left}px`;
    session.item.style.top = `${top}px`;

    updateMovePlaceholder(
        event.clientX,
        event.clientY
    );

    autoScrollWidgetMove(event.clientY);
}

function getLayoutItemVisualOrder(items) {
    return [...items].sort((first, second) => {
        const firstRect = first.getBoundingClientRect();
        const secondRect = second.getBoundingClientRect();
        const topDifference = firstRect.top - secondRect.top;

        if (Math.abs(topDifference) > 8) {
            return topDifference;
        }

        return firstRect.left - secondRect.left;
    });
}

function updateMovePlaceholder(
    pointerX,
    pointerY
) {
    const session = widgetMoveSession;

    if (!session) {
        return;
    }

    const boardRect =
        session.board.getBoundingClientRect();

    if (
        pointerX < boardRect.left - 40
        || pointerX > boardRect.right + 40
    ) {
        return;
    }

    const targetColumn = getDashboardColumnFromPointer(
        pointerX,
        session.span
    );

    session.placeholder.dataset.layoutColumn =
        String(targetColumn);

    const candidates = getLayoutItemVisualOrder(
        session.board.querySelectorAll(
            ":scope > [data-layout-item]"
        )
    ).filter((item) => {
        if (
            item === session.placeholder
            || item === session.item
            || item.dataset.layoutFloating === "true"
        ) {
            return false;
        }

        const itemColumn = Number(
            item.dataset.layoutColumn
        ) || 1;
        const itemSpan = Number(
            item.dataset.layoutSpan
        ) === 2 ? 2 : 1;

        return dashboardColumnRangesOverlap(
            targetColumn,
            session.span,
            itemColumn,
            itemSpan
        );
    });

    let referenceItem = null;

    for (const candidate of candidates) {
        const rect = candidate.getBoundingClientRect();
        const middleY = rect.top + rect.height / 2;

        if (pointerY < middleY) {
            referenceItem = candidate;
            break;
        }
    }

    if (referenceItem) {
        session.board.insertBefore(
            session.placeholder,
            referenceItem
        );
    } else {
        session.board.append(session.placeholder);
    }

    scheduleDashboardLayout();
}

function autoScrollWidgetMove(pointerY) {
    const edgeSize = 80;
    const scrollSpeed = 14;

    if (pointerY < edgeSize) {
        window.scrollBy(0, -scrollSpeed);
        return;
    }

    if (
        pointerY
        > window.innerHeight - edgeSize
    ) {
        window.scrollBy(0, scrollSpeed);
    }
}

function handleWidgetMoveKeydown(event) {
    if (
        event.key !== "Escape"
        || !widgetMoveSession
    ) {
        return;
    }

    event.preventDefault();
    finishWidgetMove(false);
}

function finishWidgetMove(saveLayout) {
    const session = widgetMoveSession;

    if (!session) {
        return;
    }

    if (saveLayout) {
        session.item.dataset.layoutColumn =
            session.placeholder.dataset.layoutColumn;

        session.board.insertBefore(
            session.item,
            session.placeholder
        );
    } else if (
        session.originalNextSibling
        && session.originalNextSibling.parentElement
        === session.board
    ) {
        session.board.insertBefore(
            session.item,
            session.originalNextSibling
        );
        session.item.dataset.layoutColumn =
            String(session.originalColumn);
    } else {
        session.board.append(session.item);
        session.item.dataset.layoutColumn =
            String(session.originalColumn);
    }

    session.placeholder.remove();
    resetWidgetMoveStyle(session.item);

    session.board.classList.remove(
        "is-widget-move-board"
    );
    document.body.classList.remove(
        "is-widget-move-mode"
    );

    widgetMoveSession = null;

    if (saveLayout) {
        saveDashboardLayoutFromDom();
    }

    observeDashboardBoardItems();
}

function resetWidgetMoveStyle(item) {
    item.classList.remove("is-move-selected");
    delete item.dataset.layoutFloating;

    [
        "position",
        "left",
        "top",
        "width",
        "height",
        "margin",
        "z-index",
        "pointer-events",
        "transition",
        "transform"
    ].forEach((property) => {
        item.style.removeProperty(property);
    });
}

function preventNativeWidgetDrag(event) {
    if (event.target.closest("[data-layout-item]")) {
        event.preventDefault();
    }
}

// 입력 이벤트 처리
function handleInput(event) {
    if (
        event.target.matches(
            "#memoInput"
        )
    ) {
        state.memoText =
            event.target.value;

        return;
    }

    if (
        event.target.matches(
            "#loginUsername"
        )
    ) {
        state.loginForm.username =
            event.target.value;

        return;
    }

    if (
        event.target.matches(
            "#loginPassword"
        )
    ) {
        state.loginForm.password =
            event.target.value;
    }
}

// 세계시간 카드를 키보드로 선택
function handleWorldTimeKeydown(event) {
    if (
        event.key !== "Enter"
        && event.key !== " "
    ) {
        return;
    }

    const worldTimeItem =
        event.target.closest(
            '[data-action="select-world-time"]'
        );

    if (!worldTimeItem) {
        return;
    }

    event.preventDefault();

    worldTimeItem.click();
}

// 버튼 액션 처리
function handleAction(event) {
    const button =
        event.target.closest(
            "[data-action]"
        );

    if (!button) {
        return;
    }

    const action =
        button.dataset.action;

    const id =
        Number(button.dataset.id);

    const value =
        button.dataset.value;

    if (action === "select-weather-day") {
        const weatherDayIndex =
            Number(value);

        selectWeatherDay(
            weatherDayIndex
        );

        return;
    }

    if (
        action
        === "select-world-time"
    ) {
        const city =
            button.dataset.city;

        const country =
            button.dataset.country;

        const timeZone =
            button.dataset.timeZone;

        if (
            typeof selectCurrentTimeCity
            !== "function"
        ) {
            return;
        }

        selectCurrentTimeCity(
            city,
            country,
            timeZone
        );

        return;
    }

    if (
        action
        === "select-news-category"
    ) {
        const category =
            button.dataset.value;

        if (
            !state.newsCategories.includes(
                category
            )
        ) {
            return;
        }

        if (
            state.newsCategory
            === category
        ) {
            return;
        }

        fetchNews(category);

        return;
    }

    if (action === "toggle-edit") {
        finishWidgetMove(false);

        state.isSettingsOpen = true;

        updateSettingsDrawer();
        updateControlBox();
        updateGlobalBanner();

        return;
    }

    if (action === "close-edit") {
        finishWidgetMove(false);

        state.isSettingsOpen = false;

        updateSettingsDrawer();
        updateControlBox();
        updateGlobalBanner();

        return;
    }

    if (
        action
        === "start-layout-edit"
    ) {
        finishWidgetMove(false);

        state.isSettingsOpen = false;
        state.isEditMode = true;

        updateSettingsDrawer();
        updateControlBox();
        updateGlobalBanner();
        updateDashboardEditModeUi();

        return;
    }

    if (
        action
        === "finish-layout-edit"
    ) {
        finishWidgetMove(true);

        state.isEditMode = false;

        updateGlobalBanner();
        updateDashboardEditModeUi();

        return;
    }

    if (
        action
        === "toggle-widget"
    ) {
        finishWidgetMove(false);

        toggleWidget(id);

        return;
    }

    if (
        action
        === "toggle-collapse"
    ) {
        finishWidgetMove(false);

        toggleCollapse(id);

        return;
    }

    if (action === "select-stock") {
        const stockIndex = Number(value);

        if (
            !Number.isInteger(stockIndex) ||
            stockIndex < 0 ||
            stockIndex >= state.stockItems.length
        ) {
            return;
        }

        state.stockSlideIndex = stockIndex;
        updateStockQuoteSelection(stockIndex);

        if (state.stockSwiper) {
            state.stockSwiper.slideToLoop(
                stockIndex,
                450
            );
        }

        return;
    }

    if (action === "refresh-stocks") {
        if (state.stockLoading) {
            return;
        }

        fetchStocks();
        return;
    }


    if (
        action === "move-up"
        || action === "move-down"
    ) {
        return;
    }

    if (
        action
        === "append-calc"
    ) {
        appendCalculatorValue(value);

        return;
    }

    if (
        action
        === "clear-calc"
    ) {
        state.calculatorText =
            "";

        updateCalculatorDisplay();

        return;
    }

    if (
        action
        === "percent-calc"
    ) {
        applyCalculatorPercent();

        return;
    }

    if (
        action
        === "backspace-calc"
    ) {
        removeCalculatorCharacter();

        return;
    }

    if (
        action
        === "parentheses-calc"
    ) {
        appendCalculatorParenthesis();

        return;
    }

    if (
        action
        === "decimal-calc"
    ) {
        appendCalculatorDecimal();

        return;
    }

    if (
        action
        === "calculate"
    ) {
        calculate();

        return;
    }

    if (
        action
        === "login"
    ) {
        login();

        return;
    }

    if (
        action
        === "logout"
    ) {
        state.currentUser =
            null;

        render();

        return;
    }

    if (
        action
        === "prev-calendar-month"
    ) {
        state.calendarMonth--;

        if (
            state.calendarMonth < 0
        ) {
            state.calendarMonth =
                11;

            state.calendarYear--;
        }

        state.miniCalendarDayMap =
            new Map();

        state.miniCalendarError =
            "";

        refreshMiniCalendarWidget();

        fetchMiniCalendarMonthData(
            true
        );

        return;
    }

    if (
        action
        === "next-calendar-month"
    ) {
        state.calendarMonth++;

        if (
            state.calendarMonth > 11
        ) {
            state.calendarMonth =
                0;

            state.calendarYear++;
        }

        state.miniCalendarDayMap =
            new Map();

        state.miniCalendarError =
            "";

        refreshMiniCalendarWidget();

        fetchMiniCalendarMonthData(
            true
        );

        return;
    }

    if (
        action
        === "go-calendar-day"
    ) {
        const date =
            button.dataset.date;

        if (!date) {
            return;
        }

        window.location.href =
            "/calendar?date="
            + encodeURIComponent(
                date
            );
    }
}