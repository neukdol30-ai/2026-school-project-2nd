/*
    메인 화면 미니 캘린더 위젯

    기능
    - 월간 미니 캘린더 출력
    - 이전 달 / 다음 달 이동
    - 오늘 날짜 표시
    - 공휴일 표시
    - 일정이 있는 날짜에 점 표시
    - 월별 일정과 공휴일 API 조회
*/


// =========================
// 미니 캘린더 화면 출력
// =========================

function renderMiniCalendar() {

    const calendarDays =
        makeMiniCalendarDays();

    return `
        <div class="mini-calendar-header">

            <button
                type="button"
                class="mini-calendar-month-button"
                data-action="prev-calendar-month"
                aria-label="이전 달"
            >
                ‹
            </button>

            <strong class="mini-calendar-title">
                ${state.calendarYear}.
                ${state.calendarMonth + 1}.
            </strong>

            <button
                type="button"
                class="mini-calendar-month-button"
                data-action="next-calendar-month"
                aria-label="다음 달"
            >
                ›
            </button>

        </div>

        ${
        state.miniCalendarError
            ? `
                    <p class="mini-calendar-message error">
                        ${escapeHtml(state.miniCalendarError)}
                    </p>
                `
            : ""
    }

        <div class="mini-calendar-grid">

            ${state.dayNames
        .map(
            (dayName, index) => `
                        <div class="
                            mini-calendar-week-name
                            ${index === 0 ? "sunday" : ""}
                            ${index === 6 ? "saturday" : ""}
                        ">
                            ${dayName}
                        </div>
                    `,
        )
        .join("")}

            ${calendarDays
        .map((date) => {

            const titleText =
                date.holidayName
                || (
                    date.hasEvent
                        ? "등록된 일정이 있습니다."
                        : ""
                );

            return `
                        <button
                            type="button"
                            class="
                                mini-calendar-day
                                ${date.otherMonth ? "other-month" : ""}
                                ${date.today ? "today" : ""}
                                ${date.sunday ? "sunday" : ""}
                                ${date.saturday ? "saturday" : ""}
                                ${date.holiday ? "holiday" : ""}
                                ${date.hasEvent ? "has-event" : ""}
                            "
                            data-action="go-calendar-day"
                            data-date="${date.date}"
                            title="${escapeHtml(titleText)}"
                        >
                            <span class="mini-calendar-day-number">
                                ${date.day}
                            </span>

                            ${
                date.hasEvent
                    ? `
                                        <span
                                            class="mini-calendar-event-dot"
                                            aria-hidden="true">
                                        </span>
                                    `
                    : ""
            }
                        </button>
                    `;
        })
        .join("")}

        </div>
    `;
}


// =========================
// 날짜 생성
// =========================

// 숫자를 두 자리 문자로 변경
function pad(number) {

    return String(number)
        .padStart(2, "0");
}


// yyyy-MM-dd 형식 날짜 생성
function makeDateText(
    year,
    month,
    day
) {

    return (
        year
        + "-"
        + pad(month + 1)
        + "-"
        + pad(day)
    );
}


// 날짜 한 칸 정보 생성
function createMiniCalendarDay(
    date,
    otherMonth,
    todayText
) {

    const dateText =
        makeDateText(
            date.getFullYear(),
            date.getMonth(),
            date.getDate()
        );

    const serverDay =
        state.miniCalendarDayMap.get(
            dateText
        );

    return {

        day:
            date.getDate(),

        date:
        dateText,

        otherMonth:
        otherMonth,

        today:
            dateText === todayText,

        sunday:
            date.getDay() === 0,

        saturday:
            date.getDay() === 6,

        hasEvent:
            Boolean(serverDay?.hasEvent),

        holiday:
            Boolean(serverDay?.holiday),

        holidayName:
            serverDay?.holidayName || ""
    };
}


// 미니 캘린더 날짜 목록 생성
function makeMiniCalendarDays() {

    const result = [];

    const today =
        new Date();

    const todayText =
        makeDateText(
            today.getFullYear(),
            today.getMonth(),
            today.getDate()
        );


    // 이번 달 1일의 요일
    const firstDay =
        new Date(
            state.calendarYear,
            state.calendarMonth,
            1
        ).getDay();


    // 이번 달 마지막 날짜
    const lastDate =
        new Date(
            state.calendarYear,
            state.calendarMonth + 1,
            0
        ).getDate();


    // 이전 달 마지막 날짜
    const previousLastDate =
        new Date(
            state.calendarYear,
            state.calendarMonth,
            0
        ).getDate();


    // 이전 달 날짜
    for (
        let index = firstDay - 1;
        index >= 0;
        index--
    ) {

        const date =
            new Date(
                state.calendarYear,
                state.calendarMonth - 1,
                previousLastDate - index
            );

        result.push(
            createMiniCalendarDay(
                date,
                true,
                todayText
            )
        );
    }


    // 현재 달 날짜
    for (
        let day = 1;
        day <= lastDate;
        day++
    ) {

        const date =
            new Date(
                state.calendarYear,
                state.calendarMonth,
                day
            );

        result.push(
            createMiniCalendarDay(
                date,
                false,
                todayText
            )
        );
    }


    // 다음 달 날짜 개수
    const nextDayCount =
        result.length % 7 === 0
            ? 0
            : 7 - (
            result.length % 7
        );


    // 다음 달 날짜
    for (
        let day = 1;
        day <= nextDayCount;
        day++
    ) {

        const date =
            new Date(
                state.calendarYear,
                state.calendarMonth + 1,
                day
            );

        result.push(
            createMiniCalendarDay(
                date,
                true,
                todayText
            )
        );
    }


    return result;
}


