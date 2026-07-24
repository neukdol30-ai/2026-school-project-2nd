// 위젯 관리 박스
function renderControlBox() {
    return `
        <section class="control-box">
            <h2>위젯 관리</h2>

            <button
                class="settings-layout-button"
                type="button"
                data-action="start-layout-edit"
            >
                위젯 배치 편집
            </button>

            <div class="control-buttons">
                ${state.widgets
        .map((widget) => {
            return `
                            <button
                                type="button"
                                class="${widget.visible ? "active" : ""}"
                                data-action="toggle-widget"
                                data-id="${widget.id}"
                            >
                                ${widget.title}
                            </button>
                        `;
        })
        .join("")}
            </div>
        </section>
    `;
}

// 일정과 캘린더는 네 기존 제목 디자인 사용
function isScheduleCalendarWidget(widget) {
    return widget.id === 3 || widget.id === 7;
}

// 위젯 공통 헤더
function renderWidgetHeader(
    widget,
    index,
    widgetCount
) {
    const useCalendarDesign =
        isScheduleCalendarWidget(widget);

    return `
        <div class="widget-header">
            <div>
                <div class="widget-title">
                    ${
        useCalendarDesign && widget.icon
            ? `${widget.icon} `
            : ""
    }${widget.title}
                </div>

                ${
        useCalendarDesign && widget.description
            ? `
                            <div class="widget-desc">
                                ${widget.description}
                            </div>
                        `
            : ""
    }
            </div>

            <div class="widget-actions">
                       <div class="widget-actions">
                ${widget.type === "stock" && !state.isEditMode ? `
                <div class="stock-header-tools">
                    <span class="stock-header-updated">
                        ${formatStockUpdatedAt(state.stockUpdatedAt)}
                    </span>
        
                    <button
                        class="stock-refresh-button"
                        type="button"
                        data-action="refresh-stocks"
                        aria-label="증권 정보 새로고침"
                        title="새로고침"
                    >
                        ↻
                    </button>
                </div>
                ` : ""}
                ${
        state.isEditMode
            ? `
                            <span
                                class="widget-move-guide"
                                title="더블클릭하여 이동"
                            >
                                ↕
                            </span>

                            <button
                                class="danger"
                                type="button"
                                data-action="toggle-widget"
                                data-id="${widget.id}"
                            >
                                숨김
                            </button>
                        `
            : ""
    }
            </div>
        </div>
    `;
}

// 위젯 카드
function renderWidget(
    widget,
    index,
    widgetCount
) {
    return `
        <article
            class="widget ${
        widget.zone === "main"
            ? "main-widget"
            : "side-widget"
    } ${
        state.isEditMode
            ? "is-layout-editing"
            : ""
    }"
            data-widget-id="${widget.id}"
        >
            ${renderWidgetHeader(
        widget,
        index,
        widgetCount
    )}

            ${
        widget.collapsed
            ? ""
            : `
                        <div
                            class="widget-content"
                            data-widget-content="${widget.id}"
                        >
                            ${renderWidgetContent(widget)}
                        </div>
                    `
    }
        </article>
    `;
}

// 상단 현재시간 위젯
function renderHeaderWidget() {
    const headerWidget =
        state.widgets.find((widget) => {
            return widget.id
                === state.headerWidgetId;
        });

    if (
        !headerWidget
        || !headerWidget.visible
    ) {
        return `
            <article class="widget header-widget">
                <div class="widget-content">
                    <p class="widget-desc">
                        상단 위젯이 비어 있습니다.
                    </p>
                </div>
            </article>
        `;
    }

    return `
        <article
            class="widget header-widget"
            data-header-widget-id="${headerWidget.id}"
        >
            <div class="widget-header">
                <div>
                    <div class="widget-title">
                        ${headerWidget.title}
                    </div>
                </div>
            </div>

            <div
                class="widget-content"
                data-widget-content="${headerWidget.id}"
            >
                ${renderWidgetContent(
        headerWidget
    )}
            </div>
        </article>
    `;
}

// 위젯 내용 연결
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