// 메인 위젯 조회
function getMainWidgets() {
    return state.widgets
        .filter((widget) => {
            return widget.visible
                && widget.zone === "main"
                && widget.id !== state.headerWidgetId;
        })
        .sort((a, b) => {
            return a.orderNo - b.orderNo;
        });
}

// 사이드 위젯 조회
function getSideWidgets() {
    return state.widgets
        .filter((widget) => {
            return widget.visible
                && widget.zone === "side"
                && widget.id !== state.headerWidgetId;
        })
        .sort((a, b) => {
            return a.orderNo - b.orderNo;
        });
}

// 더 짧은 위젯 컬럼이 스크롤을 따라가도록 설정
function updateFollowColumn() {
    const mainColumn =
        document.querySelector(".main-column");

    const sideColumn =
        document.querySelector(".side-column");

    const mainList =
        document.querySelector(
            '[data-widget-list="main"]'
        );

    const sideList =
        document.querySelector(
            '[data-widget-list="side"]'
        );

    if (
        !mainColumn
        || !sideColumn
        || !mainList
        || !sideList
    ) {
        return;
    }

    mainColumn.classList.remove(
        "is-follow-column"
    );

    sideColumn.classList.remove(
        "is-follow-column"
    );

    const mainHeight =
        mainList.getBoundingClientRect().height;

    const sideHeight =
        sideList.getBoundingClientRect().height;

    if (mainHeight === sideHeight) {
        return;
    }

    if (mainHeight < sideHeight) {
        mainColumn.classList.add(
            "is-follow-column"
        );

        return;
    }

    sideColumn.classList.add(
        "is-follow-column"
    );
}

// 위젯 위치 저장
function captureWidgetPositions() {
    const positions = new Map();

    document
        .querySelectorAll("[data-widget-id]")
        .forEach((element) => {
            positions.set(
                element.dataset.widgetId,
                element.getBoundingClientRect()
            );
        });

    return positions;
}

// 위젯 이동 애니메이션
function animateWidgetChanges(prevPositions) {
    document
        .querySelectorAll("[data-widget-id]")
        .forEach((element) => {
            const prevRect =
                prevPositions.get(
                    element.dataset.widgetId
                );

            if (!prevRect) {
                return;
            }

            const nextRect =
                element.getBoundingClientRect();

            const deltaX =
                prevRect.left - nextRect.left;

            const deltaY =
                prevRect.top - nextRect.top;

            if (
                deltaX === 0
                && deltaY === 0
            ) {
                return;
            }

            element.animate(
                [
                    {
                        transform:
                            `translate(${deltaX}px, ${deltaY}px)`
                    },
                    {
                        transform:
                            "translate(0, 0)"
                    }
                ],
                {
                    duration: 250,
                    easing: "ease"
                }
            );
        });
}

// 위젯 표시 숨김
function toggleWidget(id) {
    const widget = state.widgets.find(
        (item) => {
            return item.id === id;
        }
    );

    if (!widget) {
        return;
    }

    widget.visible = !widget.visible;

    render();
}

// 위젯 접기 펼치기
function toggleCollapse(id) {
    const widget = state.widgets.find(
        (item) => {
            return item.id === id;
        }
    );

    if (!widget) {
        return;
    }

    widget.collapsed = !widget.collapsed;

    render();
}

// 새 위젯 카드 DOM 생성
function createWidgetElement(
    widget,
    index,
    widgetCount
) {
    const template =
        document.createElement("template");

    template.innerHTML =
        renderWidget(
            widget,
            index,
            widgetCount
        ).trim();

    return template.content.firstElementChild;
}

// 이미 화면에 있는 위젯의 공통 영역 갱신
function updateWidgetFrame(
    card,
    widget,
    index,
    widgetCount
) {
    card.className = `widget ${
        widget.zone === "main"
            ? "main-widget"
            : "side-widget"
    }`;

    const header =
        card.querySelector(".widget-header");

    if (header) {
        header.outerHTML =
            renderWidgetHeader(
                widget,
                index,
                widgetCount
            );
    }

    const content =
        card.querySelector(".widget-content");

    if (widget.collapsed) {
        content?.remove();
        return;
    }

    if (content) {
        if (
            widget.type === "news"
            || widget.type === "weather"
        ) {
            content.innerHTML =
                renderWidgetContent(widget);
        }

        return;
    }

    const widgetHeader =
        card.querySelector(".widget-header");

    if (!widgetHeader) {
        return;
    }

    widgetHeader.insertAdjacentHTML(
        "afterend",
        `
            <div
                class="widget-content"
                data-widget-content="${widget.id}"
            >
                ${renderWidgetContent(widget)}
            </div>
        `
    );
}

// 특정 영역의 위젯 목록 동기화
function syncWidgetList(zone) {
    const widgetList =
        document.querySelector(
            `[data-widget-list="${zone}"]`
        );

    if (!widgetList) {
        return;
    }

    const widgets =
        zone === "main"
            ? getMainWidgets()
            : getSideWidgets();

    let stockWidgetChanged = false;

    const existingCards = new Map(
        [
            ...widgetList.querySelectorAll(
                ":scope > [data-widget-id]"
            )
        ].map((card) => {
            return [
                card.dataset.widgetId,
                card
            ];
        })
    );

    widgets.forEach((widget, index) => {
        const widgetId =
            String(widget.id);

        let card =
            existingCards.get(widgetId);

        if (card) {
            updateWidgetFrame(
                card,
                widget,
                index,
                widgets.length
            );

            existingCards.delete(widgetId);
        } else {
            card = createWidgetElement(
                widget,
                index,
                widgets.length
            );

            if (widget.type === "stock") {
                stockWidgetChanged = true;
            }
        }

        widgetList.append(card);
    });

    existingCards.forEach((card) => {
        if (
            card.querySelector(
                ".stock-swiper"
            )
        ) {
            stockWidgetChanged = true;
        }

        card.remove();
    });

    if (stockWidgetChanged) {
        state.stockCharts.forEach(
            (chart) => {
                chart.destroy();
            }
        );

        state.stockCharts = [];

        initStockSwiper();
    }

    requestAnimationFrame(
        updateFollowColumn
    );
}

