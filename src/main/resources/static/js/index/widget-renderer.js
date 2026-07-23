//위젯 관리 박스
function renderControlBox() {
    return `
        <section class="control-box">
            <h2>위젯 관리</h2>
            
            <button
                class="settings-layout-button"
                data-action="start-layout-edit"
            >
                위젯 배치 편집
            </button>

            <div class="control-buttons">
                ${state.widgets.map((widget) => `
                    <button
                        class="${widget.visible ? "active" : ""}"
                        data-action="toggle-widget"
                        data-id="${widget.id}"
                    >
                        ${widget.icon} ${widget.title}
                    </button>
                `).join("")}
            </div>
        </section>
    `;
}

// 위젯 공통 헤더
function renderWidgetHeader(widget, index, widgetCount) {
    return `
        <div class="widget-header">
            <div>
                <div class="widget-title">
                    ${widget.icon} ${widget.title}
                </div>
                <div class="widget-desc">
                    ${widget.description}
                </div>
            </div>

            <div class="widget-actions">
                ${state.isEditMode ? `
                    <span
                        class="widget-move-guide"
                        title="더블클릭하여 이동"
                    >
                        ↕
                    </span>

                    <button
                        class="danger"
                        data-action="toggle-widget"
                        data-id="${widget.id}"
                    >
                        숨김
                    </button>
                ` : ""}
            </div>
        </div>
    `;
}



// 위젯 카드
function renderWidget(widget, index, widgetCount) {
    return `
        <article
            class="widget ${
                widget.zone === "main"
                    ? "main-widget"
                    : "side-widget"
            } ${state.isEditMode ? "is-layout-editing" : ""}"
            data-widget-id="${widget.id}"
            draggable="${state.isEditMode}"
        >
            ${renderWidgetHeader(widget, index, widgetCount)}

            ${widget.collapsed ? "" : `
                <div class="widget-content" data-widget-content="${widget.id}">
                    ${renderWidgetContent(widget)}
                </div>
            `}
        </article>
    `;
}

// 대시보드 상단 브랜드 영역
function renderDashboardBrand() {
    return `
        <div class="dashboard-brand">
            <div class="dashboard-brand-copy">
                <h1>MY DASHBOARD</h1>
                <p>일정과 생활 정보를 한곳에서 관리하세요.</p>
            </div>

            <div
                class="dashboard-today"
                aria-label="현재 날짜와 시간"
            >
                <span class="dashboard-today-icon" aria-hidden="true">▣</span>
                <strong id="dashboard-current-date">
                    ----년 --월 --일
                </strong>

                <span class="dashboard-today-divider" aria-hidden="true"></span>

                <span class="dashboard-today-icon" aria-hidden="true">◷</span>
                <strong id="dashboard-current-time">
                    --:--
                </strong>
            </div>
        </div>
    `;
}

//공용 위젯 랜더링 연결
function renderWidgetContent(widget) {
    if (widget.type === "news") {
        return renderNewsWidget();
    }

    if (widget.type === "issue") {
        return renderIssueWidget();
    }

    if (widget.type === "schedule") {
        return renderScheduleWidget();
    }

    if (widget.type === "stock") {
        return renderStockWidget();
    }

    if (widget.type === "weather") {
        return renderWeatherWidget();
    }

    if (widget.type === "calculator") {
        return renderCalculator();
    }

    if (widget.type === "miniCalendar") {
        return renderMiniCalendar();
    }

    if (widget.type === "memo") {
        return renderMemo();
    }
    if (widget.type === "currentTime") {
        return renderCurrentTimeWidget();
    }

    return "";
}
