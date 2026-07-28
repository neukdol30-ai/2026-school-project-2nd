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

        <section class="theme-setting-box">
            <h2>화면 설정</h2>
            <p class="theme-setting-label">화면 스타일</p>

            <div
                class="theme-option-list"
                role="group"
                aria-label="화면 스타일 선택"
            >
                <button
                    class="theme-option-button ${
        state.theme === "light"
            ? "is-selected"
            : ""
    }"
                    type="button"
                    data-action="set-theme"
                    data-value="light"
                    data-theme-option="light"
                    aria-pressed="${String(
        state.theme === "light"
    )}"
                >
                    <span
                        class="theme-option-icon"
                        aria-hidden="true"
                    >
                        ☀
                    </span>

                    <span class="theme-option-text">
                        <strong>라이트 모드</strong>
                        <span>밝은 화면</span>
                    </span>

                    <span
                        class="theme-option-check"
                        aria-hidden="true"
                    >
                        ✓
                    </span>
                </button>

                <button
                    class="theme-option-button ${
        state.theme === "dark"
            ? "is-selected"
            : ""
    }"
                    type="button"
                    data-action="set-theme"
                    data-value="dark"
                    data-theme-option="dark"
                    aria-pressed="${String(
        state.theme === "dark"
    )}"
                >
                    <span
                        class="theme-option-icon"
                        aria-hidden="true"
                    >
                        ☾
                    </span>

                    <span class="theme-option-text">
                        <strong>다크 모드</strong>
                        <span>어두운 화면</span>
                    </span>

                    <span
                        class="theme-option-check"
                        aria-hidden="true"
                    >
                        ✓
                    </span>
                </button>
            </div>
        </section>
    `;
}

function isScheduleCalendarWidget(widget) {
    return widget.id === SCHEDULE_WIDGET_ID
        || widget.id === CALENDAR_WIDGET_ID;
}

function renderWidgetHeader(
    widget,
    index,
    widgetCount,
    options = {}
) {
    const useCalendarDesign =
        isScheduleCalendarWidget(widget);
    const suppressActions =
        Boolean(options.suppressActions);

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

            ${suppressActions ? "" : `
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

                    ${state.isEditMode ? `
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
                    ` : ""}
                </div>
            `}
        </div>
    `;
}

function renderWidget(
    widget,
    index,
    widgetCount,
    options = {}
) {
    const span = options.span
        || getDashboardWidgetSpan(widget);
    const sizeClass = options.sizeClass
        || (span === 2
            ? "main-widget"
            : "side-widget");
    const descriptor = options.descriptor || null;
    const layoutItem = Boolean(options.layoutItem);
    const groupChild = Boolean(options.groupChild);
    const layoutEntry = descriptor?.entry || null;
    const layoutAttributes = layoutItem && layoutEntry
        ? `
            data-layout-item
            data-layout-key="${descriptor.key}"
            data-layout-span="${span}"
            data-layout-column="${layoutEntry.column}"
        `
        : "";

    return `
        <article
            class="widget ${sizeClass} ${
        layoutItem ? "dashboard-layout-item" : ""
    } ${
        groupChild ? "dashboard-group-child" : ""
    } ${
        state.isEditMode ? "is-layout-editing" : ""
    }"
            data-widget-id="${widget.id}"
            ${layoutAttributes}
        >
            ${renderWidgetHeader(
        widget,
        index,
        widgetCount,
        options
    )}

            ${widget.collapsed ? "" : `
                <div
                    class="widget-content"
                    data-widget-content="${widget.id}"
                >
                    ${renderWidgetContent(widget)}
                </div>
            `}
        </article>
    `;
}

