// 이벤트 연결 상태
let eventsBound = false;

// 더블클릭으로 선택한 위젯 정보
let widgetMoveSession = null;

// 예정 일정과 월간 캘린더는 함께 이동
const SCHEDULE_CALENDAR_GROUP_IDS = [3, 7];

// 메인 화면 이벤트 연결
function bindEvents() {
    if (eventsBound) {
        return;
    }

    const app = document.querySelector("#app");

    if (!app) {
        return;
    }

    app.addEventListener(
        "click",
        handleEditModeClick,
        true
    );

    app.addEventListener(
        "dblclick",
        handleWidgetDoubleClick
    );

    app.addEventListener(
        "click",
        handleAction
    );

    app.addEventListener(
        "input",
        handleInput
    );

    app.addEventListener(
        "dragstart",
        preventNativeWidgetDrag
    );

    document.addEventListener(
        "pointermove",
        handleWidgetMovePointer
    );

    document.addEventListener(
        "keydown",
        handleWidgetMoveKeydown
    );

    syncLayoutEditClass();

    eventsBound = true;
}

// 배치 편집 상태 표시
function syncLayoutEditClass() {
    const app = document.querySelector("#app");

    if (!app) {
        return;
    }

    app.classList.toggle(
        "is-layout-edit-mode",
        state.isEditMode
    );
}

// 이동할 위젯 카드 목록 조회
function getWidgetMoveCards(widgetCard) {
    const widgetList =
        widgetCard.closest("[data-widget-list]");

    if (!widgetList) {
        return [];
    }

    const widgetId =
        Number(widgetCard.dataset.widgetId);

    if (
        !SCHEDULE_CALENDAR_GROUP_IDS.includes(
            widgetId
        )
    ) {
        return [widgetCard];
    }

    const groupCards =
        SCHEDULE_CALENDAR_GROUP_IDS
            .map((id) => {
                return widgetList.querySelector(
                    `:scope > [data-widget-id="${id}"]`
                );
            })
            .filter(Boolean);

    return groupCards.length === 2
        ? groupCards
        : [widgetCard];
}

// 이동할 카드 묶음의 전체 영역 계산
function getWidgetMoveGroupRect(cards) {
    const rects = cards.map((card) => {
        return card.getBoundingClientRect();
    });

    const left = Math.min(
        ...rects.map((rect) => rect.left)
    );

    const top = Math.min(
        ...rects.map((rect) => rect.top)
    );

    const right = Math.max(
        ...rects.map((rect) => rect.right)
    );

    const bottom = Math.max(
        ...rects.map((rect) => rect.bottom)
    );

    return {
        left: left,
        top: top,
        right: right,
        bottom: bottom,
        width: right - left,
        height: bottom - top
    };
}

// 카드 묶음 중 DOM에서 가장 앞에 있는 카드 조회
function getFirstMoveCardInDom(
    widgetList,
    cards
) {
    const cardSet = new Set(cards);

    return [
        ...widgetList.querySelectorAll(
            ":scope > [data-widget-id]"
        )
    ].find((card) => {
        return cardSet.has(card);
    });
}

// 카드 묶음 중 DOM에서 가장 뒤에 있는 카드 조회
function getLastMoveCardInDom(
    widgetList,
    cards
) {
    const cardSet = new Set(cards);

    return [
        ...widgetList.querySelectorAll(
            ":scope > [data-widget-id]"
        )
    ]
        .reverse()
        .find((card) => {
            return cardSet.has(card);
        });
}

// 배치 편집 중 클릭 처리
function handleEditModeClick(event) {
    if (!state.isEditMode) {
        return;
    }

    // 이동 중에는 같은 열을 한 번 클릭하면 배치한다.
    if (widgetMoveSession) {
        const targetList =
            event.target.closest(
                "[data-widget-list]"
            );

        if (
            targetList
            === widgetMoveSession.list
        ) {
            updateMovePlaceholder(
                event.clientX,
                event.clientY
            );

            finishWidgetMove(true);

            event.preventDefault();
            event.stopPropagation();
            return;
        }

        // 배치 완료 같은 화면 바깥 버튼은 정상 작동시킨다.
        const actionButton =
            event.target.closest(
                "[data-action]"
            );

        if (actionButton) {
            return;
        }

        event.preventDefault();
        event.stopPropagation();
        return;
    }

    const widgetCard =
        event.target.closest(
            "[data-widget-id]"
        );

    if (!widgetCard) {
        return;
    }

    // 숨김 버튼은 편집 중에도 사용할 수 있다.
    const hideButton =
        event.target.closest(
            '[data-action="toggle-widget"]'
        );

    if (hideButton) {
        return;
    }

    // 일반 클릭으로 위젯 내부 기능이 실행되는 것을 막는다.
    event.preventDefault();
    event.stopPropagation();
}

