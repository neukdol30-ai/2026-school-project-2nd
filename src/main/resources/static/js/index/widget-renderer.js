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
                       ${widget.title}
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
                     ${widget.title}
                </div>
            </div>

            <div class="widget-actions">
                ${state.isEditMode ? `
                    <span
                        class="widget-drag-handle"
                        draggable="true"
                        data-drag-handle
                        title="위젯 이동"
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
            class="widget ${widget.zone === "main" ? "main-widget" : "side-widget"}"
            data-widget-id="${widget.id}"
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

// 상단 좌측 고정 위젯 렌더링
function renderHeaderWidget() {
    const headerWidget = state.widgets.find(
        (widget) => widget.id === state.headerWidgetId
    );

    // 설정에서 상단 위젯을 비우는 경우를 위한 임시 화면
    if (!headerWidget || !headerWidget.visible) {
        return `
            <article class="widget header-widget">
                <div class="widget-content">
                    <p class="widget-desc">상단 위젯이 비어 있습니다.</p>
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
                ${renderWidgetContent(headerWidget)}
            </div>
        </article>
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
