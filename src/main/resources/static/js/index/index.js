function updateHeaderWorldTimeWidget() {
    const worldTimeSlot = document.querySelector(
        "[data-time-world-slot]"
    );

    if (!worldTimeSlot) {
        return;
    }

    worldTimeSlot.innerHTML =
        renderHeaderWorldTimeWidget();

    if (typeof updateWorldTimeWidget === "function") {
        updateWorldTimeWidget();
    }

    scheduleDashboardLayout({
        animate: false
    });
}


let dashboardStarted = false;
let dashboardRendered = false;
let dashboardInitialMemberId = "";
let calendarRefreshIntervalId = null;

function getCurrentDashboardMemberId() {
    if (typeof getWidgetOrderMemberId === "function") {
        return getWidgetOrderMemberId();
    }

    return String(
        state.currentUser?.username
        || state.currentUser?.memberId
        || ""
    ).trim();
}

// 최초 화면을 그리기 전에 로그인 상태를 먼저 확인한다.
async function loadDashboardLoginState() {
    const controller = new AbortController();

    const timeoutId = window.setTimeout(
        function () {
            controller.abort();
        },
        5000
    );

    try {
        const response = await fetch(
            "/mypage/me",
            {
                credentials: "same-origin",
                cache: "no-store",
                headers: {
                    "Accept": "application/json",
                    "X-Requested-With": "XMLHttpRequest"
                },
                signal: controller.signal
            }
        );

        if (!response.ok) {
            state.currentUser = null;
            state.myPage = null;
            return;
        }

        const data = await response.json();

        if (
            !data
            || !data.loggedIn
            || !data.profile
        ) {
            state.currentUser = null;
            state.myPage = null;
            return;
        }

        state.currentUser = data.profile;
        state.myPage = data;
    } catch (error) {
        // 마이페이지 쪽 조회가 먼저 끝났다면 그 로그인 정보는 유지한다.
        if (!state.currentUser) {
            state.currentUser = null;
            state.myPage = null;
        }

        if (error?.name !== "AbortError") {
            console.warn(
                "초기 로그인 상태 확인 실패:",
                error
            );
        }
    } finally {
        window.clearTimeout(timeoutId);
    }
}

function startDashboardRuntime() {
    startCurrentTimeClock();
    startWorldTimeClock();
    fetchSunTime();
    fetchNews();
    fetchStocks();
    fetchWeather();

    Promise.all([
        fetchTodaySchedule(true),
        fetchMiniCalendarMonthData(true)
    ])
        .then(function () {
            return refreshCalendarWidgets(true);
        })
        .catch(function (error) {
            console.warn(
                "캘린더 최초 조회 실패:",
                error
            );
        });

    if (calendarRefreshIntervalId === null) {
        calendarRefreshIntervalId = window.setInterval(
            function () {
                refreshCalendarWidgets(true);
            },
            5000
        );
    }
}

async function startDashboard() {
    if (dashboardStarted) {
        return;
    }

    dashboardStarted = true;

    await loadDashboardLoginState();

    // 회원 정보가 확정된 상태에서 저장된 배치를 먼저 state에 적용한다.
    loadWidgetOrderForCurrentUser();
    dashboardInitialMemberId =
        getCurrentDashboardMemberId();

    renderInitialPage();
    dashboardRendered = true;

    startDashboardRuntime();
}

function renderInitialPage() {
    const app = document.querySelector("#app");

    if (!app) {
        return;
    }

    ensureDashboardLayoutModel();
    rememberStockSwiperIndex();

    app.innerHTML = `
        <div class="global-banner-area">
            <div data-global-banner>
                ${renderGlobalBanner()}
            </div>
        </div>

        <div class="container">
            <div
                class="settings-layer ${state.isSettingsOpen ? "is-open" : ""}"
                data-settings-layer
                aria-hidden="${String(!state.isSettingsOpen)}"
            >
                <button
                    class="settings-backdrop"
                    type="button"
                    data-action="close-edit"
                    aria-label="환경설정 닫기"
                ></button>

                <aside
                    class="settings-drawer"
                    aria-label="환경설정"
                >
                    <div class="settings-drawer-header">
                        <h2>환경설정</h2>

                        <button
                            class="settings-close-button"
                            type="button"
                            data-action="close-edit"
                            aria-label="환경설정 닫기"
                        >
                            ×
                        </button>
                    </div>

                    <div data-control-box>
                        ${state.isSettingsOpen ? renderControlBox() : ""}
                    </div>
                </aside>
            </div>

            <main
                class="dashboard-board"
                data-dashboard-board
            >
                ${renderDashboardBoardContent()}
            </main>
        </div>

        <div data-global-footer>
            ${renderGlobalFooter()}
        </div>
    `;

    bindEvents();
    syncLayoutEditClass();
    initStockSwiper();

    layoutDashboardBoard();

    observeDashboardBoardItems({
        animate: false
    });

    if (typeof updateCurrentTimeWidget === "function") {
        updateCurrentTimeWidget();
    }

    if (typeof updateSunTimeWidget === "function") {
        updateSunTimeWidget();
    }

    if (typeof updateWorldTimeWidget === "function") {
        updateWorldTimeWidget();
    }
}