// 위젯 더블클릭으로 이동 시작
function handleWidgetDoubleClick(event) {
    if (!state.isEditMode) {
        return;
    }

    const hideButton =
        event.target.closest(
            '[data-action="toggle-widget"]'
        );

    if (hideButton) {
        return;
    }

    const widgetCard =
        event.target.closest(
            "[data-widget-id]"
        );

    if (!widgetCard) {
        return;
    }

    event.preventDefault();
    event.stopPropagation();

    if (widgetMoveSession) {
        // 선택한 일정 또는 캘린더를 다시 더블클릭하면 둘 다 취소
        if (
            widgetMoveSession.cards.includes(
                widgetCard
            )
        ) {
            finishWidgetMove(false);
            return;
        }

        finishWidgetMove(false);
    }

    startWidgetMove(
        widgetCard,
        event.clientX,
        event.clientY
    );
}

// 위젯 이동 시작
function startWidgetMove(
    widgetCard,
    pointerX,
    pointerY
) {
    const widgetList =
        widgetCard.closest(
            "[data-widget-list]"
        );

    if (!widgetList) {
        return;
    }

    const cards =
        getWidgetMoveCards(widgetCard);

    if (!cards.length) {
        return;
    }

    const groupRect =
        getWidgetMoveGroupRect(cards);

    const firstCard =
        getFirstMoveCardInDom(
            widgetList,
            cards
        );

    const lastCard =
        getLastMoveCardInDom(
            widgetList,
            cards
        );

    if (!firstCard || !lastCard) {
        return;
    }

    const originalNextSibling =
        lastCard.nextElementSibling;

    const cardStates =
        cards.map((card, index) => {
            const rect =
                card.getBoundingClientRect();

            return {
                card: card,
                rect: rect,

                relativeLeft:
                    rect.left
                    - groupRect.left,

                relativeTop:
                    rect.top
                    - groupRect.top,

                zIndex:
                    1000 + index
            };
        });

    const placeholder =
        document.createElement("div");

    placeholder.className =
        "widget-move-placeholder";

    placeholder.style.height =
        `${groupRect.height}px`;

    // 일정과 캘린더 묶음은 한 줄 전체의 빈자리로 표시
    if (cards.length > 1) {
        placeholder.style.gridColumn =
            "1 / -1";
    }

    widgetList.insertBefore(
        placeholder,
        firstCard
    );

    widgetMoveSession = {
        cards: cards,
        cardStates: cardStates,
        list: widgetList,
        zone:
        widgetList.dataset.widgetList,
        placeholder: placeholder,

        offsetX:
            pointerX
            - groupRect.left,

        offsetY:
            pointerY
            - groupRect.top,

        originalNextSibling:
        originalNextSibling
    };

    cardStates.forEach((cardState) => {
        const card =
            cardState.card;

        const rect =
            cardState.rect;

        card.classList.add(
            "is-move-selected"
        );

        card.style.position =
            "fixed";

        card.style.left =
            `${rect.left}px`;

        card.style.top =
            `${rect.top}px`;

        card.style.width =
            `${rect.width}px`;

        card.style.height =
            `${rect.height}px`;

        card.style.margin =
            "0";

        card.style.zIndex =
            String(cardState.zIndex);

        card.style.pointerEvents =
            "none";

        card.style.transition =
            "none";

        card.style.transform =
            "none";
    });

    widgetList.classList.add(
        "is-widget-move-list"
    );

    document.body.classList.add(
        "is-widget-move-mode"
    );
}

// 선택한 위젯이 마우스를 따라 이동
function handleWidgetMovePointer(event) {
    if (!widgetMoveSession) {
        return;
    }

    const session =
        widgetMoveSession;

    const groupLeft =
        event.clientX
        - session.offsetX;

    const groupTop =
        event.clientY
        - session.offsetY;

    session.cardStates.forEach(
        (cardState) => {
            cardState.card.style.left =
                `${
                    groupLeft
                    + cardState.relativeLeft
                }px`;

            cardState.card.style.top =
                `${
                    groupTop
                    + cardState.relativeTop
                }px`;
        }
    );

    updateMovePlaceholder(
        event.clientX,
        event.clientY
    );

    autoScrollWidgetMove(
        event.clientY
    );
}

