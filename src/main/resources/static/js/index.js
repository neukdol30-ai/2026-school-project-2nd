//기본 뼈대 생성
const state = {
    isEditMode: false,
    memoText: "",
    calculatorText: "",
    currentUser: null,
    loginForm: {
        username: "",
        password: ""
    },
    newsItems: [],
    newsLoading: false,
    newsError: "",
    dayNames: ["월", "화", "수", "목", "금", "토", "일"],

    widgets: [
        {
            id: 1,
            zone: "main",
            type: "news",
            title: "뉴스",
            icon: "📰",
            description: "주요 뉴스를 표시합니다.",
            visible: true,
            collapsed: false,
            orderNo: 1
        },
        {
            id: 2,
            zone: "main",
            type: "issue",
            title: "주요 이슈",
            icon: "🔥",
            description: "오늘의 이슈 키워드를 표시합니다.",
            visible: true,
            collapsed: false,
            orderNo: 2
        },
        {
            id: 3,
            zone: "main",
            type: "schedule",
            title: "오늘 일정",
            icon: "📌",
            description: "오늘의 주요 일정을 표시합니다.",
            visible: true,
            collapsed: false,
            orderNo: 3
        },
        {
            id: 4,
            zone: "main",
            type: "stock",
            title: "증권",
            icon: "📈",
            description: "관심 종목과 차트를 표시합니다.",
            visible: true,
            collapsed: false,
            orderNo: 4
        },
        {
            id: 5,
            zone: "side",
            type: "weather",
            title: "날씨",
            icon: "🌤️",
            description: "현재 날씨를 간단히 표시합니다.",
            visible: true,
            collapsed: false,
            orderNo: 1
        },
        {
            id: 6,
            zone: "side",
            type: "calculator",
            title: "계산기",
            icon: "🧮",
            description: "간단한 계산을 수행합니다.",
            visible: true,
            collapsed: false,
            orderNo: 2
        },
        {
            id: 7,
            zone: "side",
            type: "miniCalendar",
            title: "미니 캘린더",
            icon: "📅",
            description: "이번 달 날짜를 표시합니다.",
            visible: true,
            collapsed: false,
            orderNo: 3
        },
        {
            id: 8,
            zone: "side",
            type: "memo",
            title: "메모",
            icon: "📝",
            description: "간단한 메모를 작성합니다.",
            visible: true,
            collapsed: false,
            orderNo: 4
        }
    ]
};

//필터 함수
function getMainWidgets() {
    return state.widgets
        .filter((widget) => widget.visible && widget.zone === "main")
        .sort((a, b) => a.orderNo - b.orderNo);
}

function getSideWidgets() {
    return state.widgets
        .filter((widget) => widget.visible && widget.zone === "side")
        .sort((a, b) => a.orderNo - b.orderNo);
}

//rander 함수 생성
function render() {
    const app = document.querySelector("#app");
    const prevPositions = captureWidgetPositions();

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
}

//애니메이션 함수 추가
function captureWidgetPositions() {
    const positions = new Map();

    document.querySelectorAll("[data-widget-id]").forEach((element) => {
        positions.set(element.dataset.widgetId, element.getBoundingClientRect());
    });

    return positions;
}

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
                    <button data-action="toggle-collapse" data-id="${widget.id}">
                        ${widget.collapsed ? "펼치기" : "접기"}
                    </button>

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

//위젯 내용
function renderWidgetContent(widget) {
    if (widget.type === "news") {
        return renderNewsWidget();
    }

    if (widget.type === "issue") {
        return `
            <div class="issue-tags">
                <span class="issue-tag">#AI</span>
                <span class="issue-tag">#경제</span>
                <span class="issue-tag">#증권</span>
                <span class="issue-tag">#개발</span>
                <span class="issue-tag">#날씨</span>
            </div>
        `;
    }

    if (widget.type === "schedule") {
        return `
            <ul class="schedule-list">
                <li>09:00 프로젝트 정리</li>
                <li>13:00 API 연동 테스트</li>
                <li>18:00 개인 학습 기록</li>
            </ul>
        `;
    }

    if (widget.type === "stock") {
        return `
            <div class="stock-box">
                증권 차트 영역
            </div>
        `;
    }

    if (widget.type === "weather") {
        return `
            <div class="weather-temp">24℃</div>
            <p>서울 · 구름 조금</p>
        `;
    }

    if (widget.type === "calculator") {
        return renderCalculator();
    }

    if (widget.type === "miniCalendar") {
        return renderMiniCalendar();
    }

    if (widget.type === "memo") {
        return `
            <textarea
                id="memoInput"
                placeholder="메모를 입력하세요"
            >${state.memoText}</textarea>

            <div class="memo-count">
                글자 수: ${state.memoText.length}
            </div>
        `;
    }

    return "";
}

