//메인 위젯 조회
function getMainWidgets() {
    return state.widgets
        .filter((widget) => widget.visible && widget.zone === "main")
        .sort((a, b) => a.orderNo - b.orderNo);
}
//사이드 위젯 조회
function getSideWidgets() {
    return state.widgets
        .filter((widget) => widget.visible && widget.zone === "side")
        .sort((a, b) => a.orderNo - b.orderNo);
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
//같은 영역의 위젯 조회
function getVisibleWidgetsByZone(zone) {
    return state.widgets
        .filter((widget) => widget.visible && widget.zone === zone)
        .sort((a, b) => a.orderNo - b.orderNo);
}
//위젯 순서 변경
function moveWidget(id, direction) {
    const widget = state.widgets.find((item) => item.id === id);

    if (!widget) {
        return;
    }

    const sameZoneWidgets = getVisibleWidgetsByZone(widget.zone);
    const index = sameZoneWidgets.findIndex((item) => item.id === id);
    const nextIndex = direction === "up" ? index - 1 : index + 1;

    if (nextIndex < 0 || nextIndex >= sameZoneWidgets.length) {
        return;
    }

    const otherWidget = sameZoneWidgets[nextIndex];
    const temp = widget.orderNo;

    widget.orderNo = otherWidget.orderNo;
    otherWidget.orderNo = temp;

    render();
}