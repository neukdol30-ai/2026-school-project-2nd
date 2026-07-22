//rander 함수 생성
function renderInitialPage() {
    const app = document.querySelector("#app");
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
            <div class="page-actions" data-page-actions>
                <button data-action="toggle-edit">
                    ${state.isEditMode ? "설정 완료" : "환경설정"}
                </button>
            </div>

            <div data-control-box>
            ${state.isEditMode ? renderControlBox() : ""}
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
    `;

    bindEvents();
    animateWidgetChanges(prevPositions);
    initStockSwiper();
}

// 환경설정 버튼 영역만 갱신
function updatePageActions() {
    const pageActions = document.querySelector("[data-page-actions]");

    if (!pageActions) {
        return;
    }

    pageActions.innerHTML = `
        <button data-action="toggle-edit">
            ${state.isEditMode ? "설정 완료" : "환경설정"}
        </button>
    `;
}

// 환경설정 위젯 관리 박스만 갱신
function updateControlBox() {
    const controlBox = document.querySelector("[data-control-box]");

    if (!controlBox) {
        return;
    }

    controlBox.innerHTML = state.isEditMode
        ? renderControlBox()
        : "";
}

// 로그인 카드만 갱신
function updateAuthWidget() {
    const authWidget = document.querySelector("[data-auth-widget]");

    if (!authWidget) {
        return;
    }

    authWidget.innerHTML = renderAuthWidget();
}

// 전체 HTML을 교체하지 않는 대시보드 갱신
function renderDashboard() {
    const prevPositions = captureWidgetPositions();

    updatePageActions();
    updateControlBox();
    updateAuthWidget();

    syncWidgetList("main");
    syncWidgetList("side");

    animateWidgetChanges(prevPositions);
}

// 이후 화면 변경은 필요한 영역만 갱신
function render() {
    renderDashboard();
}

renderInitialPage();

if (typeof initializeMyPage === "function") {
    initializeMyPage();
}

startCurrentTimeClock();
fetchSunTime();
fetchNews();
fetchStocks();
fetchWeather();
