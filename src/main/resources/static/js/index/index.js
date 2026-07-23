// 메인 화면 최초 생성
function renderInitialPage() {
    const app = document.querySelector("#app");

    if (!app) {
        return;
    }

    const prevPositions =
        typeof captureWidgetPositions === "function"
            ? captureWidgetPositions()
            : new Map();

    if (typeof rememberStockSwiperIndex === "function") {
        rememberStockSwiperIndex();
    }

    app.innerHTML = `
        <div class="container">
            <header class="dashboard-header">
                ${renderDashboardBrand()}
            </header>

            <div
                class="page-actions"
                data-page-actions
            >
                <button
                    type="button"
                    data-action="toggle-edit"
                >
                    환경설정
                </button>
            </div>

            <div
                class="settings-layer"
                data-settings-layer
                aria-hidden="true"
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

                    <div data-control-box></div>
                </aside>
            </div>

            <main class="dashboard-layout">
                <section class="widget-column main-column">
                    <div
                        class="widget-list"
                        data-widget-list="main"
                    >
                        ${getMainWidgets()
        .map(
            (widget, index) =>
                renderWidget(
                    widget,
                    index,
                    getMainWidgets().length
                )
        )
        .join("")}
                    </div>
                </section>

                <aside class="widget-column side-column">
                    <div
                        class="side-auth-slot"
                        data-auth-widget
                    >
                        ${renderAuthWidget()}
                    </div>

                    <div
                        class="widget-list"
                        data-widget-list="side"
                    >
                        ${getSideWidgets()
        .map(
            (widget, index) =>
                renderWidget(
                    widget,
                    index,
                    getSideWidgets().length
                )
        )
        .join("")}
                    </div>
                </aside>
            </main>
        </div>
    `;

    bindEvents();

    if (typeof animateWidgetChanges === "function") {
        animateWidgetChanges(prevPositions);
    }

    if (typeof initStockSwiper === "function") {
        initStockSwiper();
    }
}

// 환경설정 버튼 갱신
function updatePageActions() {
    const pageActions =
        document.querySelector(
            "[data-page-actions]"
        );

    if (!pageActions) {
        return;
    }

    pageActions.innerHTML =
        state.isSettingsOpen
            ? ""
            : state.isEditMode
                ? `
                    <button
                        type="button"
                        data-action="finish-layout-edit"
                    >
                        배치 완료
                    </button>
                `
                : `
                    <button
                        type="button"
                        data-action="toggle-edit"
                    >
                        환경설정
                    </button>
                `;
}

// 환경설정 내용 갱신
function updateControlBox() {
    const controlBox =
        document.querySelector(
            "[data-control-box]"
        );

    if (!controlBox) {
        return;
    }

    controlBox.innerHTML =
        state.isSettingsOpen
            ? renderControlBox()
            : "";
}

// 환경설정 서랍 갱신
function updateSettingsDrawer() {
    const settingsLayer =
        document.querySelector(
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

// 로그인 위젯 갱신
function updateAuthWidget() {
    const authWidget =
        document.querySelector(
            "[data-auth-widget]"
        );

    if (!authWidget) {
        return;
    }

    authWidget.innerHTML =
        renderAuthWidget();
}

// 필요한 영역만 갱신
function renderDashboard() {
    const prevPositions =
        typeof captureWidgetPositions === "function"
            ? captureWidgetPositions()
            : new Map();

    updatePageActions();
    updateSettingsDrawer();
    updateControlBox();
    updateAuthWidget();

    syncWidgetList("main");
    syncWidgetList("side");


    if (typeof animateWidgetChanges === "function") {
        animateWidgetChanges(prevPositions);
    }

    if (typeof animateWidgetChanges === "function") {
        animateWidgetChanges(prevPositions);
    }
}

function render() {
    renderDashboard();
}

// 메인 화면에서 구글 일정 동기화
async function autoSyncGoogleCalendarFromDashboard() {
    if (
        !state.currentUser
        || !state.googleCalendarConnected
    ) {
        return false;
    }

    try {
        const response =
            await fetch(
                "/api/calendar/auto-sync",
                {
                    method: "POST",
                    credentials: "same-origin",
                    cache: "no-store"
                }
            );

        const data =
            await response.json();

        if (
            !response.ok
            || data.success === false
        ) {
            if (response.status === 403) {
                state.googleCalendarConnected =
                    false;
            }

            return false;
        }

        state.todayScheduleItems =
            data.eventList || [];

        state.todayScheduleError = "";

        if (
            typeof refreshScheduleWidget
            === "function"
        ) {
            refreshScheduleWidget();
        }

        return true;

    } catch (error) {
        console.warn(
            "메인 화면 구글 일정 동기화 실패:",
            error
        );

        return false;
    }
}

// 오늘 일정과 미니 캘린더 갱신
let calendarWidgetRefreshRunning = false;

async function refreshCalendarWidgets(
    syncGoogle = true
) {
    if (calendarWidgetRefreshRunning) {
        return;
    }

    calendarWidgetRefreshRunning = true;

    try {
        const synced =
            syncGoogle
                ? await autoSyncGoogleCalendarFromDashboard()
                : false;

        const jobs = [];

        if (
            !synced
            && typeof fetchTodaySchedule
            === "function"
        ) {
            jobs.push(
                fetchTodaySchedule(false)
            );
        }

        if (
            typeof fetchMiniCalendarEventDates
            === "function"
        ) {
            jobs.push(
                fetchMiniCalendarEventDates()
            );
        }

        await Promise.allSettled(jobs);

    } finally {
        calendarWidgetRefreshRunning = false;
    }
}

// 다른 페이지에서 일정이 바뀐 경우
if ("BroadcastChannel" in window) {
    const calendarChannel =
        new BroadcastChannel(
            "calendar-events"
        );

    calendarChannel.addEventListener(
        "message",
        function (event) {
            if (
                event.data?.type
                === "calendar-changed"
            ) {
                refreshCalendarWidgets(false);
            }
        }
    );
}

// BroadcastChannel 미지원 환경
window.addEventListener(
    "storage",
    function (event) {
        if (
            event.key
            === "calendar-changed-at"
        ) {
            refreshCalendarWidgets(false);
        }
    }
);

// 메인 화면으로 돌아왔을 때 갱신
window.addEventListener(
    "focus",
    async function () {
        if (
            typeof fetchMiniCalendarMonthData
            === "function"
        ) {
            await fetchMiniCalendarMonthData(false);
        }

        await refreshCalendarWidgets(true);
    }
);

// 최초 실행
renderInitialPage();

if (typeof startCurrentTimeClock === "function") {
    startCurrentTimeClock();
}

if (typeof fetchSunTime === "function") {
    fetchSunTime();
}

if (typeof fetchNews === "function") {
    fetchNews();
}

if (typeof fetchStocks === "function") {
    fetchStocks();
}

if (typeof fetchWeather === "function") {
    fetchWeather();
}

const initialJobs = [];

if (typeof fetchTodaySchedule === "function") {
    initialJobs.push(
        fetchTodaySchedule(true)
    );
}

if (
    typeof fetchMiniCalendarMonthData
    === "function"
) {
    initialJobs.push(
        fetchMiniCalendarMonthData(true)
    );
}

Promise.allSettled(initialJobs)
    .then(function () {
        return refreshCalendarWidgets(true);
    })
    .catch(function (error) {
        console.warn(
            "캘린더 최초 조회 실패:",
            error
        );
    });

// 5초마다 일정과 미니 캘린더 갱신
setInterval(
    function () {
        refreshCalendarWidgets(true);
    },
    5000
);