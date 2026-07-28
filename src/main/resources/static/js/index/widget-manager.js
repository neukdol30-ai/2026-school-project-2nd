// 일정과 캘린더 위젯 ID
const SCHEDULE_WIDGET_ID = 3;
const CALENDAR_WIDGET_ID = 7;

// 대시보드 배치 기본값
const DASHBOARD_TIME_GROUP_KEY = "time-group";
const DASHBOARD_SCHEDULE_GROUP_KEY = "schedule-calendar";
const DASHBOARD_AUTH_KEY = "auth-fixed";
const DASHBOARD_LAYOUT_VERSION = 1;
const DASHBOARD_COLUMN_COUNT = 3;
const DASHBOARD_COLUMN_WIDTH = 450;
const DASHBOARD_COLUMN_GAP = 20;
const DASHBOARD_ROW_GAP = 20;
const DASHBOARD_BOARD_WIDTH =
    DASHBOARD_COLUMN_WIDTH * DASHBOARD_COLUMN_COUNT
    + DASHBOARD_COLUMN_GAP * (DASHBOARD_COLUMN_COUNT - 1);

const dashboardLayoutEntries = new Map();
let dashboardLayoutInitialized = false;
let dashboardLayoutFrameId = null;
let dashboardResizeObserver = null;
let dashboardResizeBound = false;
let dashboardLayoutPendingAnimate = false;
let dashboardLayoutAnimationActive = false;
let dashboardLayoutAnimationTimerId = null;
let dashboardSilentLayoutQueued = false;
let loadedWidgetOrderMemberId = "";

function normalizeWidgetType(widget) {
    return String(widget?.type || "")
        .toLowerCase()
        .replace(/[\s_-]/g, "");
}

function normalizeWidgetTitle(widget) {
    return String(widget?.title || "")
        .replace(/\s/g, "");
}

function getCurrentTimeWidget() {
    return state.widgets.find((widget) => {
        return widget.id === state.headerWidgetId
            || normalizeWidgetType(widget) === "currenttime"
            || normalizeWidgetTitle(widget) === "현재시간";
    });
}

function getWorldTimeWidget() {
    return state.widgets.find((widget) => {
        return normalizeWidgetType(widget) === "worldtime"
            || normalizeWidgetTitle(widget) === "세계시간";
    });
}

function getScheduleWidget() {
    return state.widgets.find((widget) => {
        return widget.id === SCHEDULE_WIDGET_ID;
    });
}

function getCalendarWidget() {
    return state.widgets.find((widget) => {
        return widget.id === CALENDAR_WIDGET_ID;
    });
}

function isSpecialDashboardWidget(widget) {
    if (!widget) {
        return true;
    }

    return widget.id === SCHEDULE_WIDGET_ID
        || widget.id === CALENDAR_WIDGET_ID
        || widget.id === state.headerWidgetId
        || normalizeWidgetType(widget) === "currenttime"
        || normalizeWidgetType(widget) === "worldtime";
}

function getDashboardWidgetKey(widget) {
    return `widget:${widget.id}`;
}

// 기존 메인 위젯은 2칸, 기존 사이드 위젯은 1칸 크기를 유지한다.
function getDashboardWidgetSpan(widget) {
    const savedSpan = Number(widget?.dashboardSpan);

    if (savedSpan === 1 || savedSpan === 2) {
        return savedSpan;
    }

    const span = widget?.zone === "main" ? 2 : 1;

    if (widget) {
        widget.dashboardSpan = span;
    }

    return span;
}

function clampDashboardColumn(column, span) {
    const safeSpan = span === 2 ? 2 : 1;
    const maxColumn = DASHBOARD_COLUMN_COUNT - safeSpan + 1;
    const numericColumn = Number(column);

    if (!Number.isFinite(numericColumn)) {
        return 1;
    }

    return Math.min(
        Math.max(Math.round(numericColumn), 1),
        maxColumn
    );
}

function createDashboardLayoutEntry(
    key,
    span,
    column,
    order
) {
    return {
        key,
        span: span === 2 ? 2 : 1,
        column: clampDashboardColumn(column, span),
        order: Number.isFinite(Number(order))
            ? Number(order)
            : 9999
    };
}

function ensureDashboardLayoutEntry(
    key,
    span,
    column,
    order
) {
    const existing = dashboardLayoutEntries.get(key);

    if (existing) {
        existing.span = span === 2 ? 2 : 1;
        existing.column = clampDashboardColumn(
            existing.column,
            existing.span
        );
        return existing;
    }

    const entry = createDashboardLayoutEntry(
        key,
        span,
        column,
        order
    );

    dashboardLayoutEntries.set(key, entry);
    return entry;
}

