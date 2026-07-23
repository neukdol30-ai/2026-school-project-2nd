// 메인 화면 렌더링
// 최초 화면 전체 생성
function renderInitialPage() {
    const app = document.querySelector("#app");

    if (!app) {
        return;
    }

    const prevPositions = captureWidgetPositions();

    rememberStockSwiperIndex();

    app.innerHTML = `
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
                class="page-actions"
                data-page-actions
            >
                <button data-action="toggle-edit">
                    ${
        state.isEditMode
            ? "설정 완료"
            : "환경설정"
    }
                </button>
            </div>

            <div data-control-box>
                ${
        state.isEditMode
            ? renderControlBox()
            : ""
    }
            </div>

            <main class="dashboard-layout">

                <section class="widget-column main-column">

                    <div
                        class="widget-list"
                        data-widget-list="main"
                    >
                        ${
        getMainWidgets()
            .map(
                (widget, index) =>
                    renderWidget(
                        widget,
                        index,
                        getMainWidgets().length
                    )
            )
            .join("")
    }
                    </div>

                </section>

                <aside class="widget-column side-column">

                    <div
                        class="widget-list"
                        data-widget-list="side"
                    >
                        ${
        getSideWidgets()
            .map(
                (widget, index) =>
                    renderWidget(
                        widget,
                        index,
                        getSideWidgets().length
                    )
            )
            .join("")
    }
                    </div>

                </aside>

            </main>

        </div>
    `;

    // 최초 화면 이벤트 연결
    bindEvents();

    // 위젯 이동 애니메이션
    animateWidgetChanges(prevPositions);

    // 주식 슬라이드 초기화
    initStockSwiper();
}


// 환경설정 버튼 영역만 갱신
function updatePageActions() {
    const pageActions =
        document.querySelector(
            "[data-page-actions]"
        );

    if (!pageActions) {
        return;
    }

    pageActions.innerHTML = `
        <button data-action="toggle-edit">
            ${
        state.isEditMode
            ? "설정 완료"
            : "환경설정"
    }
        </button>
    `;
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
        state.isEditMode
            ? renderControlBox()
            : "";
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

    // 필요한 영역만 부분 갱신
    updatePageActions();
    updateControlBox();
    updateAuthWidget();

    // 메인·사이드 위젯 목록 갱신
    syncWidgetList("main");
    syncWidgetList("side");

    // 위젯 이동 애니메이션
    animateWidgetChanges(prevPositions);
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