function updateControlBox() {
    const controlBox = document.querySelector(
        "[data-control-box]"
    );

    if (!controlBox) {
        return;
    }

    controlBox.innerHTML = state.isSettingsOpen
        ? renderControlBox()
        : "";
}

function updateSettingsDrawer() {
    const settingsLayer = document.querySelector(
        "[data-settings-layer]"
    );

    if (!settingsLayer) {
        return;
    }

    settingsLayer.classList.toggle(
        "is-open",
        state.isSettingsOpen
    );

    settingsLayer.setAttribute(
        "aria-hidden",
        String(!state.isSettingsOpen)
    );
}

// 배치 편집 표시만 부분 갱신
function updateDashboardEditModeUi() {
    const board = document.querySelector(
        "[data-dashboard-board]"
    );

    if (!board) {
        return;
    }

    board
        .querySelectorAll(
            ":scope > [data-layout-item]"
        )
        .forEach((item) => {
            item.classList.toggle(
                "is-layout-editing",
                state.isEditMode
            );
        });

    board
        .querySelectorAll(".widget")
        .forEach((card) => {
            card.classList.toggle(
                "is-layout-editing",
                state.isEditMode
            );
        });

    board
        .querySelectorAll(
            ":scope > [data-layout-item]"
        )
        .forEach((item) => {
            const isScheduleGroup =
                item.classList.contains(
                    "schedule-calendar-group"
                );

            updateDashboardGroupMoveGuide(
                item,
                isScheduleGroup
                    ? "더블클릭으로 묶음 이동"
                    : "더블클릭으로 이동"
            );
        });

    board
        .querySelectorAll(
            "[data-widget-id]"
        )
        .forEach((card) => {
            const widgetId = Number(
                card.dataset.widgetId
            );

            const widget = state.widgets.find(
                (item) => {
                    return item.id === widgetId;
                }
            );

            if (!widget) {
                return;
            }

            const header = card.querySelector(
                ":scope > .widget-header"
            );

            if (!header) {
                return;
            }

            const isTimeGroupChild = Boolean(
                card.closest(
                    ".header-time-group"
                )
            );

            header.outerHTML = renderWidgetHeader(
                widget,
                0,
                1,
                {
                    suppressActions:
                    isTimeGroupChild
                }
            );
        });

    syncLayoutEditClass();
    scheduleDashboardLayout({
        animate: false
    });
}

// 묶음 위젯의 이동 안내만 추가하거나 제거
function updateDashboardGroupMoveGuide(
    group,
    text
) {
    if (!group) {
        return;
    }

    const guide = group.querySelector(
        ":scope > .dashboard-group-move-guide"
    );

    if (!state.isEditMode) {
        guide?.remove();
        return;
    }

    if (guide) {
        guide.textContent = text;
        return;
    }

    group.insertAdjacentHTML(
        "afterbegin",
        `
            <span class="dashboard-group-move-guide">
                ${text}
            </span>
        `
    );
}

function updateHeaderWidget() {
    const currentTimeSlot = document.querySelector(
        "[data-time-current-slot]"
    );

    if (!currentTimeSlot) {
        return;
    }

    currentTimeSlot.innerHTML = renderHeaderWidget();

    if (typeof updateCurrentTimeWidget === "function") {
        updateCurrentTimeWidget();
    }

    if (typeof updateSunTimeWidget === "function") {
        updateSunTimeWidget();
    }

    scheduleDashboardLayout({
        animate: false
    });
}

// 저장된 배치 정보만 현재 DOM에 적용한다.
// 로그인 상태 확인 후 보드 전체를 다시 만들지 않기 위한 처리다.
function applyDashboardLayoutModelToDom() {
    const board = document.querySelector(
        "[data-dashboard-board]"
    );

    if (!board) {
        return;
    }

    const itemMap = new Map(
        [
            ...board.querySelectorAll(
                ":scope > [data-layout-item]"
            )
        ].map((item) => {
            return [
                item.dataset.layoutKey,
                item
            ];
        })
    );

    getVisibleDashboardLayoutItems()
        .forEach((descriptor) => {
            const item = itemMap.get(
                descriptor.key
            );

            if (!item || !descriptor.entry) {
                return;
            }

            item.dataset.layoutColumn = String(
                descriptor.entry.column
            );

            item.dataset.layoutSpan = String(
                descriptor.span
            );

            // append는 요소를 새로 만들지 않고 DOM 순서만 옮긴다.
            board.append(item);
        });

    observeDashboardBoardItems({
        animate: false
    });
}

