/*
    메인 화면 오늘 일정 위젯

    기능
    - 오늘부터 예정된 일정 조회
    - 일정 날짜와 시간 표시
    - 로딩, 오류, 빈 일정 표시
*/


// 날짜·시간 문자열에서 HH:mm 추출
function extractScheduleTime(datetimeText) {

    const match =
        String(datetimeText || "")
            .match(/(\d{2}:\d{2})$/);

    return match
        ? match[1]
        : "";
}


// 21:30 → 오후 9:30 형식으로 변경
function formatScheduleTime(timeText) {

    if (!timeText) {
        return "";
    }

    const [
        hourText,
        minuteText
    ] = timeText.split(":");

    const hour =
        Number(hourText);

    const period =
        hour < 12
            ? "오전"
            : "오후";

    const displayHour =
        hour % 12 === 0
            ? 12
            : hour % 12;

    return (
        period
        + " "
        + displayHour
        + ":"
        + minuteText
    );
}


// 일정 날짜와 시간 표시 문구 생성
function getScheduleDateTimeText(schedule) {

    const dateText =
        schedule.eventDate
        || String(
            schedule.startDatetime || ""
        ).substring(0, 10);


    // 시간을 선택하지 않은 일정은 날짜만 표시
    if (schedule.allDayYn === "Y") {
        return dateText;
    }


    const startTime =
        extractScheduleTime(
            schedule.startDatetime
        );

    const endTime =
        extractScheduleTime(
            schedule.endDatetime
        );


    if (startTime && endTime) {

        return (
            dateText
            + " · "
            + formatScheduleTime(startTime)
            + " ~ "
            + formatScheduleTime(endTime)
        );
    }


    if (startTime) {

        return (
            dateText
            + " · "
            + formatScheduleTime(startTime)
        );
    }


    return dateText;
}

// 서버가 최근 등록·수정 순으로 반환한 오늘 이후 일정 최대 3개를 사용한다.
// 새로 추가한 일정이 항상 위에 보이도록 여기서 날짜·시간순으로 재정렬하지 않는다.
function getUpcomingScheduleItems() {

    const today =
        new Date();

    const todayText =
        today.getFullYear()
        + "-"
        + String(
            today.getMonth() + 1
        ).padStart(2, "0")
        + "-"
        + String(
            today.getDate()
        ).padStart(2, "0");


    return (
        Array.isArray(
            state.todayScheduleItems
        )
            ? [...state.todayScheduleItems]
            : []
    )
        .filter(
            (schedule) => {

                const dateText =
                    schedule.eventDate
                    || String(
                        schedule.startDatetime
                        || ""
                    ).substring(0, 10);

                return (
                    dateText
                    && dateText >= todayText
                );
            }
        )
        .slice(0, 3);
}


// 다가오는 일정 위젯 화면 생성
function renderScheduleWidget() {

    // 일정 조회 중
    if (state.todayScheduleLoading) {

        return `
            <p class="widget-desc">
                다가오는 일정을 불러오는 중입니다.
            </p>
        `;
    }


    // 일정 조회 오류
    if (state.todayScheduleError) {

        return `
            <p class="widget-desc">
                ${escapeHtml(
            state.todayScheduleError
        )}
            </p>
        `;
    }


    const upcomingScheduleItems =
        getUpcomingScheduleItems();


    // 등록된 예정 일정 없음
    if (
        upcomingScheduleItems.length === 0
    ) {

        return `
             <p class="widget-desc schedule-empty">
                예정된 일정이 없습니다.
            </p>
        `;
    }


    // 일정 목록 출력
    return `
        <ul class="schedule-list">

            ${upcomingScheduleItems
        .map(
            (schedule) => `
                        <li class="schedule-item">

                            <strong class="schedule-date-time">
                                ${escapeHtml(
                getScheduleDateTimeText(
                    schedule
                )
            )}
                            </strong>

                            <span class="schedule-title">
                                ${escapeHtml(
                schedule.title
                || "제목 없음"
            )}
                            </span>

                        </li>
                    `
        )
        .join("")}

        </ul>
    `;
}


// 오늘 일정 위젯 부분만 다시 출력
function refreshScheduleWidget() {

    const scheduleContent =
        document.querySelector(
            '[data-widget-id="3"] .widget-content'
        );

    if (!scheduleContent) {
        return;
    }


    scheduleContent.innerHTML =
        renderScheduleWidget();
}


// 오늘부터 예정된 일정 조회
async function fetchTodaySchedule(
    showLoading = false
) {

    // 비로그인 상태
    if (!state.currentUser) {

        state.todayScheduleItems =
            [];

        state.todayScheduleLoading =
            false;

        state.todayScheduleError =
            "";

        refreshScheduleWidget();

        return;
    }


    // 로딩 문구 표시
    if (showLoading) {

        state.todayScheduleLoading =
            true;

        state.todayScheduleError =
            "";

        refreshScheduleWidget();
    }


    try {

        const response =
            await fetch(
                "/api/calendar/today",
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
                || "오늘 일정을 불러오지 못했습니다."
            );
        }


        state.todayScheduleItems =
            data.eventList || [];

        state.todayScheduleError =
            "";

    } catch (error) {

        state.todayScheduleItems =
            [];

        state.todayScheduleError =
            error.message
            || "오늘 일정을 불러오지 못했습니다.";


        console.error(
            "오늘 일정 조회 실패:",
            error
        );

    } finally {

        state.todayScheduleLoading =
            false;

        refreshScheduleWidget();
    }
}