// 마우스 위치에 맞춰 들어갈 자리 표시
function updateMovePlaceholder(
    pointerX,
    pointerY
) {
    const session =
        widgetMoveSession;

    if (!session) {
        return;
    }

    const listRect =
        session.list.getBoundingClientRect();

    // 다른 열로 마우스가 넘어가면 위치를 바꾸지 않는다.
    if (
        pointerX < listRect.left
        || pointerX > listRect.right
    ) {
        return;
    }

    const otherCards = [
        ...session.list.querySelectorAll(
            ":scope > [data-widget-id]"
        )
    ].filter((card) => {
        return !session.cards.includes(
            card
        );
    });

    let referenceCard = null;

    for (const card of otherCards) {
        const rect =
            card.getBoundingClientRect();

        const middleY =
            rect.top
            + rect.height / 2;

        if (pointerY < middleY) {
            referenceCard = card;
            break;
        }
    }

    if (referenceCard) {
        session.list.insertBefore(
            session.placeholder,
            referenceCard
        );

        return;
    }

    session.list.append(
        session.placeholder
    );
}

// 화면 위아래 근처에서 자동 스크롤
function autoScrollWidgetMove(pointerY) {
    const edgeSize = 80;
    const scrollSpeed = 14;

    if (pointerY < edgeSize) {
        window.scrollBy(
            0,
            -scrollSpeed
        );

        return;
    }

    if (
        pointerY
        > window.innerHeight
        - edgeSize
    ) {
        window.scrollBy(
            0,
            scrollSpeed
        );
    }
}

// Esc 키로 이동 취소
function handleWidgetMoveKeydown(event) {
    if (
        event.key !== "Escape"
        || !widgetMoveSession
    ) {
        return;
    }

    event.preventDefault();

    finishWidgetMove(false);
}

// 이동 완료 또는 취소
function finishWidgetMove(saveOrder) {
    const session =
        widgetMoveSession;

    if (!session) {
        return;
    }

    if (saveOrder) {
        // 일정과 캘린더를 같은 위치에 순서대로 배치
        session.cards.forEach((card) => {
            session.list.insertBefore(
                card,
                session.placeholder
            );
        });
    } else if (
        session.originalNextSibling
        && session.originalNextSibling.parentElement
        === session.list
    ) {
        // 취소하면 기존 위치로 둘 다 복원
        session.cards.forEach((card) => {
            session.list.insertBefore(
                card,
                session.originalNextSibling
            );
        });
    } else {
        session.cards.forEach((card) => {
            session.list.append(card);
        });
    }

    session.placeholder.remove();

    session.cards.forEach((card) => {
        resetWidgetMoveStyle(card);
    });

    session.list.classList.remove(
        "is-widget-move-list"
    );

    document.body.classList.remove(
        "is-widget-move-mode"
    );

    if (saveOrder) {
        saveWidgetOrderFromDom(
            session.zone
        );

        syncWidgetList(
            session.zone
        );
    }

    widgetMoveSession = null;
}

// 선택된 위젯 스타일 복구
function resetWidgetMoveStyle(card) {
    card.classList.remove(
        "is-move-selected"
    );

    card.style.removeProperty(
        "position"
    );

    card.style.removeProperty(
        "left"
    );

    card.style.removeProperty(
        "top"
    );

    card.style.removeProperty(
        "width"
    );

    card.style.removeProperty(
        "height"
    );

    card.style.removeProperty(
        "margin"
    );

    card.style.removeProperty(
        "z-index"
    );

    card.style.removeProperty(
        "pointer-events"
    );

    card.style.removeProperty(
        "transition"
    );

    card.style.removeProperty(
        "transform"
    );
}

// 브라우저 기본 드래그 차단
function preventNativeWidgetDrag(event) {
    if (
        event.target.closest(
            "[data-widget-id]"
        )
    ) {
        event.preventDefault();
    }
}

// 입력 이벤트 처리
function handleInput(event) {
    if (
        event.target.matches(
            "#memoInput"
        )
    ) {
        state.memoText =
            event.target.value;

        return;
    }

    if (
        event.target.matches(
            "#loginUsername"
        )
    ) {
        state.loginForm.username =
            event.target.value;

        return;
    }

    if (
        event.target.matches(
            "#loginPassword"
        )
    ) {
        state.loginForm.password =
            event.target.value;
    }
}

