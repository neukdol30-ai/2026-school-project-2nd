const { useMemo, useState } = React;

const initialWidgets = [
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
];

function App() {
    const [isEditMode, setIsEditMode] = useState(false);
    const [widgets, setWidgets] = useState(initialWidgets);
    const [memoText, setMemoText] = useState("");
    const [calculatorText, setCalculatorText] = useState("");
    const [currentUser, setCurrentUser] = useState(null);
    const [loginForm, setLoginForm] = useState({
        username: "",
        password: ""
    });

    const dayNames = ["월", "화", "수", "목", "금", "토", "일"];

    const mainWidgets = useMemo(() => {
        return widgets
            .filter((widget) => widget.visible && widget.zone === "main")
            .sort((a, b) => a.orderNo - b.orderNo);
    }, [widgets]);

    const sideWidgets = useMemo(() => {
        return widgets
            .filter((widget) => widget.visible && widget.zone === "side")
            .sort((a, b) => a.orderNo - b.orderNo);
    }, [widgets]);

    function toggleEditMode() {
        setIsEditMode((prev) => !prev);
    }

    function toggleWidget(id) {
        setWidgets((prevWidgets) =>
            prevWidgets.map((widget) =>
                widget.id === id
                    ? { ...widget, visible: !widget.visible }
                    : widget
            )
        );
    }

    function toggleCollapse(id) {
        setWidgets((prevWidgets) =>
            prevWidgets.map((widget) =>
                widget.id === id
                    ? { ...widget, collapsed: !widget.collapsed }
                    : widget
            )
        );
    }

    function getVisibleWidgetsByZone(widgetList, zone) {
        return widgetList
            .filter((widget) => widget.visible && widget.zone === zone)
            .sort((a, b) => a.orderNo - b.orderNo);
    }

    function moveWidget(id, direction) {
        setWidgets((prevWidgets) => {
            const targetWidget = prevWidgets.find((widget) => widget.id === id);

            if (!targetWidget) {
                return prevWidgets;
            }

            const sameZoneWidgets = getVisibleWidgetsByZone(prevWidgets, targetWidget.zone);
            const index = sameZoneWidgets.findIndex((widget) => widget.id === id);
            const nextIndex = direction === "up" ? index - 1 : index + 1;

            if (nextIndex < 0 || nextIndex >= sameZoneWidgets.length) {
                return prevWidgets;
            }

            const otherWidget = sameZoneWidgets[nextIndex];

            return prevWidgets.map((widget) => {
                if (widget.id === targetWidget.id) {
                    return { ...widget, orderNo: otherWidget.orderNo };
                }

                if (widget.id === otherWidget.id) {
                    return { ...widget, orderNo: targetWidget.orderNo };
                }

                return widget;
            });
        });
    }

    function mockLogin() {
        if (!loginForm.username) {
            alert("아이디를 입력하세요.");
            return;
        }

        setCurrentUser({
            username: loginForm.username,
            nickname: loginForm.username
        });

        setLoginForm((prev) => ({
            ...prev,
            password: ""
        }));
    }

    function mockLogout() {
        setCurrentUser(null);
    }

    function appendCalc(value) {
        setCalculatorText((prev) => prev + value);
    }

    function clearCalc() {
        setCalculatorText("");
    }

    function calculate() {
        try {
            if (!/^[0-9+\-*/.() ]+$/.test(calculatorText)) {
                setCalculatorText("Error");
                return;
            }

            const result = Function(`"use strict"; return (${calculatorText})`)();
            setCalculatorText(String(result));
        } catch (e) {
            setCalculatorText("Error");
        }
    }

    return (
        <div className="container">
            <div className="page-actions">
                <button onClick={toggleEditMode}>
                    {isEditMode ? "설정 완료" : "환경설정"}
                </button>
            </div>

            {isEditMode && (
                <section className="control-box">
                    <h2>위젯 관리</h2>

                    <div className="control-buttons">
                        {widgets.map((widget) => (
                            <button
                                key={widget.id}
                                className={widget.visible ? "active" : ""}
                                onClick={() => toggleWidget(widget.id)}
                            >
                                {widget.icon} {widget.title}
                            </button>
                        ))}
                    </div>
                </section>
            )}

            <main className="dashboard-layout">
                <section className="widget-column main-column">
                    <h2 className="column-title">메인 위젯</h2>

                    <div className="widget-list">
                        {mainWidgets.map((widget, index) => (
                            <WidgetCard
                                key={widget.id}
                                widget={widget}
                                index={index}
                                widgetCount={mainWidgets.length}
                                isEditMode={isEditMode}
                                onToggleCollapse={toggleCollapse}
                                onToggleWidget={toggleWidget}
                                onMoveWidget={moveWidget}
                            >
                                <WidgetContent
                                    widget={widget}
                                    memoText={memoText}
                                    setMemoText={setMemoText}
                                    calculatorText={calculatorText}
                                    appendCalc={appendCalc}
                                    clearCalc={clearCalc}
                                    calculate={calculate}
                                    dayNames={dayNames}
                                />
                            </WidgetCard>
                        ))}
                    </div>
                </section>

                <aside className="widget-column side-column">
                    <h2 className="column-title">보조 위젯</h2>

                    <AuthWidget
                        currentUser={currentUser}
                        loginForm={loginForm}
                        setLoginForm={setLoginForm}
                        mockLogin={mockLogin}
                        mockLogout={mockLogout}
                    />

                    <div className="widget-list">
                        {sideWidgets.map((widget, index) => (
                            <WidgetCard
                                key={widget.id}
                                widget={widget}
                                index={index}
                                widgetCount={sideWidgets.length}
                                isEditMode={isEditMode}
                                onToggleCollapse={toggleCollapse}
                                onToggleWidget={toggleWidget}
                                onMoveWidget={moveWidget}
                            >
                                <WidgetContent
                                    widget={widget}
                                    memoText={memoText}
                                    setMemoText={setMemoText}
                                    calculatorText={calculatorText}
                                    appendCalc={appendCalc}
                                    clearCalc={clearCalc}
                                    calculate={calculate}
                                    dayNames={dayNames}
                                />
                            </WidgetCard>
                        ))}
                    </div>
                </aside>
            </main>
        </div>
    );
}