function getDefaultWidgetOrder(widget) {
    const orderNo = Number(widget?.orderNo) || 999;
    const zoneOffset = widget?.zone === "side" ? 1 : 0;

    return 100 + orderNo * 10 + zoneOffset;
}

function ensureDashboardLayoutModel() {
    const validKeys = new Set();

    ensureDashboardLayoutEntry(
        DASHBOARD_TIME_GROUP_KEY,
        2,
        1,
        0
    );
    validKeys.add(DASHBOARD_TIME_GROUP_KEY);

    const scheduleWidget = getScheduleWidget();
    const calendarWidget = getCalendarWidget();

    if (scheduleWidget || calendarWidget) {
        const scheduleOrder = Math.min(
            Number(scheduleWidget?.orderNo) || 999,
            Number(calendarWidget?.orderNo) || 999
        );

        ensureDashboardLayoutEntry(
            DASHBOARD_SCHEDULE_GROUP_KEY,
            2,
            1,
            100 + scheduleOrder * 10
        );
        validKeys.add(DASHBOARD_SCHEDULE_GROUP_KEY);
    }

    state.widgets.forEach((widget) => {
        if (isSpecialDashboardWidget(widget)) {
            return;
        }

        const key = getDashboardWidgetKey(widget);
        const span = getDashboardWidgetSpan(widget);
        const defaultColumn = span === 2
            ? 1
            : widget.zone === "side"
                ? 3
                : 1;

        ensureDashboardLayoutEntry(
            key,
            span,
            defaultColumn,
            getDefaultWidgetOrder(widget)
        );
        validKeys.add(key);
    });

    [...dashboardLayoutEntries.keys()].forEach((key) => {
        if (!validKeys.has(key)) {
            dashboardLayoutEntries.delete(key);
        }
    });

    dashboardLayoutInitialized = true;
}

function getDashboardLayoutEntry(key) {
    if (!dashboardLayoutInitialized) {
        ensureDashboardLayoutModel();
    }

    return dashboardLayoutEntries.get(key) || null;
}

function setDashboardLayoutColumn(key, column) {
    const entry = getDashboardLayoutEntry(key);

    if (!entry) {
        return;
    }

    entry.column = clampDashboardColumn(
        column,
        entry.span
    );
}

function getVisibleDashboardLayoutItems() {
    ensureDashboardLayoutModel();

    const items = [];
    const currentTimeWidget = getCurrentTimeWidget();
    const worldTimeWidget = getWorldTimeWidget();

    if (
        currentTimeWidget?.visible
        || worldTimeWidget?.visible
    ) {
        const entry = getDashboardLayoutEntry(
            DASHBOARD_TIME_GROUP_KEY
        );

        items.push({
            kind: "time-group",
            key: DASHBOARD_TIME_GROUP_KEY,
            entry,
            span: 2,
            currentTimeWidget,
            worldTimeWidget
        });
    }

    const scheduleWidget = getScheduleWidget();
    const calendarWidget = getCalendarWidget();

    if (
        scheduleWidget?.visible
        || calendarWidget?.visible
    ) {
        const entry = getDashboardLayoutEntry(
            DASHBOARD_SCHEDULE_GROUP_KEY
        );

        items.push({
            kind: "schedule-group",
            key: DASHBOARD_SCHEDULE_GROUP_KEY,
            entry,
            span: 2,
            scheduleWidget,
            calendarWidget
        });
    }

    state.widgets.forEach((widget) => {
        if (
            !widget.visible
            || isSpecialDashboardWidget(widget)
        ) {
            return;
        }

        const key = getDashboardWidgetKey(widget);
        const entry = getDashboardLayoutEntry(key);

        items.push({
            kind: "widget",
            key,
            entry,
            span: entry?.span || getDashboardWidgetSpan(widget),
            widget
        });
    });

    return items.sort((a, b) => {
        return (a.entry?.order ?? 9999)
            - (b.entry?.order ?? 9999);
    });
}

// 기존 호출과의 호환을 위해 남겨둔다.
function normalizeScheduleCalendarGroup() {
    ensureDashboardLayoutModel();
}

function getMainWidgets() {
    return state.widgets
        .filter((widget) => {
            return widget.visible
                && !isSpecialDashboardWidget(widget)
                && getDashboardWidgetSpan(widget) === 2;
        })
        .sort((a, b) => a.orderNo - b.orderNo);
}

