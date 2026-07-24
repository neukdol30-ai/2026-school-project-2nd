function getWorldTimeWidget() {
    return state.widgets.find((widget) => {
        const type = String(widget.type || "")
            .toLowerCase()
            .replace(/[\s_-]/g, "");

        const title = String(widget.title || "")
            .replace(/\s/g, "");

        return type === "worldtime"
            || title === "세계시간";
    });
}

function renderHeaderWorldTimeWidget() {
    const worldTimeWidget =
        getWorldTimeWidget();

    if (
        !worldTimeWidget
        || !worldTimeWidget.visible
    ) {
        return "";
    }

    return renderWidget(
        worldTimeWidget,
        0,
        1
    );
}

function updateHeaderWorldTimeWidget() {
    const worldTimeSlot =
        document.querySelector(
            "[data-world-time-widget]"
        );

    if (!worldTimeSlot) {
        return;
    }

    worldTimeSlot.innerHTML =
        renderHeaderWorldTimeWidget();

    if (
        typeof updateWorldTimeWidget
        === "function"
    ) {
        updateWorldTimeWidget();
    }
}

// 메인 화면 렌더
// 최초 화면 전체 생성
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
                <div class="header-time-group">
                    <section
                        class="header-widget-slot"
                        data-header-widget
                    >
                        ${renderHeaderWidget()}
                    </section>

                    <section
                        class="header-world-time-slot"
                        data-world-time-widget
                    >
                        ${renderHeaderWorldTimeWidget()}
                    </section>
                </div>

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
                        ${getMainWidgets()
        .map((widget, index) =>
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
                        class="widget-list"
                        data-widget-list="side"
                    >
                        ${getSideWidgets()
        .map((widget, index) =>
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

        <div data-global-footer>
            ${renderGlobalFooter()}
        </div>
    `;

    bindEvents();
    animateWidgetChanges(prevPositions);
    initStockSwiper();

    requestAnimationFrame(
        updateFollowColumn
    );
}


// 환경설정 위젯 관리 박스만 갱신
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


// 로그인 카드만 갱신
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


// 전체 HTML을 교체하지 않는 대시보드 갱신
function renderDashboard() {
    const prevPositions =
        captureWidgetPositions();

    updateSettingsDrawer();
    updateControlBox();
    updateAuthWidget();
    updateHeaderWidget();
    updateHeaderWorldTimeWidget();
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


// 이후 화면 변경은 필요한 영역만 갱신
function render() {
    renderDashboard();
}



// 구글 캘린더 자동 동기화
// 메인 화면에서 구글 캘린더 자동 동기화
async function autoSyncGoogleCalendarFromDashboard() {

    // 비로그인 상태이거나
    // 구글 캘린더가 연동되지 않은 경우 중단
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
            // 구글 캘린더 연동이 해제되었거나
            // 사용할 수 없는 상태
            if (response.status === 403) {
                state.googleCalendarConnected =
                    false;
            }

            return false;
        }

        // 동기화 결과로 받은 오늘 일정 저장
        state.todayScheduleItems =
            data.eventList || [];

        state.todayScheduleError = "";

        // 오늘 일정 위젯만 갱신
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


// 일정 갱신 요청 중복 실행 방지
let calendarWidgetRefreshRunning = false;


// 오늘 일정과 미니 캘린더 함께 갱신
async function refreshCalendarWidgets(
    syncGoogle = true
) {
    if (calendarWidgetRefreshRunning) {
        return;
    }

    calendarWidgetRefreshRunning = true;

    try {
        // 구글 캘린더 연동 상태이면 먼저 동기화
        const synced =
            syncGoogle
                ? await autoSyncGoogleCalendarFromDashboard()
                : false;

        await Promise.all([

            // 구글 동기화 결과에 오늘 일정이 포함되어 있으면
            // 오늘 일정 API를 다시 호출하지 않음
            synced
                ? Promise.resolve()
                : fetchTodaySchedule(false),

            // 현재 미니 캘린더에 일정 날짜 표시
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



// 캘린더 변경 신호 감지
// 캘린더 팝업에서 BroadcastChannel 신호 받기
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
                !== "calendar-changed"
            ) {
                return;
            }

            // 사이트 내부 일정 변경이므로
            // 구글 동기화 없이 바로 위젯 조회
            refreshCalendarWidgets(false);
        }
    );
}


// 팝업창에서 postMessage 변경 신호 받기
window.addEventListener(
    "message",
    function (event) {

        // 다른 사이트에서 보낸 메시지는 무시
        if (
            event.origin
            !== window.location.origin
        ) {
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


// BroadcastChannel 미지원 환경의 변경 신호
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


// 다른 화면이나 팝업에서
// 메인 화면으로 돌아왔을 때 갱신
window.addEventListener(
    "focus",
    async function () {
        try {
            // 미니 캘린더 월 정보 먼저 갱신
            await fetchMiniCalendarMonthData(false);

            // 오늘 일정과 일정 표시 날짜 갱신
            await refreshCalendarWidgets(true);

        } catch (error) {
            console.warn(
                "화면 복귀 후 캘린더 갱신 실패:",
                error
            );
        }
    }
);



// 최초 실행
// 최초 한 번만 전체 화면 생성
renderInitialPage();


// 팀원 담당 위젯 최초 실행
startCurrentTimeClock();
startWorldTimeClock();
fetchSunTime();
fetchNews();
fetchStocks();
fetchWeather();


// 오늘 일정과 미니 캘린더 최초 조회
Promise.all([
    fetchTodaySchedule(true),
    fetchMiniCalendarMonthData(true)
])
    .then(function () {

        // 최초 데이터 출력 후
        // 구글 캘린더가 연동된 경우 한 번 동기화
        return refreshCalendarWidgets(true);
    })
    .catch(function (error) {
        console.warn(
            "캘린더 최초 조회 실패:",
            error
        );
    });


// 5초마다 오늘 일정과 미니 캘린더 갱신
setInterval(
    function () {
        refreshCalendarWidgets(true);
    },
    5000
);