function renderHeaderWidget() {
    const headerWidget = getCurrentTimeWidget();

    if (!headerWidget || !headerWidget.visible) {
        return "";
    }

    return `
        <article
            class="widget header-widget"
            data-header-widget-id="${headerWidget.id}"
        >
            <div class="widget-header">
                <div>
                    <div
                        class="widget-title"
                        data-current-time-basis="${
        typeof getSelectedCurrentTimeCity === "function"
            ? `${getSelectedCurrentTimeCity()} 기준`
            : "서울 기준"
    }"
                    >
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

function renderHeaderWorldTimeWidget() {
    const worldTimeWidget = getWorldTimeWidget();

    if (!worldTimeWidget || !worldTimeWidget.visible) {
        return "";
    }

    return renderWidget(
        worldTimeWidget,
        0,
        1,
        {
            layoutItem: false,
            groupChild: true,
            span: 1,
            suppressActions: true
        }
    );
}

function renderTimeGroupLayoutItem(descriptor) {
    const currentVisible =
        Boolean(descriptor.currentTimeWidget?.visible);
    const worldVisible =
        Boolean(descriptor.worldTimeWidget?.visible);
    const singleClass = currentVisible && worldVisible
        ? ""
        : "is-single-time";

    return `
        <section
            class="header-time-group dashboard-layout-item dashboard-time-item ${singleClass} ${
        state.isEditMode ? "is-layout-editing" : ""
    }"
            data-layout-item
            data-layout-key="${descriptor.key}"
            data-layout-span="2"
            data-layout-column="${descriptor.entry.column}"
        >
            ${state.isEditMode ? `
                <span class="dashboard-group-move-guide">
                    ↔ 더블클릭으로 이동
                </span>
            ` : ""}

            ${currentVisible ? `
                <section
                    class="header-widget-slot"
                    data-time-current-slot
                >
                    ${renderHeaderWidget()}
                </section>
            ` : ""}

            ${worldVisible ? `
                <section
                    class="header-world-time-slot"
                    data-time-world-slot
                >
                    ${renderHeaderWorldTimeWidget()}
                </section>
            ` : ""}
        </section>
    `;
}

function renderScheduleCalendarGroupItem(descriptor) {
    const widgets = [
        descriptor.scheduleWidget,
        descriptor.calendarWidget
    ].filter((widget) => widget?.visible);
    const singleClass = widgets.length === 1
        ? "is-single-group"
        : "";

    return `
        <section
            class="schedule-calendar-group dashboard-layout-item ${singleClass} ${
        state.isEditMode ? "is-layout-editing" : ""
    }"
            data-layout-item
            data-layout-key="${descriptor.key}"
            data-layout-span="2"
            data-layout-column="${descriptor.entry.column}"
        >
            ${state.isEditMode ? `
                <span class="dashboard-group-move-guide">
                    ↔ 묶음 이동
                </span>
            ` : ""}

            ${widgets.map((widget, index) => {
        return renderWidget(
            widget,
            index,
            widgets.length,
            {
                layoutItem: false,
                groupChild: true,
                span: 1,
                sizeClass: "main-widget"
            }
        );
    }).join("")}
        </section>
    `;
}

function renderDashboardLayoutItem(descriptor) {
    if (descriptor.kind === "time-group") {
        return renderTimeGroupLayoutItem(descriptor);
    }

    if (descriptor.kind === "schedule-group") {
        return renderScheduleCalendarGroupItem(descriptor);
    }

    return renderWidget(
        descriptor.widget,
        0,
        1,
        {
            layoutItem: true,
            descriptor,
            span: descriptor.span
        }
    );
}

function renderDashboardBoardContent() {
    const items = getVisibleDashboardLayoutItems();

    return `
        <section
            class="header-auth-slot dashboard-auth-item"
            data-layout-fixed="auth"
            data-layout-key="${DASHBOARD_AUTH_KEY}"
            data-auth-widget
        >
            ${renderAuthWidget()}
        </section>

        ${items.map(renderDashboardLayoutItem).join("")}
    `;
}

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

    if (widget.type === "worldTime") {
        return renderWorldTimeWidget();
    }

    return "";
}