//뉴스 위젯
function renderNewsWidget() {
    if (state.newsLoading) {
        return `<p class="widget-desc">뉴스를 불러오는 중입니다.</p>`;
    }

    if (state.newsError) {
        return `<p class="widget-desc">${state.newsError}</p>`;
    }

    return `
        <ul class="news-list">
            ${state.newsItems.map((news) => `
                <li class="news-item">
                    <a
                        class="news-title"
                        href="${news.link}"
                        target="_blank"
                        rel="noopener noreferrer"
                    >
                        ${news.title}
                    </a>

                    <p class="news-summary">
                        ${news.summary}
                    </p>

                    <div class="news-meta">
                        ${news.source} · ${news.publishedAt}
                    </div>
                </li>
            `).join("")}
        </ul>
    `;
}

//계산기 캘린더
function renderCalculator() {
    const buttons = ["7", "8", "9", "/", "4", "5", "6", "*", "1", "2", "3", "-", "0"];

    return `
        <input
            class="calculator-display"
            type="text"
            value="${state.calculatorText}"
            readonly
        >

        <div class="calculator-buttons">
            ${buttons.map((value) => `
                <button data-action="append-calc" data-value="${value}">
                    ${value === "/" ? "÷" : value === "*" ? "×" : value}
                </button>
            `).join("")}

            <button data-action="clear-calc">C</button>
            <button data-action="calculate">=</button>
            <button data-action="append-calc" data-value="+">+</button>
        </div>
    `;
}

function renderMiniCalendar() {
    return `
        <div class="calendar-grid">
            ${state.dayNames.map((dayName) => `
                <div class="calendar-cell">
                    <strong>${dayName}</strong>
                </div>
            `).join("")}

            ${Array.from({ length: 31 }, (_, index) => index + 1).map((day) => `
                <div class="calendar-cell">${day}</div>
            `).join("")}
        </div>
    `;
}

//로그인 위젯
function renderAuthWidget() {
    if (!state.currentUser) {
        return `
            <article class="widget side-widget auth-widget">
                <div class="widget-header">
                    <div>
                        <div class="widget-title">🔐 계정</div>
                        <div class="widget-desc">
                            로그인하면 사용자 설정이 적용됩니다.
                        </div>
                    </div>
                </div>

                <div class="auth-content">
                
                <!--
                    <input
                        id="loginUsername"
                        class="auth-input"
                        type="text"
                        value="${state.loginForm.username}"
                        placeholder="아이디"
                    >

                    <input
                        id="loginPassword"
                        class="auth-input"
                        type="password"
                        value="${state.loginForm.password}"
                        placeholder="비밀번호"
                    >
                     -->
                     
                    <!-- 로그인 버튼: 누르면 로그인 페이지로 이동 -->
                    <button class="auth-login-button" data-action="go-login">
                        로그인
                    </button>
                    
                    <!-- 회원가입 버튼: 누르면 회원가입 페이지로 이동 (원래 data-action 없어서 죽어있던 버튼) -->
                    <button class="auth-register-button" data-action="go-signup">
                        회원가입
                    </button>
                </div>
            </article>
        `;
    }

    return `
        <article class="widget side-widget auth-widget">
            <div class="widget-header">
                <div>
                    <div class="widget-title">🔐 계정</div>
                    <div class="widget-desc">
                        로그인하면 사용자 설정이 적용됩니다.
                    </div>
                </div>
            </div>

            <div class="auth-content">
                <p class="auth-user">
                    ${state.currentUser.nickname}님 로그인 중
                </p>

                <button class="auth-login-button" data-action="logout">
                    로그아웃
                </button>
            </div>
        </article>
    `;
}