// 버튼 액션 처리
function handleAction(event) {
    const button =
        event.target.closest(
            "[data-action]"
        );

    if (!button) {
        return;
    }

    const action =
        button.dataset.action;

    const id =
        Number(button.dataset.id);

    const value =
        button.dataset.value;

    if (
        action
        === "select-news-category"
    ) {
        const category =
            button.dataset.value;

        if (
            !state.newsCategories.includes(
                category
            )
        ) {
            return;
        }

        if (
            state.newsCategory
            === category
        ) {
            return;
        }

        fetchNews(category);

        return;
    }

    if (action === "toggle-edit") {
        finishWidgetMove(false);

        state.isSettingsOpen =
            true;

        render();
        syncLayoutEditClass();

        return;
    }

    if (action === "close-edit") {
        finishWidgetMove(false);

        state.isSettingsOpen =
            false;

        render();
        syncLayoutEditClass();

        return;
    }

    if (
        action
        === "start-layout-edit"
    ) {
        finishWidgetMove(false);

        state.isSettingsOpen =
            false;

        state.isEditMode =
            true;

        render();
        syncLayoutEditClass();

        return;
    }

    if (
        action
        === "finish-layout-edit"
    ) {
        finishWidgetMove(true);

        state.isEditMode =
            false;

        render();
        syncLayoutEditClass();

        return;
    }

    if (
        action
        === "toggle-widget"
    ) {
        finishWidgetMove(false);

        toggleWidget(id);

        return;
    }

    if (
        action
        === "toggle-collapse"
    ) {
        finishWidgetMove(false);

        toggleCollapse(id);

        return;
    }

    if (action === "select-stock") {
        const stockIndex = Number(value);

        if (
            !Number.isInteger(stockIndex) ||
            stockIndex < 0 ||
            stockIndex >= state.stockItems.length
        ) {
            return;
        }

        state.stockSlideIndex = stockIndex;
        updateStockQuoteSelection(stockIndex);

        if (state.stockSwiper) {
            state.stockSwiper.slideToLoop(
                stockIndex,
                450
            );
        }

        return;
    }

    if (action === "refresh-stocks") {
        if (state.stockLoading) {
            return;
        }

        fetchStocks();
        return;
    }


    if (action === "move-up") {
        moveWidget(
            id,
            "up"
        );

        return;
    }

    if (
        action
        === "move-down"
    ) {
        moveWidget(
            id,
            "down"
        );

        return;
    }

    if (
        action
        === "append-calc"
    ) {
        appendCalculatorValue(value);

        return;
    }

    if (
        action
        === "clear-calc"
    ) {
        state.calculatorText =
            "";

        updateCalculatorDisplay();

        return;
    }

    if (
        action
        === "percent-calc"
    ) {
        applyCalculatorPercent();

        return;
    }

    if (
        action
        === "backspace-calc"
    ) {
        removeCalculatorCharacter();

        return;
    }

    if (
        action
        === "parentheses-calc"
    ) {
        appendCalculatorParenthesis();

        return;
    }

    if (
        action
        === "decimal-calc"
    ) {
        appendCalculatorDecimal();

        return;
    }

    if (
        action
        === "calculate"
    ) {
        calculate();

        return;
    }

    if (
        action
        === "login"
    ) {
        login();

        return;
    }

    if (
        action
        === "logout"
    ) {
        state.currentUser =
            null;

        render();

        return;
    }

    if (
        action
        === "prev-calendar-month"
    ) {
        state.calendarMonth--;

        if (
            state.calendarMonth < 0
        ) {
            state.calendarMonth =
                11;

            state.calendarYear--;
        }

        state.miniCalendarDayMap =
            new Map();

        state.miniCalendarError =
            "";

        refreshMiniCalendarWidget();

        fetchMiniCalendarMonthData(
            true
        );

        return;
    }

    if (
        action
        === "next-calendar-month"
    ) {
        state.calendarMonth++;

        if (
            state.calendarMonth > 11
        ) {
            state.calendarMonth =
                0;

            state.calendarYear++;
        }

        state.miniCalendarDayMap =
            new Map();

        state.miniCalendarError =
            "";

        refreshMiniCalendarWidget();

        fetchMiniCalendarMonthData(
            true
        );

        return;
    }

    if (
        action
        === "go-calendar-day"
    ) {
        const date =
            button.dataset.date;

        if (!date) {
            return;
        }

        window.location.href =
            "/calendar?date="
            + encodeURIComponent(
                date
            );
    }
}