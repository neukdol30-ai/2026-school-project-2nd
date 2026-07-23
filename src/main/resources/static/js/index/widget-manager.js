//메인 위젯 조회
function getMainWidgets() {
    return state.widgets
        .filter((widget) =>
            widget.visible &&
            widget.zone === "main" &&
            widget.id !== state.headerWidgetId
        )
        .sort((a, b) => a.orderNo - b.orderNo);
}
//사이드 위젯 조회
function getSideWidgets() {
    return state.widgets
        .filter((widget) =>
            widget.visible &&
            widget.zone === "side" &&
            widget.id !== state.headerWidgetId
        )
        .sort((a, b) => a.orderNo - b.orderNo);
}

// 메인/보조 위젯 높이를 비교해서 짧은 컬럼만 스크롤을 따라가게 설정
function updateFollowColumn() {
    const mainColumn = document.querySelector(".main-column");
    const sideColumn = document.querySelector(".side-column");

    const mainList = document.querySelector(
        '[data-widget-list="main"]'
    );

    const sideList = document.querySelector(
        '[data-widget-list="side"]'
    );

    if (!mainColumn || !sideColumn || !mainList || !sideList) {
        return;
    }

    // 이전에 붙은 이동 클래스를 먼저 제거
    mainColumn.classList.remove("is-follow-column");
    sideColumn.classList.remove("is-follow-column");

    const mainHeight = mainList.getBoundingClientRect().height;
    const sideHeight = sideList.getBoundingClientRect().height;

    // 높이가 같으면 어느 쪽도 따라가지 않음
    if (mainHeight === sideHeight) {
        return;
    }

    // 더 짧은 컬럼에만 sticky 클래스 부여
    if (mainHeight < sideHeight) {
        mainColumn.classList.add("is-follow-column");
        return;
    }

    sideColumn.classList.add("is-follow-column");
}

//위젯 위치 저장
function captureWidgetPositions() {
    const positions = new Map();

    document.querySelectorAll("[data-widget-id]").forEach((element) => {
        positions.set(element.dataset.widgetId, element.getBoundingClientRect());
    });

    return positions;
}
//위젯 이동 애니매이션
function animateWidgetChanges(prevPositions) {
    document.querySelectorAll("[data-widget-id]").forEach((element) => {
        const prevRect = prevPositions.get(element.dataset.widgetId);

        if (!prevRect) {
            return;
        }

        const nextRect = element.getBoundingClientRect();
        const deltaX = prevRect.left - nextRect.left;
        const deltaY = prevRect.top - nextRect.top;

        if (deltaX === 0 && deltaY === 0) {
            return;
        }

        element.animate(
            [
                { transform: `translate(${deltaX}px, ${deltaY}px)` },
                { transform: "translate(0, 0)" }
            ],
            {
                duration: 250,
                easing: "ease"
            }
        );
    });
}

//위젯 표시 숨김
function toggleWidget(id) {
    const widget = state.widgets.find((item) => item.id === id);

    if (!widget) {
        return;
    }

    widget.visible = !widget.visible;
    render();
}
//위젯 접기,펼치기
function toggleCollapse(id) {
    const widget = state.widgets.find((item) => item.id === id);

    if (!widget) {
        return;
    }

    widget.collapsed = !widget.collapsed;
    render();
}

// 새 위젯 카드 DOM 생성
function createWidgetElement(widget, index, widgetCount) {
    const template = document.createElement("template");

    template.innerHTML = renderWidget(widget, index, widgetCount).trim();

    return template.content.firstElementChild;
}

// 이미 화면에 있는 위젯의 공통 영역만 갱신
function updateWidgetFrame(card, widget, index, widgetCount) {
    card.className = `widget ${
        widget.zone === "main" ? "main-widget" : "side-widget"
    }`;

    const header = card.querySelector(".widget-header");

    if (header) {
        header.outerHTML = renderWidgetHeader(widget, index, widgetCount);
    }

    const content = card.querySelector(".widget-content");

    // 접힌 상태라면 내용 영역만 제거
    if (widget.collapsed) {
        content?.remove();
        return;
    }

    // 기존 내용이 있으면 유지한다.
    // 그래서 Swiper, Chart.js, 계산기 입력값 등이 초기화되지 않는다.
    if (content) {
        return;
    }

    // 접혔다가 다시 펼친 위젯만 내용 영역을 새로 생성
    const widgetHeader = card.querySelector(".widget-header");

    widgetHeader.insertAdjacentHTML(
        "afterend",
        `
            <div class="widget-content" data-widget-content="${widget.id}">
                ${renderWidgetContent(widget)}
            </div>
        `
    );
}