//이벤트 연결
function bindEvents() {
    document.querySelectorAll("[data-action]").forEach((element) => {
        element.addEventListener("click", handleAction);
    });

    const memoInput = document.querySelector("#memoInput");
    if (memoInput) {
        memoInput.addEventListener("input", (event) => {
            state.memoText = event.target.value;
            render();
        });
    }

    const loginUsername = document.querySelector("#loginUsername");
    if (loginUsername) {
        loginUsername.addEventListener("input", (event) => {
            state.loginForm.username = event.target.value;
        });
    }

    const loginPassword = document.querySelector("#loginPassword");
    if (loginPassword) {
        loginPassword.addEventListener("input", (event) => {
            state.loginForm.password = event.target.value;
        });
    }
}

//버튼 액션 처리
function handleAction(event) {
    const action = event.currentTarget.dataset.action;
    const id = Number(event.currentTarget.dataset.id);
    const value = event.currentTarget.dataset.value;

    if (action === "toggle-edit") {
        state.isEditMode = !state.isEditMode;
        render();
        return;
    }

    if (action === "toggle-widget") {
        toggleWidget(id);
        return;
    }

    if (action === "toggle-collapse") {
        toggleCollapse(id);
        return;
    }

    if (action === "move-up") {
        moveWidget(id, "up");
        return;
    }

    if (action === "move-down") {
        moveWidget(id, "down");
        return;
    }

    if (action === "append-calc") {
        state.calculatorText += value;
        render();
        return;
    }

    if (action === "clear-calc") {
        state.calculatorText = "";
        render();
        return;
    }

    if (action === "calculate") {
        calculate();
        return;
    }

    // 로그인 버튼 클릭 -> 로그인 페이지로 브라우저 이동
    if (action === "go-login") {
        window.location.href = "/member/login";
        return;
    }

    // 회원가입 버튼 클릭 -> 회원가입 페이지로 이동
    if (action === "go-signup") {
        window.location.href = "/member/signup";
        return;
    }

    // 위의 2개 코드 추가로 안 쓰게 되는 코드 -> login()함수: 이제 부르는 곳 없음. / bindEvents()안의 #loginUsername, #loginPassword 연결 부분 / state.loginForm 칸

    if (action === "login") {
        login();
        return;
    }

    if (action === "logout") {
        state.currentUser = null;
        render();
    }
}

//상태 변경 함수
function toggleWidget(id) {
    const widget = state.widgets.find((item) => item.id === id);

    if (!widget) {
        return;
    }

    widget.visible = !widget.visible;
    render();
}

function toggleCollapse(id) {
    const widget = state.widgets.find((item) => item.id === id);

    if (!widget) {
        return;
    }

    widget.collapsed = !widget.collapsed;
    render();
}

function getVisibleWidgetsByZone(zone) {
    return state.widgets
        .filter((widget) => widget.visible && widget.zone === zone)
        .sort((a, b) => a.orderNo - b.orderNo);
}

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

//로그인 계산 함수
function login() {
    if (!state.loginForm.username) {
        alert("아이디를 입력하세요.");
        return;
    }

    state.currentUser = {
        username: state.loginForm.username,
        nickname: state.loginForm.username
    };

    state.loginForm.password = "";
    render();
}

function calculate() {
    try {
        if (!/^[0-9+\-*/.() ]+$/.test(state.calculatorText)) {
            state.calculatorText = "Error";
            render();
            return;
        }

        const result = Function(`"use strict"; return (${state.calculatorText})`)();
        state.calculatorText = String(result);
    } catch (error) {
        state.calculatorText = "Error";
    }

    render();
}

// 뉴스 API 연결
async function fetchNews() {
    state.newsLoading = true;
    state.newsError = "";
    render();

    try {
        const response = await fetch("/api/news");

        if (!response.ok) {
            throw new Error("뉴스를 불러오지 못했습니다.");
        }

        state.newsItems = await response.json();
    } catch (error) {
        state.newsError = error.message;
    } finally {
        state.newsLoading = false;
        render();
    }
}

render();
fetchNews();
