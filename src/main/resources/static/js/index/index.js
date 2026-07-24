// 메인 화면 최초 생성
function renderInitialPage() {
    const app = document.querySelector("#app");

    if (!app) {
        return;
    }

    const prevPositions = captureWidgetPositions();

    rememberStockSwiperIndex();

    app.innerHTML = `
        <div class="global-banner-area">
            <div data-global-banner>
                ${renderGlobalBanner()}
            </div>
        </div>

        <div class="container">
            <header class="dashboard-header">
                <section
                    class="header-widget-slot"
                    data-header-widget
                >
                    ${renderHeaderWidget()}
                </section>

                <div class="header-logo-slot"></div>

                <div
                    class="header-auth-slot"
                    data-auth-widget
                >
                    ${renderAuthWidget()}
                </div>
            </header>

           

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

            <main class="dashboard-layout">
                <section class="widget-column main-column">
                    <div
                        class="widget-list"
                        data-widget-list="main"
                    >
                        ${renderWidgetList(getMainWidgets())}
                    </div>
                </section>

                <aside class="widget-column side-column">
                    <div
                        class="widget-list"
                        data-widget-list="side"
                    >
                        ${renderWidgetList(getSideWidgets())}
                    </div>
                </aside>
            </main>
        </div>

        <div data-global-footer>
            ${renderGlobalFooter()}
        </div>
    `;

    bindEvents();
    animateWidgetChanges(prevPositions);
    initStockSwiper();

    requestAnimationFrame(updateFollowColumn);
}

// 위젯 목록 HTML 생성
function renderWidgetList(widgetList) {
    return widgetList
        .map((widget, index) => {
            return renderWidget(
                widget,
                index,
                widgetList.length
            );
        })
        .join("");
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

// 로그인 카드 갱신
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

// 현재시간 카드 갱신
function updateHeaderWidget() {
    const headerWidget =
        document.querySelector(
            "[data-header-widget]"
        );

    if (!headerWidget) {
        return;
    }

    headerWidget.innerHTML =
        renderHeaderWidget();

    if (
        typeof updateCurrentTimeWidget
        === "function"
    ) {
        updateCurrentTimeWidget();
    }

    if (
        typeof updateSunTimeWidget
        === "function"
    ) {
        updateSunTimeWidget();
    }
}

// 필요한 화면 영역만 갱신
function renderDashboard() {
    const prevPositions =
        captureWidgetPositions();

    updateSettingsDrawer();
    updateControlBox();
    updateAuthWidget();
    updateHeaderWidget();
    updateGlobalBanner();

    syncWidgetList("main");
    syncWidgetList("side");

    animateWidgetChanges(
        prevPositions
    );

    requestAnimationFrame(
        updateFollowColumn
    );
}

// 공통 화면 갱신
function render() {
    renderDashboard();
}

// 구글 캘린더 자동 동기화
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

        let data = {};

        try {
            data = await response.json();
        } catch (error) {
            data = {};
        }

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
            Array.isArray(data.eventList)
                ? data.eventList
                : [];

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

// 일정 갱신 중복 실행 방지
let calendarWidgetRefreshRunning = false;

// 오늘 일정과 미니 캘린더 갱신
async function refreshCalendarWidgets(
    syncGoogle = true
) {
    if (calendarWidgetRefreshRunning) {
        return;
    }

    calendarWidgetRefreshRunning = true;

    try {
        const googleSynced =
            syncGoogle
                ? await autoSyncGoogleCalendarFromDashboard()
                : false;

        const refreshJobs = [];

        if (
            !googleSynced
            && typeof fetchTodaySchedule
            === "function"
        ) {
            refreshJobs.push(
                fetchTodaySchedule(false)
            );
        }

        if (
            typeof fetchMiniCalendarMonthData
            === "function"
        ) {
            refreshJobs.push(
                fetchMiniCalendarMonthData(false)
            );
        } else if (
            typeof fetchMiniCalendarEventDates
            === "function"
        ) {
            refreshJobs.push(
                fetchMiniCalendarEventDates()
            );
        }

        await Promise.allSettled(
            refreshJobs
        );

        requestAnimationFrame(
            updateFollowColumn
        );
    } catch (error) {
        console.warn(
            "캘린더 위젯 갱신 실패:",
            error
        );
    } finally {
        calendarWidgetRefreshRunning =
            false;
    }
}

// 다른 캘린더 화면의 변경 감지
let calendarBroadcastChannel = null;

if ("BroadcastChannel" in window) {
    calendarBroadcastChannel =
        new BroadcastChannel(
            "calendar-events"
        );

    calendarBroadcastChannel.addEventListener(
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

// BroadcastChannel 미지원 환경의 변경 감지
window.addEventListener(
    "storage",
    function (event) {
        if (
            event.key
            !== "calendar-changed-at"
        ) {
            return;
        }

        refreshCalendarWidgets(false);
    }
);

// 메인 화면으로 돌아왔을 때 일정 갱신
window.addEventListener(
    "focus",
    function () {
        refreshCalendarWidgets(true);
    }
);

// 최초 화면 생성
renderInitialPage();

if (
    typeof initializeMyPage
    === "function"
) {
    initializeMyPage();
}

if (
    typeof startCurrentTimeClock
    === "function"
) {
    startCurrentTimeClock();
}

if (
    typeof fetchSunTime
    === "function"
) {
    fetchSunTime();
}

if (
    typeof fetchNews
    === "function"
) {
    fetchNews();
}

if (
    typeof fetchStocks
    === "function"
) {
    fetchStocks();
}

if (
    typeof fetchWeather
    === "function"
) {
    fetchWeather();
}

// 일정과 미니 캘린더 최초 조회
const initialCalendarJobs = [];

if (
    typeof fetchTodaySchedule
    === "function"
) {
    initialCalendarJobs.push(
        fetchTodaySchedule(true)
    );
}

if (
    typeof fetchMiniCalendarMonthData
    === "function"
) {
    initialCalendarJobs.push(
        fetchMiniCalendarMonthData(true)
    );
} else if (
    typeof fetchMiniCalendarEventDates
    === "function"
) {
    initialCalendarJobs.push(
        fetchMiniCalendarEventDates()
    );
}

Promise.allSettled(
    initialCalendarJobs
)
    .then(function () {
        return refreshCalendarWidgets(true);
    })
    .catch(function (error) {
        console.warn(
            "캘린더 최초 조회 실패:",
            error
        );
    });

// 일정과 미니 캘린더 주기적 갱신
setInterval(
    function () {
        refreshCalendarWidgets(true);
    },
    5000
);