function WidgetCard({
                        widget,
                        index,
                        widgetCount,
                        isEditMode,
                        onToggleCollapse,
                        onToggleWidget,
                        onMoveWidget,
                        children
                    }) {
    return (
        <article className={`widget ${widget.zone === "main" ? "main-widget" : "side-widget"}`}>
            <div className="widget-header">
                <div>
                    <div className="widget-title">
                        {widget.icon} {widget.title}
                    </div>
                    <div className="widget-desc">
                        {widget.description}
                    </div>
                </div>

                <div className="widget-actions">
                    <button onClick={() => onToggleCollapse(widget.id)}>
                        {widget.collapsed ? "펼치기" : "접기"}
                    </button>

                    {isEditMode && (
                        <>
                            <button
                                onClick={() => onMoveWidget(widget.id, "up")}
                                disabled={index === 0}
                            >
                                ↑
                            </button>

                            <button
                                onClick={() => onMoveWidget(widget.id, "down")}
                                disabled={index === widgetCount - 1}
                            >
                                ↓
                            </button>

                            <button
                                className="danger"
                                onClick={() => onToggleWidget(widget.id)}
                            >
                                숨김
                            </button>
                        </>
                    )}
                </div>
            </div>

            {!widget.collapsed && (
                <div className="widget-content">
                    {children}
                </div>
            )}
        </article>
    );
}