function updateAuthWidget() {
    const authWidget = document.querySelector(
        "[data-auth-widget]"
    );

    if (authWidget) {
        authWidget.innerHTML = renderAuthWidget();
    }

    if (!dashboardRendered) {
        return;
    }

    const currentMemberId =
        getCurrentDashboardMemberId();

    // 정상적인 최초 로딩에서는 이미 같은 회원 배치가 적용되어 있다.
    if (currentMemberId === dashboardInitialMemberId) {
        return;
    }

    // 로그인 정보가 예외적으로 늦게 도착한 경우에만 배치를 한 번 보정한다.
    dashboardInitialMemberId = currentMemberId;

    const layoutLoaded =
        loadWidgetOrderForCurrentUser();

    if (layoutLoaded) {
        applyDashboardLayoutModelToDom();
    }
}

function syncDashboardBoard() {
    const board = document.querySelector(
        "[data-dashboard-board]"
    );

    if (!board) {
        return;
    }

    destroyStockWidgetRuntime();
    ensureDashboardLayoutModel();

    board.innerHTML = renderDashboardBoardContent();

    initStockSwiper();
    observeDashboardBoardItems({
        animate: true
    });

    if (typeof updateCurrentTimeWidget === "function") {
        updateCurrentTimeWidget();
    }

    if (typeof updateSunTimeWidget === "function") {
        updateSunTimeWidget();
    }

    if (typeof updateWorldTimeWidget === "function") {
        updateWorldTimeWidget();
    }

    syncLayoutEditClass();
}

function renderDashboard() {
    updateSettingsDrawer();
    updateControlBox();
    updateGlobalBanner();
    loadWidgetOrderForCurrentUser();
    syncDashboardBoard();
}

function render() {
    renderDashboard();
}

async function autoSyncGoogleCalendarFromDashboard() {
    if (
        !state.currentUser
        || !state.googleCalendarConnected
    ) {
        return false;
    }

    try {
        const response = await fetch(
            "/api/calendar/auto-sync",
            {
                method: "POST",
                credentials: "same-origin",
                cache: "no-store"
            }
        );

        const data = await response.json();

        if (
            !response.ok
            || data.success === false
        ) {
            if (response.status === 403) {
                state.googleCalendarConnected = false;
            }

            return false;
        }

        state.todayScheduleItems =
            data.eventList || [];
        state.todayScheduleError = "";
        refreshScheduleWidget();

        return true;
    } catch (error) {
        console.warn(
            "메인 화면 구글 일정 동기화 실패:",
            error
        );

        return false;
    }
}

let calendarWidgetRefreshRunning = false;

async function refreshCalendarWidgets(
    syncGoogle = true
) {
    if (calendarWidgetRefreshRunning) {
        return;
    }

    calendarWidgetRefreshRunning = true;

    try {
        const synced = syncGoogle
            ? await autoSyncGoogleCalendarFromDashboard()
            : false;

        await Promise.all([
            synced
                ? Promise.resolve()
                : fetchTodaySchedule(false),
            fetchMiniCalendarEventDates()
        ]);
    } catch (error) {
        console.warn(
            "캘린더 위젯 갱신 실패:",
            error
        );
    } finally {
        calendarWidgetRefreshRunning = false;
    }
}

if ("BroadcastChannel" in window) {
    const calendarChannel = new BroadcastChannel(
        "calendar-events"
    );

    calendarChannel.addEventListener(
        "message",
        function (event) {
            if (
                event.data?.type
                !== "calendar-changed"
            ) {
                return;
            }

            refreshCalendarWidgets(false);
        }
    );
}

window.addEventListener(
    "message",
    function (event) {
        if (event.origin !== window.location.origin) {
            return;
        }

        if (
            event.data?.type
            !== "calendar-changed"
        ) {
            return;
        }

        refreshCalendarWidgets(false);
    }
);

window.addEventListener(
    "storage",
    function (event) {
        if (event.key !== "calendar-changed-at") {
            return;
        }

        refreshCalendarWidgets(false);
    }
);

window.addEventListener(
    "focus",
    async function () {
        try {
            await fetchMiniCalendarMonthData(false);
            await refreshCalendarWidgets(true);
        } catch (error) {
            console.warn(
                "화면 복귀 후 캘린더 갱신 실패:",
                error
            );
        }
    }
);

startDashboard();
