//위젯 관리 박스
function renderControlBox() {
    return `
        <section class="control-box">
            <h2>위젯 관리</h2>

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

//위젯 카드
function renderWidget(widget, index, widgetCount) {
    return `
        <article class="widget ${widget.zone === "main" ? "main-widget" : "side-widget"}"
        data-widget-id="${widget.id}">
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
                        <button
                            data-action="move-up"
                            data-id="${widget.id}"
                            ${index === 0 ? "disabled" : ""}
                        >
                            ↑
                        </button>

                        <button
                            data-action="move-down"
                            data-id="${widget.id}"
                            ${index === widgetCount - 1 ? "disabled" : ""}
                        >
                            ↓
                        </button>

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

            ${widget.collapsed ? "" : `
                <div class="widget-content">
                    ${renderWidgetContent(widget)}
                </div>
            `}
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

    return "";
}