function getSideWidgets() {
    return state.widgets
        .filter((widget) => {
            return widget.visible
                && !isSpecialDashboardWidget(widget)
                && getDashboardWidgetSpan(widget) === 1;
        })
        .sort((a, b) => a.orderNo - b.orderNo);
}

function getDashboardItemSpan(element) {
    return Number(element?.dataset.layoutSpan) === 2
        ? 2
        : 1;
}

function getDashboardItemColumn(element) {
    return clampDashboardColumn(
        element?.dataset.layoutColumn,
        getDashboardItemSpan(element)
    );
}

function getDashboardColumnX(column) {
    return (column - 1)
        * (DASHBOARD_COLUMN_WIDTH + DASHBOARD_COLUMN_GAP);
}

function setDashboardElementWidth(element, span) {
    const width = DASHBOARD_COLUMN_WIDTH * span
        + DASHBOARD_COLUMN_GAP * (span - 1);

    element.style.width = `${width}px`;
}

function positionDashboardElement(
    element,
    column,
    top,
    span
) {
    const left = getDashboardColumnX(column);

    setDashboardElementWidth(element, span);
    element.style.transform =
        `translate3d(${left}px, ${top}px, 0)`;
    element.dataset.layoutY = String(top);
}

function layoutDashboardBoard() {
    dashboardLayoutFrameId = null;

    const board = document.querySelector(
        "[data-dashboard-board]"
    );

    if (!board) {
        return;
    }

    board.style.width = `${DASHBOARD_BOARD_WIDTH}px`;

    const columnHeights = Array(
        DASHBOARD_COLUMN_COUNT
    ).fill(0);

    const authItem = board.querySelector(
        ":scope > [data-layout-fixed=\"auth\"]"
    );

    if (authItem) {
        const authColumn = 3;
        const authSpan = 1;

        positionDashboardElement(
            authItem,
            authColumn,
            0,
            authSpan
        );

        const authHeight = authItem.offsetHeight;
        columnHeights[authColumn - 1] =
            authHeight + DASHBOARD_ROW_GAP;
    }

    const layoutItems = [
        ...board.querySelectorAll(
            ":scope > [data-layout-item]"
        )
    ].filter((item) => {
        return item.dataset.layoutFloating !== "true";
    });

    layoutItems.forEach((item) => {
        const span = getDashboardItemSpan(item);
        const column = getDashboardItemColumn(item);
        const coveredHeights = columnHeights.slice(
            column - 1,
            column - 1 + span
        );
        const top = Math.max(0, ...coveredHeights);

        item.dataset.layoutColumn = String(column);

        positionDashboardElement(
            item,
            column,
            top,
            span
        );

        const itemHeight = item.offsetHeight;
        const nextHeight =
            top + itemHeight + DASHBOARD_ROW_GAP;

        for (
            let index = column - 1;
            index < column - 1 + span;
            index++
        ) {
            columnHeights[index] = nextHeight;
        }
    });

    const tallestColumn = Math.max(
        0,
        ...columnHeights
    );

    board.style.height = `${Math.max(
        tallestColumn - DASHBOARD_ROW_GAP,
        0
    )}px`;

}

function scheduleDashboardLayout(options = {}) {
    const animate = typeof options === "boolean"
        ? options
        : options.animate !== false;

    if (!animate && dashboardLayoutAnimationActive) {
        dashboardSilentLayoutQueued = true;
        return;
    }

    dashboardLayoutPendingAnimate =
        dashboardLayoutPendingAnimate || animate;

    if (dashboardLayoutFrameId !== null) {
        cancelAnimationFrame(dashboardLayoutFrameId);
    }

    dashboardLayoutFrameId = requestAnimationFrame(() => {
        const board = document.querySelector(
            "[data-dashboard-board]"
        );

        const shouldAnimate =
            dashboardLayoutPendingAnimate;

        dashboardLayoutPendingAnimate = false;

        if (board) {
            board.classList.toggle(
                "is-layout-animating",
                shouldAnimate
            );
        }

        layoutDashboardBoard();

        if (!shouldAnimate || !board) {
            return;
        }

        dashboardLayoutAnimationActive = true;

        if (dashboardLayoutAnimationTimerId !== null) {
            clearTimeout(
                dashboardLayoutAnimationTimerId
            );
        }

        dashboardLayoutAnimationTimerId = setTimeout(() => {
            board.classList.remove(
                "is-layout-animating"
            );

            dashboardLayoutAnimationActive = false;
            dashboardLayoutAnimationTimerId = null;

            if (!dashboardSilentLayoutQueued) {
                return;
            }

            dashboardSilentLayoutQueued = false;
            scheduleDashboardLayout({
                animate: false
            });
        }, 280);
    });
}