// =========================
// 미니 캘린더 부분 갱신
// =========================

function refreshMiniCalendarWidget() {

    const miniCalendarContent =
        document.querySelector(
            '[data-widget-id="7"] .widget-content'
        );

    if (!miniCalendarContent) {
        return;
    }

    /*
        클릭 이벤트는 event.js가 #app에서 한 번만 처리한다.
        여기서 다시 연결하면 달 이동이 두 번 실행될 수 있다.
    */
    miniCalendarContent.innerHTML =
        renderMiniCalendar();
}


// =========================
// 월별 일정과 공휴일 조회
// =========================

async function fetchMiniCalendarMonthData(
    showLoading = false
) {

    const requestedYear =
        state.calendarYear;

    const requestedMonth =
        state.calendarMonth;


    if (showLoading) {

        state.miniCalendarLoading =
            true;

        state.miniCalendarError =
            "";
    }


    try {

        const response =
            await fetch(
                "/api/calendar/month-summary"
                + "?year="
                + requestedYear
                + "&month="
                + (requestedMonth + 1),
                {
                    method: "GET",
                    credentials: "same-origin",
                    cache: "no-store"
                }
            );


        const data =
            await response.json();


        if (
            !response.ok
            || data.success === false
        ) {

            throw new Error(
                data.message
                || "미니 캘린더를 불러오지 못했습니다."
            );
        }


        /*
            조회하는 동안 사용자가 다른 달로 이동했다면
            이전 달의 API 결과를 적용하지 않는다.
        */
        if (
            requestedYear
            !== state.calendarYear

            || requestedMonth
            !== state.calendarMonth
        ) {
            return;
        }


        state.miniCalendarDayMap =
            new Map(
                (data.calendarList || [])
                    .map(
                        (day) => [
                            String(day.date),
                            day
                        ]
                    )
            );


        state.googleCalendarConnected =
            data.googleConnected === true;


        /*
            static/index.html에서는 Thymeleaf 세션 값을 읽을 수 없으므로,
            월별 캘린더 API가 내려준 실제 Spring Security 로그인 상태를 사용한다.
        */
        const previousMemberId =
            state.currentUser?.username || "";

        if (data.loggedIn === true) {

            state.currentUser = {
                username:
                    data.memberId || "",

                nickname:
                    data.nickname
                    || data.memberId
                    || "회원"
            };

        } else {

            state.currentUser = null;
        }


        const currentMemberId =
            state.currentUser?.username || "";

        if (
            previousMemberId
            !== currentMemberId
        ) {
            // API를 통해 로그인 회원이 확인된 뒤
            // 해당 회원의 위젯 순서를 불러온다.
            if (currentMemberId) {
                loadWidgetOrderForCurrentUser();
            }

            render();
        }


        state.miniCalendarError =
            "";

    } catch (error) {

        if (
            requestedYear
            === state.calendarYear

            && requestedMonth
            === state.calendarMonth
        ) {

            state.miniCalendarDayMap =
                new Map();

            state.miniCalendarError =
                error.message
                || "미니 캘린더를 불러오지 못했습니다.";
        }


        console.error(
            "미니 캘린더 조회 실패:",
            error
        );

    } finally {

        if (
            requestedYear
            === state.calendarYear

            && requestedMonth
            === state.calendarMonth
        ) {

            state.miniCalendarLoading =
                false;

            refreshMiniCalendarWidget();
        }
    }
}


// =========================
// 일정이 있는 날짜만 갱신
// =========================

async function fetchMiniCalendarEventDates() {

    // 로그인하지 않은 경우 일정 점 제거
    if (!state.currentUser) {

        const clearedDayMap =
            new Map();


        state.miniCalendarDayMap
            .forEach(
                (day, dateText) => {

                    clearedDayMap.set(
                        dateText,
                        {
                            ...day,
                            hasEvent: false
                        }
                    );
                }
            );


        state.miniCalendarDayMap =
            clearedDayMap;


        refreshMiniCalendarWidget();

        return;
    }


    const requestedYear =
        state.calendarYear;

    const requestedMonth =
        state.calendarMonth;


    try {

        const response =
            await fetch(
                "/api/calendar/event-dates"
                + "?year="
                + requestedYear
                + "&month="
                + (requestedMonth + 1),
                {
                    method: "GET",
                    credentials: "same-origin",
                    cache: "no-store"
                }
            );


        const data =
            await response.json();


        if (
            !response.ok
            || data.success === false
        ) {

            throw new Error(
                data.message
                || "일정 날짜를 불러오지 못했습니다."
            );
        }


        if (
            requestedYear
            !== state.calendarYear

            || requestedMonth
            !== state.calendarMonth
        ) {
            return;
        }


        const eventDateSet =
            new Set(
                data.eventDateList || []
            );


        const updatedDayMap =
            new Map();


        state.miniCalendarDayMap
            .forEach(
                (day, dateText) => {

                    updatedDayMap.set(
                        dateText,
                        {
                            ...day,
                            hasEvent:
                                eventDateSet.has(
                                    dateText
                                )
                        }
                    );
                }
            );


        state.miniCalendarDayMap =
            updatedDayMap;


        refreshMiniCalendarWidget();

    } catch (error) {

        console.warn(
            "미니 캘린더 일정 점 조회 실패:",
            error
        );
    }

}