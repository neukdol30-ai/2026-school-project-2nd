//rander 함수 생성
function renderInitialPage() {
    const app = document.querySelector("#app");
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
            <div class="page-actions" data-page-actions>
                <button data-action="toggle-edit">
                    ${state.isSettingsOpen ? "설정 완료" : "환경설정"}
                </button>
            </div>

            <div
                class="settings-layer ${state.isSettingsOpen ? "is-open" : ""}"
                data-settings-layer
                aria-hidden="${!state.isSettingsOpen}"
            >
            
            <button
                class="settings-backdrop"
                type="button"
                data-action="close-edit"
                aria-label="환경설정 닫기"
            ></button>

            <aside class="settings-drawer" aria-label="환경설정">
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
                    <div class="widget-list" data-widget-list="main">
                        ${getMainWidgets().map((widget, index) =>
                        renderWidget(widget, index, getMainWidgets().length)
                        ).join("")}
                    </div>
                </section>

                <aside class="widget-column side-column">
                    <div class="widget-list" data-widget-list="side">
                        ${getSideWidgets().map((widget, index) =>
                        renderWidget(widget, index, getSideWidgets().length)
                        ).join("")}
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

// 환경설정 버튼 영역만 갱신
function updatePageActions() {
    const pageActions = document.querySelector("[data-page-actions]");

    if (!pageActions) {
        return;
    }

    pageActions.innerHTML = state.isSettingsOpen
        ? ""
        : state.isEditMode
            ? `
            <button data-action="finish-layout-edit">
                배치 완료
            </button>
        `
            : `
            <button data-action="toggle-edit">
                환경설정
            </button>
        `;
}

// 환경설정 위젯 관리 박스만 갱신
function updateControlBox() {
    const controlBox = document.querySelector("[data-control-box]");

    if (!controlBox || !state.isSettingsOpen) {
        return;
    }
    controlBox.innerHTML = renderControlBox();
}

// 오른쪽 설정 서랍 열림/닫힘 상태 갱신
function updateSettingsDrawer() {
    const settingsLayer = document.querySelector(
        "[data-settings-layer]"
    );

    if (!settingsLayer) {
        return;
    }

    settingsLayer.classList.toggle("is-open", state.isSettingsOpen);

    settingsLayer.setAttribute(
        "aria-hidden",
        String(!state.isSettingsOpen)
    );
}

// 로그인 카드만 갱신
function updateAuthWidget() {
    const authWidget = document.querySelector("[data-auth-widget]");

    if (!authWidget) {
        return;
    }

    authWidget.innerHTML = renderAuthWidget();
}

// 상단 좌측 고정 위젯만 갱신
function updateHeaderWidget() {
    const headerWidget = document.querySelector("[data-header-widget]");

    if (!headerWidget) {
        return;
    }

    headerWidget.innerHTML = renderHeaderWidget();

    // 현재시간 위젯이 상단에 있을 경우 즉시 시간과 일출·일몰을 채움
    updateCurrentTimeWidget();
    updateSunTimeWidget();
}

// 전체 HTML을 교체하지 않는 대시보드 갱신
function renderDashboard() {
    const prevPositions = captureWidgetPositions();

    updatePageActions();
    updateSettingsDrawer();
    updateControlBox();
    updateAuthWidget();
    updateHeaderWidget();
    updateGlobalBanner();

    syncWidgetList("main");
    syncWidgetList("side");

    requestAnimationFrame(updateFollowColumn);

    animateWidgetChanges(prevPositions);
}

// 이후 화면 변경은 필요한 영역만 갱신
function render() {
    renderDashboard();
}

renderInitialPage();
startCurrentTimeClock();
fetchSunTime();
fetchNews();
fetchStocks();
fetchWeather();