function observeDashboardBoardItems(options = {}) {
    const board = document.querySelector(
        "[data-dashboard-board]"
    );

    const animate = options.animate !== true;

    if (!board) {
        return;
    }

    if (dashboardResizeObserver) {
        dashboardResizeObserver.disconnect();
    }

    if ("ResizeObserver" in window) {
        dashboardResizeObserver = new ResizeObserver(() => {
            scheduleDashboardLayout({
                animate: false
            });
        });

        board.querySelectorAll(
            ":scope > [data-layout-item], "
            + ":scope > [data-layout-fixed]"
        ).forEach((element) => {
            dashboardResizeObserver.observe(element);
        });
    }

    if (!dashboardResizeBound) {
        window.addEventListener(
            "resize",
            () => {
                scheduleDashboardLayout({
                    animate: false
                });
            }
        );
        dashboardResizeBound = true;
    }

    scheduleDashboardLayout({
        animate: animate
    });
}

// 기존 짧은 컬럼 sticky 대신 새 3칸 배치를 다시 계산한다.
function updateFollowColumn() {
    scheduleDashboardLayout({
        animate: false
    });
}

function captureWidgetPositions() {
    const positions = new Map();

    document
        .querySelectorAll("[data-layout-key]")
        .forEach((element) => {
            positions.set(
                element.dataset.layoutKey,
                element.getBoundingClientRect()
            );
        });

    return positions;
}

// 위치 전환은 transform transition으로 처리한다.
function animateWidgetChanges() {
    scheduleDashboardLayout({
        animate: true
    });
}

function toggleWidget(id) {
    const widget = state.widgets.find((item) => {
        return item.id === id;
    });

    if (!widget) {
        return;
    }

    widget.visible = !widget.visible;
    render();
}

function toggleCollapse(id) {
    const widget = state.widgets.find((item) => {
        return item.id === id;
    });

    if (!widget) {
        return;
    }

    widget.collapsed = !widget.collapsed;
    render();
}

function destroyStockWidgetRuntime() {
    if (typeof rememberStockSwiperIndex === "function") {
        rememberStockSwiperIndex();
    }

    if (Array.isArray(state.stockCharts)) {
        state.stockCharts.forEach((chart) => {
            if (typeof chart?.destroy === "function") {
                chart.destroy();
            }
        });
        state.stockCharts = [];
    }

    if (state.stockSwiper?.destroy) {
        state.stockSwiper.destroy(true, true);
        state.stockSwiper = null;
    }
}

function refreshWidgetContent(widgetId) {
    const widget = state.widgets.find((item) => {
        return item.id === widgetId;
    });

    if (!widget) {
        return;
    }

    const content = document.querySelector(
        `[data-widget-content="${widgetId}"]`
    );

    if (!content) {
        return;
    }

    if (widget.type === "stock") {
        destroyStockWidgetRuntime();
        content.innerHTML = renderWidgetContent(widget);
        initStockSwiper();
        observeDashboardBoardItems({
            animate: false
        });
        return;
    }

    content.innerHTML = renderWidgetContent(widget);

    if (widget.type === "currentTime") {
        if (typeof updateCurrentTimeWidget === "function") {
            updateCurrentTimeWidget();
        }

        if (typeof updateSunTimeWidget === "function") {
            updateSunTimeWidget();
        }
    }

    if (
        widget.type === "worldTime"
        && typeof updateWorldTimeWidget === "function"
    ) {
        updateWorldTimeWidget();
    }

    scheduleDashboardLayout({
        animate: false
    });
}

function getWidgetOrderMemberId() {
    return String(
        state.currentUser?.username
        || state.currentUser?.memberId
        || ""
    ).trim();
}

function getWidgetOrderStorageKey(memberId) {
    return `dashboardFlexibleLayout:v${DASHBOARD_LAYOUT_VERSION}:${memberId}`;
}

function normalizeDashboardLayoutOrders() {
    [...dashboardLayoutEntries.values()]
        .sort((a, b) => a.order - b.order)
        .forEach((entry, index) => {
            entry.order = index + 1;
        });
}

