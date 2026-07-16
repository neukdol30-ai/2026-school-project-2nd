//rander 함수 생성
function render() {
    const app = document.querySelector("#app");
    const prevPositions = captureWidgetPositions();
    rememberStockSwiperIndex();

    app.innerHTML = `
        <div class="container">
            <div class="page-actions">
                <button data-action="toggle-edit">
                    ${state.isEditMode ? "설정 완료" : "환경설정"}
                </button>
            </div>

            ${state.isEditMode ? renderControlBox() : ""}

            <main class="dashboard-layout">
                <section class="widget-column main-column">
                    <h2 class="column-title">메인 위젯</h2>
                    <div class="widget-list">
                        ${getMainWidgets().map((widget, index) =>
        renderWidget(widget, index, getMainWidgets().length)
    ).join("")}
                    </div>
                </section>

                <aside class="widget-column side-column">
                    <h2 class="column-title">보조 위젯</h2>
                    ${renderAuthWidget()}
                    <div class="widget-list">
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

render();
startCurrentTimeClock();
fetchSunTime();
fetchNews();
fetchStocks();
fetchWeather();