// 특정 위젯 내용만 다시 그리기
function refreshWidgetContent(widgetId) {
    const widget = state.widgets.find(
        (item) => {
            return item.id === widgetId;
        }
    );

    if (!widget) {
        return;
    }

    const content =
        document.querySelector(
            `[data-widget-content="${widgetId}"]`
        );

    if (!content) {
        return;
    }

    if (widget.type === "stock") {
        rememberStockSwiperIndex();

        state.stockCharts.forEach(
            (chart) => {
                chart.destroy();
            }
        );

        state.stockCharts = [];

        content.innerHTML =
            renderWidgetContent(widget);

        initStockSwiper();

        requestAnimationFrame(
            updateFollowColumn
        );

        return;
    }

    content.innerHTML =
        renderWidgetContent(widget);

    requestAnimationFrame(
        updateFollowColumn
    );
}

// 현재 순서를 불러온 회원 아이디
let loadedWidgetOrderMemberId = "";

// 현재 로그인 회원 아이디 조회
function getWidgetOrderMemberId() {
    return String(
        state.currentUser?.username
        || state.currentUser?.memberId
        || ""
    ).trim();
}

// 회원별 위젯 순서 저장 키
function getWidgetOrderStorageKey(memberId) {
    return `dashboardWidgetOrder:${memberId}`;
}

// 현재 위젯 순서를 회원별로 저장
function saveWidgetOrderToStorage() {
    const memberId =
        getWidgetOrderMemberId();

    if (!memberId) {
        return;
    }

    const savedOrder = {
        main: state.widgets
            .filter((widget) => {
                return widget.zone === "main";
            })
            .sort((a, b) => {
                return a.orderNo - b.orderNo;
            })
            .map((widget) => {
                return widget.id;
            }),

        side: state.widgets
            .filter((widget) => {
                return widget.zone === "side";
            })
            .sort((a, b) => {
                return a.orderNo - b.orderNo;
            })
            .map((widget) => {
                return widget.id;
            })
    };

    localStorage.setItem(
        getWidgetOrderStorageKey(memberId),
        JSON.stringify(savedOrder)
    );
}

// 저장된 영역 순서를 state에 적용
function applySavedWidgetOrder(
    zone,
    savedIds
) {
    if (!Array.isArray(savedIds)) {
        return;
    }

    const orderMap = new Map(
        savedIds.map((id, index) => {
            return [
                Number(id),
                index + 1
            ];
        })
    );

    const zoneWidgets =
        state.widgets
            .filter((widget) => {
                return widget.zone === zone;
            })
            .sort((a, b) => {
                return a.orderNo - b.orderNo;
            });

    let nextOrderNo =
        savedIds.length + 1;

    zoneWidgets.forEach((widget) => {
        const savedOrderNo =
            orderMap.get(widget.id);

        if (savedOrderNo) {
            widget.orderNo =
                savedOrderNo;

            return;
        }

        widget.orderNo =
            nextOrderNo;

        nextOrderNo++;
    });
}

// 현재 로그인 회원의 저장 순서 불러오기
function loadWidgetOrderForCurrentUser() {
    const memberId =
        getWidgetOrderMemberId();

    if (!memberId) {
        return false;
    }

    if (
        loadedWidgetOrderMemberId
        === memberId
    ) {
        return false;
    }

    const storageKey =
        getWidgetOrderStorageKey(
            memberId
        );

    const savedText =
        localStorage.getItem(
            storageKey
        );

    loadedWidgetOrderMemberId =
        memberId;

    if (!savedText) {
        return false;
    }

    try {
        const savedOrder =
            JSON.parse(savedText);

        applySavedWidgetOrder(
            "main",
            savedOrder.main
        );

        applySavedWidgetOrder(
            "side",
            savedOrder.side
        );

        return true;
    } catch (error) {
        console.error(
            "위젯 순서 불러오기 실패:",
            error
        );

        localStorage.removeItem(
            storageKey
        );

        return false;
    }
}

// 현재 화면 카드 순서를 state와 브라우저에 저장
function saveWidgetOrderFromDom(zone) {
    const widgetList =
        document.querySelector(
            `[data-widget-list="${zone}"]`
        );

    if (!widgetList) {
        return;
    }

    const visibleWidgets = [
        ...widgetList.querySelectorAll(
            ":scope > [data-widget-id]"
        )
    ]
        .map((card) => {
            const widgetId =
                Number(
                    card.dataset.widgetId
                );

            return state.widgets.find(
                (widget) => {
                    return widget.id
                        === widgetId;
                }
            );
        })
        .filter(Boolean);

    const hiddenWidgets =
        state.widgets
            .filter((widget) => {
                return widget.zone === zone
                    && !widget.visible;
            })
            .sort((a, b) => {
                return a.orderNo
                    - b.orderNo;
            });

    [
        ...visibleWidgets,
        ...hiddenWidgets
    ].forEach((widget, index) => {
        widget.orderNo =
            index + 1;
    });

    saveWidgetOrderToStorage();

    requestAnimationFrame(
        updateFollowColumn
    );
}