function saveWidgetOrderToStorage() {
    const memberId = getWidgetOrderMemberId();

    if (!memberId) {
        return;
    }

    ensureDashboardLayoutModel();
    normalizeDashboardLayoutOrders();

    const savedLayout = {
        version: DASHBOARD_LAYOUT_VERSION,
        items: [...dashboardLayoutEntries.values()]
            .sort((a, b) => a.order - b.order)
            .map((entry) => {
                return {
                    key: entry.key,
                    column: entry.column,
                    order: entry.order
                };
            })
    };

    localStorage.setItem(
        getWidgetOrderStorageKey(memberId),
        JSON.stringify(savedLayout)
    );
}

function applySavedDashboardLayout(savedLayout) {
    if (!Array.isArray(savedLayout?.items)) {
        return false;
    }

    let applied = false;

    savedLayout.items.forEach((savedItem) => {
        const entry = dashboardLayoutEntries.get(
            String(savedItem?.key || "")
        );

        if (!entry) {
            return;
        }

        entry.column = clampDashboardColumn(
            savedItem.column,
            entry.span
        );

        if (Number.isFinite(Number(savedItem.order))) {
            entry.order = Number(savedItem.order);
        }

        applied = true;
    });

    normalizeDashboardLayoutOrders();
    return applied;
}

function loadWidgetOrderForCurrentUser() {
    const memberId = getWidgetOrderMemberId();

    if (!memberId) {
        loadedWidgetOrderMemberId = "";
        ensureDashboardLayoutModel();
        return false;
    }

    if (loadedWidgetOrderMemberId === memberId) {
        return false;
    }

    ensureDashboardLayoutModel();
    loadedWidgetOrderMemberId = memberId;

    const savedText = localStorage.getItem(
        getWidgetOrderStorageKey(memberId)
    );

    if (!savedText) {
        return false;
    }

    try {
        const savedLayout = JSON.parse(savedText);
        return applySavedDashboardLayout(savedLayout);
    } catch (error) {
        console.error("위젯 배치 불러오기 실패:", error);
        localStorage.removeItem(
            getWidgetOrderStorageKey(memberId)
        );
        return false;
    }
}

function saveDashboardLayoutFromDom() {
    const board = document.querySelector(
        "[data-dashboard-board]"
    );

    if (!board) {
        return;
    }

    ensureDashboardLayoutModel();

    const visibleKeys = new Set();
    let nextOrder = 1;

    board.querySelectorAll(
        ":scope > [data-layout-item]:not([data-layout-placeholder])"
    ).forEach((item) => {
        if (item.dataset.layoutFloating === "true") {
            return;
        }

        const key = item.dataset.layoutKey;
        const entry = dashboardLayoutEntries.get(key);

        if (!entry) {
            return;
        }

        entry.column = clampDashboardColumn(
            item.dataset.layoutColumn,
            entry.span
        );
        entry.order = nextOrder;
        visibleKeys.add(key);
        nextOrder++;
    });

    [...dashboardLayoutEntries.values()]
        .filter((entry) => !visibleKeys.has(entry.key))
        .sort((a, b) => a.order - b.order)
        .forEach((entry) => {
            entry.order = nextOrder;
            nextOrder++;
        });

    saveWidgetOrderToStorage();
    scheduleDashboardLayout({
        animate: true
    });
}

// 이전 함수명을 호출하는 코드와 호환한다.
function saveWidgetOrderFromDom() {
    saveDashboardLayoutFromDom();
}

function syncWidgetList() {
    if (typeof syncDashboardBoard === "function") {
        syncDashboardBoard();
    }
}

function getDashboardColumnFromPointer(
    pointerX,
    span
) {
    const board = document.querySelector(
        "[data-dashboard-board]"
    );

    if (!board) {
        return 1;
    }

    const rect = board.getBoundingClientRect();
    const safeSpan = span === 2 ? 2 : 1;
    const maxStart = DASHBOARD_COLUMN_COUNT - safeSpan + 1;
    let nearestColumn = 1;
    let nearestDistance = Infinity;

    for (let column = 1; column <= maxStart; column++) {
        const itemWidth = DASHBOARD_COLUMN_WIDTH * safeSpan
            + DASHBOARD_COLUMN_GAP * (safeSpan - 1);
        const center = rect.left
            + getDashboardColumnX(column)
            + itemWidth / 2;
        const distance = Math.abs(pointerX - center);

        if (distance < nearestDistance) {
            nearestDistance = distance;
            nearestColumn = column;
        }
    }

    return nearestColumn;
}

function dashboardColumnRangesOverlap(
    firstColumn,
    firstSpan,
    secondColumn,
    secondSpan
) {
    const firstEnd = firstColumn + firstSpan - 1;
    const secondEnd = secondColumn + secondSpan - 1;

    return firstColumn <= secondEnd
        && secondColumn <= firstEnd;
}