function WidgetContent({
                           widget,
                           memoText,
                           setMemoText,
                           calculatorText,
                           appendCalc,
                           clearCalc,
                           calculate,
                           dayNames
                       }) {
    if (widget.type === "news") {
        return (
            <ul className="news-list">
                <li className="news-item">AI 산업 관련 주요 뉴스</li>
                <li className="news-item">오늘의 경제 뉴스 요약</li>
                <li className="news-item">개발자 채용 시장 동향</li>
            </ul>
        );
    }

    if (widget.type === "issue") {
        return (
            <div className="issue-tags">
                <span className="issue-tag">#AI</span>
                <span className="issue-tag">#경제</span>
                <span className="issue-tag">#증권</span>
                <span className="issue-tag">#개발</span>
                <span className="issue-tag">#날씨</span>
            </div>
        );
    }

    if (widget.type === "schedule") {
        return (
            <ul className="schedule-list">
                <li>09:00 프로젝트 정리</li>
                <li>13:00 API 연동 테스트</li>
                <li>18:00 개인 학습 기록</li>
            </ul>
        );
    }

    if (widget.type === "stock") {
        return (
            <>
                <div className="stock-box">증권 차트 영역</div>
            </>
        );
    }

    if (widget.type === "weather") {
        return (
            <>
                <div className="weather-temp">24℃</div>
                <p>서울 · 구름 조금</p>
            </>
        );
    }

    if (widget.type === "calculator") {
        return (
            <>
                <input
                    className="calculator-display"
                    type="text"
                    value={calculatorText}
                    readOnly
                />

                <div className="calculator-buttons">
                    {["7", "8", "9", "/", "4", "5", "6", "*", "1", "2", "3", "-", "0"].map((value) => (
                        <button key={value} onClick={() => appendCalc(value)}>
                            {value === "/" ? "÷" : value === "*" ? "×" : value}
                        </button>
                    ))}

                    <button onClick={clearCalc}>C</button>
                    <button onClick={calculate}>=</button>
                    <button onClick={() => appendCalc("+")}>+</button>
                </div>
            </>
        );
    }

    if (widget.type === "miniCalendar") {
        return (
            <div className="calendar-grid">
                {dayNames.map((dayName) => (
                    <div key={dayName} className="calendar-cell">
                        <strong>{dayName}</strong>
                    </div>
                ))}

                {Array.from({ length: 31 }, (_, index) => index + 1).map((day) => (
                    <div key={day} className="calendar-cell">
                        {day}
                    </div>
                ))}
            </div>
        );
    }

    if (widget.type === "memo") {
        return (
            <>
        <textarea
            value={memoText}
            onChange={(event) => setMemoText(event.target.value)}
            placeholder="메모를 입력하세요"
        />

                <div className="memo-count">
                    글자 수: {memoText.length}
                </div>
            </>
        );
    }

    return null;
}

function AuthWidget({
                        currentUser,
                        loginForm,
                        setLoginForm,
                        mockLogin,
                        mockLogout
                    }) {
    return (
        <article className="widget side-widget auth-widget">
            <div className="widget-header">
                <div>
                    <div className="widget-title">🔐 계정</div>
                    <div className="widget-desc">
                        로그인하면 사용자 설정이 적용됩니다.
                    </div>
                </div>
            </div>

            {!currentUser ? (
                <div className="auth-content">
                    <input
                        className="auth-input"
                        type="text"
                        value={loginForm.username}
                        onChange={(event) =>
                            setLoginForm((prev) => ({
                                ...prev,
                                username: event.target.value
                            }))
                        }
                        placeholder="아이디"
                    />

                    <input
                        className="auth-input"
                        type="password"
                        value={loginForm.password}
                        onChange={(event) =>
                            setLoginForm((prev) => ({
                                ...prev,
                                password: event.target.value
                            }))
                        }
                        placeholder="비밀번호"
                    />

                    <button className="auth-login-button" onClick={mockLogin}>
                        로그인
                    </button>

                    <button className="auth-register-button">
                        회원가입
                    </button>
                </div>
            ) : (
                <div className="auth-content">
                    <p className="auth-user">
                        {currentUser.nickname}님 로그인 중
                    </p>

                    <button className="auth-login-button" onClick={mockLogout}>
                        로그아웃
                    </button>
                </div>
            )}
        </article>
    );
}

const root = ReactDOM.createRoot(document.getElementById("root"));
root.render(<App />);