// 특정 영역의 위젯 목록만 동기화
function syncWidgetList(zone) {
    const widgetList = document.querySelector(
        `[data-widget-list="${zone}"]`
    );

    if (!widgetList) {
        return;
    }

    const widgets = zone === "main"
        ? getMainWidgets()
        : getSideWidgets();

    let stockWidgetChanged = false;

    // 현재 화면에 이미 있는 위젯 카드들을 id 기준으로 저장
    const existingCards = new Map(
        [...widgetList.querySelectorAll(":scope > [data-widget-id]")]
            .map((card) => [card.dataset.widgetId, card])
    );

    widgets.forEach((widget, index) => {
        const widgetId = String(widget.id);
        let card = existingCards.get(widgetId);

        if (card) {
            // 기존 카드와 내부 기능은 유지
            updateWidgetFrame(card, widget, index, widgets.length);
            existingCards.delete(widgetId);
        } else {
            // 새로 표시된 위젯만 생성
            card = createWidgetElement(widget, index, widgets.length);

            if (widget.type === "stock") {
                stockWidgetChanged = true;
            }
        }

        // append는 기존 DOM을 삭제하지 않고, 필요한 위치로 이동시킨다.
        widgetList.append(card);

        if (widget.type === "currentTime") {
            updateCurrentTimeWidget();
            updateSunTimeWidget();
        }
    });

    // 현재 visible 목록에 없는 카드만 화면에서 제거
    existingCards.forEach((card) => {
        if (card.querySelector(".stock-swiper")) {
            stockWidgetChanged = true;
        }

        card.remove();
    });

    if (stockWidgetChanged) {
        state.stockCharts.forEach((chart) => chart.destroy());
        state.stockCharts = [];

        initStockSwiper();
    }
}

// 특정 위젯의 내용 영역만 다시 그리기
function refreshWidgetContent(widgetId) {
    const widget = state.widgets.find((item) => item.id === widgetId);

    if (!widget) {
        return;
    }

    const content = document.querySelector(
        `[data-widget-content="${widgetId}"]`
    );

    // 숨김 또는 접힘 상태라면 갱신할 내용 영역이 없음
    if (!content) {
        return;
    }

    // 증권 위젯만 기존 Swiper와 Chart를 정리한 후 다시 생성
    if (widget.type === "stock") {
        rememberStockSwiperIndex();

        state.stockCharts.forEach((chart) => chart.destroy());
        state.stockCharts = [];

        content.innerHTML = renderWidgetContent(widget);
        initStockSwiper();

        requestAnimationFrame(updateFollowColumn);

        return;
    }

    // 뉴스, 날씨, 계산기 등은 해당 위젯 내용만 교체
    content.innerHTML = renderWidgetContent(widget);

    requestAnimationFrame(updateFollowColumn);
}

// 현재 화면의 카드 순서를 state.orderNo에 저장
function saveWidgetOrderFromDom(zone) {
    const widgetList = document.querySelector(
        `[data-widget-list="${zone}"]`
    );

    if (!widgetList) {
        return;
    }

    const visibleWidgets = [...widgetList.querySelectorAll(
        ":scope > [data-widget-id]"
    )].map((card) => {
        return state.widgets.find(
            (widget) => widget.id === Number(card.dataset.widgetId)
        );
    });

    const hiddenWidgets = state.widgets
        .filter((widget) => widget.zone === zone && !widget.visible)
        .sort((a, b) => a.orderNo - b.orderNo);

    [...visibleWidgets, ...hiddenWidgets].forEach((widget, index) => {
        widget.orderNo = index + 1;
    });
}