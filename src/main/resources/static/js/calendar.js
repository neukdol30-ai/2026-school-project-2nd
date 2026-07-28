// 캘린더 기본 상태

const $ = (selector) => document.querySelector(selector);

const calendarPage = $(".calendar-page");
const today = new Date();

const googleConnected =
    calendarPage?.dataset.googleConnected === "true";

let selectedDateText =
    calendarPage?.dataset.selectedDate
    || makeDateText(
        today.getFullYear(),
        today.getMonth(),
        today.getDate()
    );

let currentYear =
    Number(calendarPage?.dataset.year)
    || today.getFullYear();

let currentMonth =
    (Number(calendarPage?.dataset.month)
        || today.getMonth() + 1) - 1;


// 달력 점 표시용 날짜
const eventDateSet = new Set(
    [...document.querySelectorAll(
        "#eventDateData [data-date]"
    )].map(
        (element) => element.dataset.date
    )
);


// 공휴일 날짜와 이름
const holidayMap = new Map(
    [...document.querySelectorAll(
        "#holidayDateData [data-date]"
    )].map(
        (element) => [
            element.dataset.date,
            element.dataset.name || ""
        ]
    )
);


// 수정 버튼에서 사용할 현재 일정 정보
const currentEventMap = new Map();


// 화면 요소

const element = {

    calendarTitle:
        $("#calendarTitle"),

    calendarDays:
        $("#calendarDays"),

    detailPanel:
        $("#calendarDetailPanel"),

    selectedDateTitle:
        $("#selectedDateTitle"),

    eventListArea:
        $("#eventListArea"),

    eventMessage:
        $("#eventMessage"),

    agendaTotalCount:
        $("#agendaTotalCount"),

    agendaGoogleCount:
        $("#agendaGoogleCount"),

    sidebarUpcomingList:
        $("#sidebarUpcomingList"),

    sidebarUpcomingCount:
        $("#sidebarUpcomingCount"),

    sidebarUpcomingMessage:
        $("#sidebarUpcomingMessage"),

    toggleWriteButton:
        $("#toggleEventWriteBtn"),

    cancelWriteButton:
        $("#cancelEventWriteBtn"),

    writeBox:
        $("#eventWriteBox"),

    writeForm:
        $("#eventWriteForm"),

    saveButton:
        $("#saveEventBtn"),

    eventDate:
        $("#eventDate"),

    eventTitle:
        $("#eventTitle"),

    eventContent:
        $("#eventContent"),

    eventLocation:
        $("#eventLocation"),

    startTime:
        $("#eventStartTime"),

    endTime:
        $("#eventEndTime")
};


// 일정 번호 hidden input이 없으면 자동 생성
element.eventNo =
    ensureEventNoInput();


function ensureEventNoInput() {

    if (!element.writeForm) {
        return null;
    }

    let input =
        element.writeForm.querySelector(
            "#eventNo"
        );

    if (!input) {

        input =
            document.createElement(
                "input"
            );

        input.type =
            "hidden";

        input.id =
            "eventNo";

        input.name =
            "no";

        element.writeForm.prepend(
            input
        );
    }

    return input;
}


// 공통 함수

function pad(number) {

    return String(number)
        .padStart(2, "0");
}


function makeDateText(
    year,
    month,
    day
) {

    return `${year}-${pad(month + 1)}-${pad(day)}`;
}


// 입력한 HTML 태그가 실행되지 않게 처리
function escapeHtml(value) {

    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}


// 날짜·시간에서 HH:mm 추출
function extractTime(datetimeText) {

    return String(datetimeText || "")
            .match(/(\d{2}:\d{2})$/)?.[1]
        || "";
}


// 21:30 → 오후 9:30
function formatTimeText(timeText) {

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

    return `${period} ${displayHour}:${minuteText}`;
}


// 일정 카드에 표시할 시간
function getEventTimeText(eventItem) {

    if (eventItem.allDayYn === "Y") {
        return "";
    }

    const startTime =
        extractTime(
            eventItem.startDatetime
        );

    const endTime =
        extractTime(
            eventItem.endDatetime
        );

    return startTime && endTime
        ? `${formatTimeText(startTime)} ~ ${formatTimeText(endTime)}`
        : formatTimeText(startTime);
}


// 성공 또는 오류 메시지
function showMessage(
    message,
    isError = false
) {

    if (!element.eventMessage) {
        return;
    }

    element.eventMessage.textContent =
        message || "";

    element.eventMessage.classList.toggle(
        "error",
        isError
    );

    element.eventMessage.hidden =
        !message;
}


// 처리 중 상태
function setBusy(isBusy) {

    calendarPage?.setAttribute(
        "aria-busy",
        String(isBusy)
    );

    if (element.saveButton) {

        element.saveButton.disabled =
            isBusy;
    }
}


// API JSON 응답 확인
async function readJsonResponse(response) {

    const data =
        await response.json();

    if (
        !response.ok
        || data.success === false
    ) {

        throw new Error(
            data.message
            || "요청 처리에 실패했습니다."
        );
    }

    return data;
}


// API 공통 요청
async function requestJson(
    url,
    options = {}
) {

    const response =
        await fetch(
            url,
            {
                credentials:
                    "same-origin",

                ...options
            }
        );

    return readJsonResponse(
        response
    );
}


// 메인 화면 일정 위젯에 변경 알림
function notifyCalendarChanged() {

    const message = {
        type: "calendar-changed",
        changedAt: Date.now()
    };


    window.dispatchEvent(
        new CustomEvent(
            "calendar-changed",
            {
                detail: message
            }
        )
    );


    // 다른 탭이나 창에 열린 메인 화면에 알림
    if ("BroadcastChannel" in window) {

        const channel =
            new BroadcastChannel(
                "calendar-events"
            );

        channel.postMessage(
            message
        );

        channel.close();
    }


    // BroadcastChannel 미지원 환경용
    try {

        localStorage.setItem(
            "calendar-changed-at",
            String(message.changedAt)
        );

    } catch (error) {

        console.warn(
            "[캘린더 변경 알림 저장 실패]",
            error
        );
    }
}


// 공통 배너와 화면 테마

const PORTAL_THEME_STORAGE_KEY = "portalTheme";
const LEGACY_CALENDAR_THEME_STORAGE_KEY = "calendar-theme";
const PORTAL_THEME_VALUES = new Set([
    "light",
    "dark"
]);

let calendarBannerUser = null;

function normalizePortalTheme(theme) {

    return PORTAL_THEME_VALUES.has(theme)
        ? theme
        : "light";
}


function getStoredPortalTheme() {

    try {

        const portalTheme =
            localStorage.getItem(
                PORTAL_THEME_STORAGE_KEY
            );

        if (
            PORTAL_THEME_VALUES.has(
                portalTheme
            )
        ) {
            return portalTheme;
        }


        const legacyTheme =
            localStorage.getItem(
                LEGACY_CALENDAR_THEME_STORAGE_KEY
            );

        if (
            PORTAL_THEME_VALUES.has(
                legacyTheme
            )
        ) {

            localStorage.setItem(
                PORTAL_THEME_STORAGE_KEY,
                legacyTheme
            );

            return legacyTheme;
        }

    } catch (error) {

        console.warn(
            "[공통 테마 불러오기 실패]",
            error
        );
    }


    return "light";
}


function updatePortalThemeButtons(theme) {

    document
        .querySelectorAll(
            "[data-theme-option]"
        )
        .forEach(
            (button) => {

                const isSelected =
                    button.dataset.themeOption
                    === theme;

                button.classList.toggle(
                    "is-selected",
                    isSelected
                );

                button.setAttribute(
                    "aria-pressed",
                    String(isSelected)
                );
            }
        );
}


function applyTheme(theme) {

    const nextTheme =
        normalizePortalTheme(theme);

    document.documentElement.dataset.theme =
        nextTheme;

    document.documentElement.style.colorScheme =
        nextTheme;

    updatePortalThemeButtons(
        nextTheme
    );
}


function setPortalTheme(theme) {

    const nextTheme =
        normalizePortalTheme(theme);

    applyTheme(
        nextTheme
    );


    try {

        localStorage.setItem(
            PORTAL_THEME_STORAGE_KEY,
            nextTheme
        );

        /*
            아직 이전 캘린더 파일을 사용하는 탭도
            같은 화면 모드를 유지하도록 함께 저장한다.
        */
        localStorage.setItem(
            LEGACY_CALENDAR_THEME_STORAGE_KEY,
            nextTheme
        );

    } catch (error) {

        console.warn(
            "[공통 테마 저장 실패]",
            error
        );
    }
}


function initializeTheme() {

    applyTheme(
        getStoredPortalTheme()
    );
}


function setCalendarServiceMenuOpen(isOpen) {

    const menu =
        document.querySelector(
            "[data-calendar-service-menu]"
        );

    if (!menu) {
        return;
    }


    const button =
        menu.querySelector(
            '[data-calendar-action="toggle-service-menu"]'
        );

    const panel =
        menu.querySelector(
            "[data-calendar-service-panel]"
        );

    if (
        !button
        || !panel
    ) {
        return;
    }


    menu.classList.toggle(
        "is-open",
        isOpen
    );

    button.setAttribute(
        "aria-expanded",
        String(isOpen)
    );

    button.setAttribute(
        "aria-label",
        isOpen
            ? "서비스 메뉴 닫기"
            : "서비스 메뉴 열기"
    );

    panel.hidden =
        !isOpen;
}


function setCalendarSettingsOpen(isOpen) {

    const layer =
        document.querySelector(
            "[data-calendar-settings-layer]"
        );

    if (!layer) {
        return;
    }


    layer.classList.toggle(
        "is-open",
        isOpen
    );

    layer.setAttribute(
        "aria-hidden",
        String(!isOpen)
    );

    document.body.classList.toggle(
        "calendar-settings-open",
        isOpen
    );

    if (isOpen) {

        setCalendarServiceMenuOpen(
            false
        );
    }
}


function getCalendarBannerDisplayName(user) {

    if (!user) {
        return "회원";
    }

    return String(
        user.displayName
        || user.nickname
        || user.name
        || user.memberId
        || user.username
        || "회원"
    ).trim();
}


function renderCalendarBannerUser() {

    const userMenu =
        document.querySelector(
            "[data-calendar-user-menu]"
        );

    const serviceUserArea =
        document.querySelector(
            "[data-calendar-service-user]"
        );

    if (
        !userMenu
        || !serviceUserArea
    ) {
        return;
    }


    if (!calendarBannerUser) {

        userMenu.innerHTML = `
            <a href="/member/login">
                로그인
            </a>
        `;

        serviceUserArea.innerHTML = `
            <a
                class="global-service-login-link"
                href="/member/login"
            >
                <strong>로그인하세요</strong>
                <span aria-hidden="true">›</span>
            </a>

            <p class="global-service-login-desc">
                로그인하고 여러 서비스를 편리하게 이용하세요.
            </p>
        `;

        return;
    }


    const displayName =
        getCalendarBannerDisplayName(
            calendarBannerUser
        );

    const avatarText =
        displayName.charAt(0)
        || "회";


    userMenu.innerHTML = `
        <button
            type="button"
            data-mypage-open
        >
            마이페이지
        </button>
    `;


    serviceUserArea.innerHTML = `
        <div class="global-service-profile">

            <button
                class="global-service-profile-button"
                type="button"
                data-mypage-open
            >
                <span
                    class="global-service-avatar"
                    aria-hidden="true"
                >
                    ${escapeHtml(avatarText)}
                </span>

                <span class="global-service-profile-text">
                    <strong>
                        ${escapeHtml(displayName)}님
                    </strong>
                    <span>마이페이지로 이동</span>
                </span>

                <span
                    class="global-service-profile-arrow"
                    aria-hidden="true"
                >
                    ›
                </span>
            </button>

            <form
                class="global-service-logout-form"
                action="/member/logout"
                method="post"
            >
                <button
                    class="global-service-logout-button"
                    type="submit"
                >
                    로그아웃
                </button>
            </form>

        </div>
    `;
}


async function loadCalendarBannerUser() {

    try {

        const response =
            await fetch(
                "/mypage/me",
                {
                    credentials:
                        "same-origin",

                    cache:
                        "no-store",

                    headers: {
                        "Accept":
                            "application/json",

                        "X-Requested-With":
                            "XMLHttpRequest"
                    }
                }
            );


        if (!response.ok) {

            calendarBannerUser =
                null;

            renderCalendarBannerUser();

            return;
        }


        const data =
            await response.json();


        calendarBannerUser =
            data?.loggedIn
            && data?.profile
                ? data.profile
                : null;


        renderCalendarBannerUser();

    } catch (error) {

        calendarBannerUser =
            null;

        renderCalendarBannerUser();

        console.warn(
            "[캘린더 배너 로그인 상태 조회 실패]",
            error
        );
    }
}


function handleCalendarGlobalClick(event) {

    const actionButton =
        event.target.closest(
            "[data-calendar-action]"
        );


    if (actionButton) {

        const action =
            actionButton.dataset.calendarAction;


        if (
            action
            === "toggle-service-menu"
        ) {

            const menu =
                actionButton.closest(
                    "[data-calendar-service-menu]"
                );

            setCalendarServiceMenuOpen(
                !menu?.classList.contains(
                    "is-open"
                )
            );

            return;
        }


        if (
            action
            === "open-settings"
        ) {

            setCalendarSettingsOpen(
                true
            );

            return;
        }


        if (
            action
            === "close-settings"
        ) {

            setCalendarSettingsOpen(
                false
            );

            return;
        }


        if (
            action
            === "set-theme"
        ) {

            setPortalTheme(
                actionButton.dataset.value
            );

            return;
        }
    }


    if (
        event.target.closest(
            "[data-calendar-service-link]"
        )
    ) {

        setCalendarServiceMenuOpen(
            false
        );

        return;
    }


    const menu =
        document.querySelector(
            "[data-calendar-service-menu]"
        );


    if (
        menu?.classList.contains(
            "is-open"
        )
        && !event.target.closest(
            "[data-calendar-service-menu]"
        )
    ) {

        setCalendarServiceMenuOpen(
            false
        );
    }
}


function handleCalendarGlobalKeydown(event) {

    if (event.key !== "Escape") {
        return;
    }

    setCalendarServiceMenuOpen(
        false
    );

    setCalendarSettingsOpen(
        false
    );
}


document.addEventListener(
    "click",
    handleCalendarGlobalClick
);


document.addEventListener(
    "keydown",
    handleCalendarGlobalKeydown
);


window.addEventListener(
    "storage",
    function (event) {

        if (
            event.key
            !== PORTAL_THEME_STORAGE_KEY
        ) {
            return;
        }

        applyTheme(
            event.newValue
        );
    }
);


initializeTheme();
renderCalendarBannerUser();
loadCalendarBannerUser();


// 월간 달력

function renderCalendar() {

    if (
        !element.calendarTitle
        || !element.calendarDays
    ) {
        return;
    }


    element.calendarTitle.textContent =
        `${currentYear}. ${currentMonth + 1}.`;

    element.calendarDays.innerHTML =
        "";


    const monthWrap =
        document.createElement(
            "div"
        );

    monthWrap.className =
        "month-wrap";


    const firstDay =
        new Date(
            currentYear,
            currentMonth,
            1
        ).getDay();

    const lastDate =
        new Date(
            currentYear,
            currentMonth + 1,
            0
        ).getDate();

    const previousLastDate =
        new Date(
            currentYear,
            currentMonth,
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
                currentYear,
                currentMonth - 1,
                previousLastDate - index
            );

        monthWrap.appendChild(
            createDayButton(
                date,
                true
            )
        );
    }


    // 현재 달 날짜
    for (
        let day = 1;
        day <= lastDate;
        day++
    ) {

        monthWrap.appendChild(
            createDayButton(
                new Date(
                    currentYear,
                    currentMonth,
                    day
                ),
                false
            )
        );
    }


    // 다음 달 날짜
    const remain =
        monthWrap.children.length % 7 === 0
            ? 0
            : 7 - (
            monthWrap.children.length % 7
        );


    for (
        let day = 1;
        day <= remain;
        day++
    ) {

        monthWrap.appendChild(
            createDayButton(
                new Date(
                    currentYear,
                    currentMonth + 1,
                    day
                ),
                true
            )
        );
    }


    element.calendarDays.appendChild(
        monthWrap
    );
}


// 날짜 버튼 생성
function createDayButton(
    date,
    isOtherMonth
) {

    const dateText =
        makeDateText(
            date.getFullYear(),
            date.getMonth(),
            date.getDate()
        );

    const todayText =
        makeDateText(
            today.getFullYear(),
            today.getMonth(),
            today.getDate()
        );


    const button =
        document.createElement(
            "button"
        );

    button.type =
        "button";

    button.className =
        "calendar-day";

    button.dataset.date =
        dateText;

    // 일요일과 토요일 날짜 색상 구분
    if (date.getDay() === 0) {
        button.classList.add(
            "sunday"
        );
    }

    if (date.getDay() === 6) {
        button.classList.add(
            "saturday"
        );
    }


    if (isOtherMonth) {

        button.classList.add(
            "other-month"
        );
    }

    if (dateText === todayText) {

        button.classList.add(
            "today"
        );
    }

    if (dateText === selectedDateText) {

        button.classList.add(
            "selected"
        );
    }


    // 날짜 숫자
    const dayNumber =
        document.createElement(
            "span"
        );

    dayNumber.className =
        "calendar-day-number";

    dayNumber.textContent =
        date.getDate();

    button.appendChild(
        dayNumber
    );


    // 공휴일 이름
    const holidayName =
        holidayMap.get(
            dateText
        );

    if (holidayName) {

        // 공휴일 날짜 숫자도 빨간색으로 표시
        button.classList.add(
            "holiday"
        );

        const holidayElement =
            document.createElement(
                "span"
            );

        holidayElement.className =
            "calendar-holiday-name";

        holidayElement.textContent =
            holidayName;

        button.appendChild(
            holidayElement
        );
    }


    // 일정 점
    if (eventDateSet.has(dateText)) {

        const eventDot =
            document.createElement(
                "span"
            );

        eventDot.className =
            "calendar-event-dot";

        button.appendChild(
            eventDot
        );
    }


    button.addEventListener(
        "click",
        async function () {

            const clickedYear =
                date.getFullYear();

            const clickedMonth =
                date.getMonth();

            const isAnotherMonth =
                clickedYear !== currentYear
                || clickedMonth !== currentMonth;


            // 이전 달이나 다음 달 날짜를 누른 경우
            if (isAnotherMonth) {

                currentYear =
                    clickedYear;

                currentMonth =
                    clickedMonth;

                try {

                    await loadMonthEventDates();

                } catch (error) {

                    showMessage(
                        error.message
                        || "달력 정보를 불러오지 못했습니다.",
                        true
                    );

                    return;
                }
            }


            await selectDate(
                dateText
            );
        }
    );

    return button;
}


// 선택 날짜 변경
async function selectDate(
    dateText,
    shouldScrollToDetail = true
) {

    selectedDateText =
        dateText;

    if (element.selectedDateTitle) {
        element.selectedDateTitle.textContent =
            dateText;
    }

    if (element.eventDate) {
        element.eventDate.value =
            dateText;
    }

    // 날짜를 바꾸면 수정 상태 초기화
    resetEventForm();

    if (element.writeBox) {
        element.writeBox.hidden =
            true;
    }

    history.replaceState(
        null,
        "",
        "/calendar?date="
        + encodeURIComponent(
            dateText
        )
    );

    renderCalendar();

    if (element.detailPanel) {
        element.detailPanel.hidden =
            false;
    }

    await loadEvents(
        dateText
    );

    // 날짜를 직접 클릭한 경우에만 일정 영역으로 이동
    if (
        shouldScrollToDetail
        && element.detailPanel
    ) {
        element.detailPanel.scrollIntoView({
            behavior: "smooth",
            block: "start"
        });
    }
}


// 이전 또는 다음 달 이동
async function moveMonth(monthAmount) {

    const date =
        new Date(
            currentYear,
            currentMonth + monthAmount,
            1
        );

    currentYear =
        date.getFullYear();

    currentMonth =
        date.getMonth();

    const firstDateText =
        makeDateText(
            currentYear,
            currentMonth,
            1
        );

    try {

        setBusy(true);

        // 이동한 달의 일정 점과 공휴일 조회
        await loadMonthEventDates();

        // 월 이동 버튼에서는 일정 영역으로 자동 이동하지 않음
        await selectDate(
            firstDateText,
            false
        );

    } catch (error) {

        showMessage(
            error.message
            || "달력 정보를 불러오지 못했습니다.",
            true
        );

    } finally {

        setBusy(false);
    }
}


$("#prevMonth")
    ?.addEventListener(
        "click",
        () => moveMonth(-1)
    );


$("#nextMonth")
    ?.addEventListener(
        "click",
        () => moveMonth(1)
    );


document.addEventListener(
    "click",
    async function (event) {

        const upcomingButton =
            event.target.closest(
                "[data-upcoming-date]"
            );

        if (!upcomingButton) {
            return;
        }

        const dateText =
            upcomingButton.dataset.upcomingDate;

        if (!dateText) {
            return;
        }

        const [year, month] =
            dateText
                .split("-")
                .map(Number);

        currentYear = year;
        currentMonth = month - 1;

        await loadMonthEventDates();
        await selectDate(dateText);
    }
);


// 일정 조회와 목록 출력

async function loadEvents(
    dateText,
    showLoading = true
) {

    if (showLoading) {
        setBusy(true);
    }


    try {

        const data =
            await requestJson(
                "/api/calendar/events?date="
                + encodeURIComponent(dateText),
                {
                    method: "GET",
                    cache: "no-store"
                }
            );


        const eventList =
            data.eventList || [];


        renderEventList(
            eventList,
            dateText
        );


        updateEventDate(
            dateText,
            eventList.length > 0
        );

    } catch (error) {

        showMessage(
            error.message,
            true
        );

    } finally {

        if (showLoading) {
            setBusy(false);
        }
    }
}


// 일정 목록 출력
// 오늘 이후 일정을 날짜별로 묶고, 같은 날짜 안에서는 시간순으로 정렬한다.
function renderEventList(
    eventList,
    selectedDate
) {

    if (!element.eventListArea) {
        return;
    }

    const sortedEventList =
        [...(eventList || [])]
            .sort(compareAgendaEvents);

    currentEventMap.clear();

    sortedEventList.forEach(
        (eventItem) => {
            currentEventMap.set(
                Number(eventItem.no),
                eventItem
            );
        }
    );

    const googleCount =
        sortedEventList.filter(
            (eventItem) =>
                eventItem.sourceType === "GOOGLE"
        ).length;

    if (element.agendaTotalCount) {
        element.agendaTotalCount.textContent =
            String(sortedEventList.length);
    }

    if (element.agendaGoogleCount) {
        element.agendaGoogleCount.textContent =
            String(googleCount);
    }

    if (sortedEventList.length === 0) {
        element.eventListArea.innerHTML = `
            <div class="empty-box">
                <div class="empty-icon">📅</div>
                <div>
                    <p>예정된 일정이 없습니다.</p>
                    <p>새 일정을 추가해보세요.</p>
                </div>
            </div>
        `;
        return;
    }

    const groupedEvents =
        new Map();

    sortedEventList.forEach(
        (eventItem) => {

            const dateText =
                getEventDateText(eventItem);

            if (!groupedEvents.has(dateText)) {
                groupedEvents.set(
                    dateText,
                    []
                );
            }

            groupedEvents.get(dateText)
                .push(eventItem);
        }
    );

    element.eventListArea.innerHTML = `
        <div class="event-list">
            ${
        [...groupedEvents.entries()]
            .map(
                ([dateText, events]) =>
                    createAgendaDateGroupHtml(
                        dateText,
                        events,
                        selectedDate
                    )
            )
            .join("")
    }
        </div>
    `;
}


function getEventDateText(eventItem) {

    const datetimeText =
        String(
            eventItem.startDatetime
            || eventItem.eventDate
            || ""
        );

    return datetimeText
        .substring(0, 10);
}


function compareAgendaEvents(
    first,
    second
) {

    const firstDate =
        getEventDateText(first);

    const secondDate =
        getEventDateText(second);

    if (firstDate !== secondDate) {
        return firstDate.localeCompare(
            secondDate
        );
    }

    const firstTime =
        first.allDayYn === "Y"
            ? "00:00"
            : extractTime(
                first.startDatetime
            );

    const secondTime =
        second.allDayYn === "Y"
            ? "00:00"
            : extractTime(
                second.startDatetime
            );

    return firstTime.localeCompare(
        secondTime
    );
}


function renderSidebarUpcoming(eventList) {

    if (
        !element.sidebarUpcomingList
        || !element.sidebarUpcomingCount
        || !element.sidebarUpcomingMessage
    ) {
        return;
    }

    const upcomingList =
        [...(eventList || [])]
            .slice(0, 5);

    element.sidebarUpcomingCount.textContent =
        `${upcomingList.length}개`;

    if (upcomingList.length === 0) {

        element.sidebarUpcomingMessage.textContent =
            "예정된 일정이 없습니다.";

        element.sidebarUpcomingList.innerHTML =
            "";

        return;
    }

    element.sidebarUpcomingMessage.textContent =
        "최근 추가한 예정 일정 5개입니다.";

    element.sidebarUpcomingList.innerHTML =
        upcomingList
            .map((eventItem) => {

                const dateText =
                    getEventDateText(eventItem);

                const timeText =
                    getEventTimeText(eventItem)
                    || "시간 미지정";

                const sourceText =
                    eventItem.sourceType === "GOOGLE"
                        ? "Google Calendar"
                        : "우리 사이트";

                return `
                    <button
                        type="button"
                        class="sidebar-upcoming-item"
                        data-upcoming-date="${escapeHtml(dateText)}"
                    >
                        <span class="sidebar-upcoming-date">
                            ${escapeHtml(formatAgendaDate(dateText))}
                        </span>

                        <strong class="sidebar-upcoming-title">
                            ${escapeHtml(
                    eventItem.title
                    || "제목 없음"
                )}
                        </strong>

                        <span class="sidebar-upcoming-time">
                            ${escapeHtml(timeText)}
                        </span>

                        <span class="sidebar-upcoming-source">
                            ${escapeHtml(sourceText)}
                        </span>
                    </button>
                `;
            })
            .join("");
}


async function loadSidebarUpcoming(
    showLoading = true
) {

    if (
        !element.sidebarUpcomingList
        || !element.sidebarUpcomingCount
        || !element.sidebarUpcomingMessage
    ) {
        return;
    }

    if (showLoading) {
        element.sidebarUpcomingMessage.textContent =
            "예정 일정을 불러오는 중입니다.";
    }

    try {

        const data =
            await requestJson(
                "/api/calendar/today",
                {
                    method: "GET",
                    cache: "no-store"
                }
            );

        renderSidebarUpcoming(
            data.eventList || []
        );

    } catch (error) {

        element.sidebarUpcomingCount.textContent =
            "0개";

        element.sidebarUpcomingMessage.textContent =
            error.message
            || "예정 일정을 불러오지 못했습니다.";

        element.sidebarUpcomingList.innerHTML =
            "";
    }
}


function formatAgendaDate(
    dateText
) {

    const [year, month, day] =
        dateText
            .split("-")
            .map(Number);

    const date =
        new Date(
            year,
            month - 1,
            day
        );

    const dayNames =
        ["일", "월", "화", "수", "목", "금", "토"];

    return `${month}월 ${day}일 (${dayNames[date.getDay()]})`;
}


function createAgendaDateGroupHtml(
    dateText,
    eventList,
    selectedDate
) {

    const selectedClass =
        dateText === selectedDate
            ? " is-selected"
            : "";

    return `
        <section class="agenda-date-group${selectedClass}"
                 data-agenda-date="${escapeHtml(dateText)}">

            <div class="agenda-date-heading">
                <span class="agenda-date-label">
                    ${escapeHtml(formatAgendaDate(dateText))}
                </span>
                <span class="agenda-date-count">
                    ${eventList.length}개
                </span>
            </div>

            ${
        eventList
            .map(
                (eventItem) =>
                    createEventCardHtml(
                        eventItem,
                        dateText
                    )
            )
            .join("")
    }
        </section>
    `;
}


// 일정 카드 생성
function createEventCardHtml(
    eventItem,
    dateText
) {

    const eventNo =
        Number(eventItem.no);

    const sourceText =
        eventItem.sourceType === "GOOGLE"
            ? "Google Calendar"
            : "우리 사이트에서 등록";

    const timeText =
        getEventTimeText(eventItem)
        || "시간 미지정";

    return `
        <article class="event-card agenda-event-card">

            <span class="agenda-event-dot"
                  aria-hidden="true"></span>

            <div class="agenda-event-content">
                <p class="agenda-event-time">
                    ${escapeHtml(timeText)}
                </p>

                <h4>
                    ${escapeHtml(
        eventItem.title
        || "제목 없음"
    )}
                </h4>

                ${
        eventItem.content
            ? `
                            <p class="agenda-event-description">
                                ${escapeHtml(eventItem.content)}
                            </p>
                        `
            : ""
    }

                ${
        eventItem.location
            ? `
                            <p class="agenda-event-location">
                                장소: ${escapeHtml(eventItem.location)}
                            </p>
                        `
            : ""
    }

                <p class="agenda-event-source">
                    ${escapeHtml(sourceText)}
                </p>
            </div>

            <div class="agenda-event-actions">
                <button type="button"
                        class="event-edit-btn"
                        data-event-no="${eventNo}">
                    수정
                </button>

                <form class="event-delete-form">
                    <input type="hidden"
                           name="no"
                           value="${eventNo}">
                    <input type="hidden"
                           name="date"
                           value="${escapeHtml(dateText)}">
                    <button type="submit"
                            class="event-delete-btn"
                            aria-label="일정 삭제">
                        삭제
                    </button>
                </form>
            </div>
        </article>
    `;
}


// 일정 점 추가 또는 제거
function updateEventDate(
    dateText,
    hasEvent
) {

    hasEvent
        ? eventDateSet.add(dateText)
        : eventDateSet.delete(dateText);

    renderCalendar();
}


// 현재 달의 일정과 공휴일 정보 조회
async function loadMonthEventDates() {

    const data =
        await requestJson(
            "/api/calendar/month-summary"
            + "?year="
            + currentYear
            + "&month="
            + (currentMonth + 1),
            {
                method: "GET",
                cache: "no-store"
            }
        );

    eventDateSet.clear();
    holidayMap.clear();

    (data.calendarList || [])
        .forEach(
            (dayItem) => {

                const dateText =
                    dayItem.date;

                if (!dateText) {
                    return;
                }

                if (dayItem.hasEvent) {

                    eventDateSet.add(
                        dateText
                    );
                }

                if (
                    dayItem.holiday
                    && dayItem.holidayName
                ) {

                    holidayMap.set(
                        dateText,
                        dayItem.holidayName
                    );
                }
            }
        );

    renderCalendar();
}


// 일정 추가와 수정

/*
    hidden 시간값과 커스텀 시간 선택기의
    버튼 문구를 함께 변경한다.
*/
function setTimePickerValue(
    input,
    value
) {

    if (!input) {
        return;
    }


    const checkedValue =
        value || "";

    input.value =
        checkedValue;


    const picker =
        input.closest(
            "[data-time-picker]"
        );

    if (!picker) {
        return;
    }


    const valueText =
        picker.querySelector(
            ".time-picker-value"
        );

    const openButton =
        picker.querySelector(
            ".time-picker-button"
        );

    const panel =
        picker.querySelector(
            ".time-picker-panel"
        );

    const directInput =
        picker.querySelector(
            ".time-direct-input"
        );


    if (valueText) {

        valueText.textContent =
            checkedValue
                ? formatTimeText(
                    checkedValue
                )
                : "시간 선택";
    }


    openButton?.classList.toggle(
        "is-selected",
        Boolean(checkedValue)
    );


    openButton?.setAttribute(
        "aria-expanded",
        "false"
    );


    if (panel) {
        panel.hidden = true;
    }

    if (directInput) {
        directInput.value = "";
    }


    picker
        .querySelectorAll(
            ".time-option"
        )
        .forEach(
            (optionButton) => {

                optionButton.classList.toggle(
                    "is-selected",
                    optionButton.dataset.value
                    === checkedValue
                );
            }
        );
}



// 시간 선택 목록 생성
function createTimePickerOptions(picker) {

    const optionList =
        picker?.querySelector(
            ".time-option-list"
        );

    if (!optionList) {
        return;
    }


    // 다시 초기화돼도 같은 버튼을 중복 생성하지 않는다.
    if (
        optionList.querySelector(
            ".time-option"
        )
    ) {
        return;
    }


    const fragment =
        document.createDocumentFragment();


    for (
        let totalMinute = 0;
        totalMinute < 24 * 60;
        totalMinute += 30
    ) {

        const hour =
            Math.floor(
                totalMinute / 60
            );

        const minute =
            totalMinute % 60;

        const value =
            `${pad(hour)}:${pad(minute)}`;


        const optionButton =
            document.createElement(
                "button"
            );

        optionButton.type =
            "button";

        optionButton.className =
            "time-option";

        optionButton.dataset.value =
            value;

        optionButton.textContent =
            formatTimeText(
                value
            );


        fragment.appendChild(
            optionButton
        );
    }


    optionList.appendChild(
        fragment
    );
}


// 시간 선택기를 닫는다.
function closeTimePicker(picker) {

    if (!picker) {
        return;
    }


    const button =
        picker.querySelector(
            ".time-picker-button"
        );

    const panel =
        picker.querySelector(
            ".time-picker-panel"
        );


    if (panel) {
        panel.hidden = true;
    }


    button?.setAttribute(
        "aria-expanded",
        "false"
    );
}


// 지정한 선택기를 제외하고 모두 닫는다.
function closeAllTimePickers(
    exceptPicker = null
) {

    document
        .querySelectorAll(
            "[data-time-picker]"
        )
        .forEach(
            (picker) => {

                if (
                    picker
                    === exceptPicker
                ) {
                    return;
                }

                closeTimePicker(
                    picker
                );
            }
        );
}


// 직접 입력한 시간을 HH:mm 형식으로 정리한다.
function normalizeDirectTime(value) {

    const match =
        String(value || "")
            .trim()
            .match(
                /^(\d{1,2}):(\d{2})$/
            );

    if (!match) {
        return "";
    }


    const hour =
        Number(match[1]);

    const minute =
        Number(match[2]);


    if (
        hour < 0
        || hour > 23
        || minute < 0
        || minute > 59
    ) {
        return "";
    }


    return `${pad(hour)}:${pad(minute)}`;
}


// 시간 선택기 하나를 초기화한다.
function initializeTimePicker(picker) {

    if (
        !picker
        || picker.dataset.timePickerInitialized
        === "true"
    ) {
        return;
    }


    const hiddenInput =
        picker.querySelector(
            "#eventStartTime, "
            + "#eventEndTime, "
            + "input[type=\"hidden\"]"
        );

    const openButton =
        picker.querySelector(
            ".time-picker-button"
        );

    const panel =
        picker.querySelector(
            ".time-picker-panel"
        );

    const optionList =
        picker.querySelector(
            ".time-option-list"
        );

    const directInput =
        picker.querySelector(
            ".time-direct-input"
        );

    const directApplyButton =
        picker.querySelector(
            ".time-direct-apply"
        );

    const clearButton =
        picker.querySelector(
            ".time-clear-button"
        );


    if (
        !hiddenInput
        || !openButton
        || !panel
        || !optionList
    ) {
        console.warn(
            "[시간 선택기 초기화 실패]",
            picker
        );

        return;
    }


    picker.dataset.timePickerInitialized =
        "true";


    createTimePickerOptions(
        picker
    );


    openButton.addEventListener(
        "click",
        function (event) {

            event.preventDefault();
            event.stopPropagation();


            // 화면 생성 시 누락됐어도 클릭할 때 다시 확인한다.
            createTimePickerOptions(
                picker
            );


            const willOpen =
                panel.hidden;


            closeAllTimePickers(
                picker
            );


            panel.hidden =
                !willOpen;


            openButton.setAttribute(
                "aria-expanded",
                String(willOpen)
            );


            if (!willOpen) {
                return;
            }


            requestAnimationFrame(
                function () {

                    const selectedOption =
                        optionList.querySelector(
                            ".time-option.is-selected"
                        );


                    selectedOption
                        ?.scrollIntoView({
                            block: "nearest"
                        });
                }
            );
        }
    );


    optionList.addEventListener(
        "click",
        function (event) {

            const optionButton =
                event.target.closest(
                    ".time-option"
                );


            if (!optionButton) {
                return;
            }


            const value =
                optionButton.dataset.value
                || "";


            setTimePickerValue(
                hiddenInput,
                value
            );


            closeTimePicker(
                picker
            );
        }
    );


    function applyDirectTime() {

        const normalizedValue =
            normalizeDirectTime(
                directInput?.value
            );


        if (!normalizedValue) {

            showMessage(
                "시간을 HH:mm 형식으로 입력해주세요.",
                true
            );

            directInput?.focus();

            return;
        }


        setTimePickerValue(
            hiddenInput,
            normalizedValue
        );


        closeTimePicker(
            picker
        );

        showMessage("");
    }


    directApplyButton
        ?.addEventListener(
            "click",
            function (event) {

                event.preventDefault();

                applyDirectTime();
            }
        );


    directInput
        ?.addEventListener(
            "keydown",
            function (event) {

                if (
                    event.key
                    !== "Enter"
                ) {
                    return;
                }


                event.preventDefault();

                applyDirectTime();
            }
        );


    clearButton
        ?.addEventListener(
            "click",
            function (event) {

                event.preventDefault();


                setTimePickerValue(
                    hiddenInput,
                    ""
                );


                closeTimePicker(
                    picker
                );

                showMessage("");
            }
        );
}


// 시작 시간과 종료 시간 선택기를 모두 연결한다.
function initializeCalendarTimePickers() {

    document
        .querySelectorAll(
            "[data-time-picker]"
        )
        .forEach(
            initializeTimePicker
        );
}


// 선택기 밖을 누르면 목록을 닫는다.
document.addEventListener(
    "click",
    function (event) {

        if (
            event.target.closest(
                "[data-time-picker]"
            )
        ) {
            return;
        }


        closeAllTimePickers();
    }
);


// Esc 키로 시간 목록을 닫는다.
document.addEventListener(
    "keydown",
    function (event) {

        if (
            event.key
            !== "Escape"
        ) {
            return;
        }


        closeAllTimePickers();
    }
);


// 폼을 일정 추가 상태로 초기화
function resetEventForm() {

    element.writeForm?.reset();


    if (element.eventNo) {

        element.eventNo.value =
            "";
    }

    if (element.eventDate) {

        element.eventDate.value =
            selectedDateText;
    }


    setTimePickerValue(
        element.startTime,
        ""
    );

    setTimePickerValue(
        element.endTime,
        ""
    );


    if (element.saveButton) {

        element.saveButton.textContent =
            "저장";
    }
}


// 기존 일정 내용을 수정 폼에 입력
function openEventEditForm(eventItem) {

    if (
        !eventItem
        || !element.writeForm
        || !element.writeBox
    ) {
        return;
    }


    if (element.eventNo) {

        element.eventNo.value =
            String(eventItem.no);
    }


    if (element.eventDate) {

        element.eventDate.value =
            eventItem.eventDate
            || selectedDateText;
    }


    if (element.eventTitle) {

        element.eventTitle.value =
            eventItem.title
            || "";
    }


    if (element.eventContent) {

        element.eventContent.value =
            eventItem.content
            || "";
    }


    if (element.eventLocation) {

        element.eventLocation.value =
            eventItem.location
            || "";
    }


    const startTime =
        eventItem.allDayYn === "Y"
            ? ""
            : extractTime(
                eventItem.startDatetime
            );


    const endTime =
        eventItem.allDayYn === "Y"
            ? ""
            : extractTime(
                eventItem.endDatetime
            );


    setTimePickerValue(
        element.startTime,
        startTime
    );

    setTimePickerValue(
        element.endTime,
        endTime
    );


    if (element.saveButton) {

        element.saveButton.textContent =
            "수정 저장";
    }


    element.writeBox.hidden =
        false;


    element.writeBox.scrollIntoView({
        behavior: "smooth",
        block: "center"
    });


    element.eventTitle?.focus();

    showMessage("");
}


// 일정 추가 버튼
element.toggleWriteButton
    ?.addEventListener(
        "click",
        function () {

            const willOpen =
                Boolean(
                    element.writeBox?.hidden
                );


            resetEventForm();


            if (element.writeBox) {

                element.writeBox.hidden =
                    !willOpen;
            }


            if (willOpen) {

                element.eventTitle
                    ?.focus();
            }
        }
    );


// 취소 버튼
element.cancelWriteButton
    ?.addEventListener(
        "click",
        function () {

            resetEventForm();


            if (element.writeBox) {

                element.writeBox.hidden =
                    true;
            }


            showMessage("");
        }
    );


// 수정 버튼
document.addEventListener(
    "click",
    function (event) {

        const editButton =
            event.target.closest(
                ".event-edit-btn"
            );


        if (!editButton) {
            return;
        }


        const eventItem =
            currentEventMap.get(
                Number(
                    editButton.dataset.eventNo
                )
            );


        if (!eventItem) {

            showMessage(
                "수정할 일정 정보를 찾을 수 없습니다.",
                true
            );

            return;
        }


        openEventEditForm(
            eventItem
        );
    }
);


// 일정 추가 또는 수정 저장
element.writeForm
    ?.addEventListener(
        "submit",
        async function (event) {

            event.preventDefault();


            const startTime =
                element.startTime?.value
                || "";

            const endTime =
                element.endTime?.value
                || "";


            // 시작과 종료 시간은 함께 선택
            if (
                Boolean(startTime)
                !== Boolean(endTime)
            ) {

                showMessage(
                    "시작 시간과 종료 시간을 모두 선택해주세요.",
                    true
                );

                return;
            }


            // 종료 시간 검사
            if (
                startTime
                && endTime
                && endTime <= startTime
            ) {

                showMessage(
                    "종료 시간은 시작 시간보다 늦어야 합니다.",
                    true
                );

                return;
            }


            const isEditMode =
                Boolean(
                    element.eventNo?.value
                );


            const requestUrl =
                isEditMode
                    ? "/api/calendar/events/update"
                    : "/api/calendar/events";


            setBusy(true);

            showMessage("");


            try {

                const data =
                    await requestJson(
                        requestUrl,
                        {
                            method: "POST",

                            body:
                                new FormData(
                                    element.writeForm
                                )
                        }
                    );


                const eventList =
                    data.eventList || [];


                updateEventDate(
                    selectedDateText,
                    eventList.length > 0
                );

                await loadEvents(
                    selectedDateText,
                    false
                );

                await loadSidebarUpcoming(
                    false
                );


                resetEventForm();

                element.writeBox.hidden =
                    true;


                showMessage(
                    data.message
                    || (
                        isEditMode
                            ? "일정이 수정되었습니다."
                            : "일정이 등록되었습니다."
                    )
                );


                notifyCalendarChanged();

            } catch (error) {

                showMessage(
                    error.message,
                    true
                );

            } finally {

                setBusy(false);
            }
        }
    );


// 일정 삭제

document.addEventListener(
    "submit",
    async function (event) {

        const deleteForm =
            event.target.closest(
                ".event-delete-form"
            );


        if (!deleteForm) {
            return;
        }


        event.preventDefault();


        if (
            !confirm(
                "이 일정을 삭제할까요?"
            )
        ) {
            return;
        }


        setBusy(true);

        showMessage("");


        try {

            const data =
                await requestJson(
                    "/api/calendar/events/delete",
                    {
                        method: "POST",

                        body:
                            new FormData(
                                deleteForm
                            )
                    }
                );


            const eventList =
                data.eventList || [];


            renderEventList(
                eventList,
                selectedDateText
            );


            updateEventDate(
                selectedDateText,
                eventList.length > 0
            );

            await loadSidebarUpcoming(
                false
            );


            resetEventForm();


            if (element.writeBox) {

                element.writeBox.hidden =
                    true;
            }


            showMessage(
                data.message
                || "일정이 삭제되었습니다."
            );


            notifyCalendarChanged();

        } catch (error) {

            showMessage(
                error.message,
                true
            );

        } finally {

            setBusy(false);
        }
    }
);


// 구글 일정 자동 동기화

const GOOGLE_AUTO_SYNC_INTERVAL =
    5000;


let googleAutoSyncRunning =
    false;


async function autoSyncGoogleCalendar() {

    if (
        !googleConnected
        || document.hidden
        || googleAutoSyncRunning
    ) {
        return;
    }


    googleAutoSyncRunning =
        true;


    try {

        await requestJson(
            "/api/calendar/auto-sync",
            {
                method: "POST",
                cache: "no-store"
            }
        );


        // 달력 점과 현재 날짜 일정 갱신
        await loadMonthEventDates();

        if (element.detailPanel && !element.detailPanel.hidden) {
            await loadEvents(
                selectedDateText,
                false
            );
        }

        await loadSidebarUpcoming(
            false
        );


        notifyCalendarChanged();

    } catch (error) {

        console.warn(
            "[구글 일정 자동 동기화 실패]",
            error
        );

    } finally {

        googleAutoSyncRunning =
            false;
    }
}


// 최초 실행

initializeCalendarTimePickers();
renderCalendar();

if (element.detailPanel) {
    element.detailPanel.hidden = true;
}

loadSidebarUpcoming(
    false
);


// 구글 연동 상태에서만 자동 동기화 실행
if (googleConnected) {

    setInterval(
        autoSyncGoogleCalendar,
        GOOGLE_AUTO_SYNC_INTERVAL
    );


    window.addEventListener(
        "focus",
        autoSyncGoogleCalendar
    );


    document.addEventListener(
        "visibilitychange",
        function () {

            if (!document.hidden) {

                autoSyncGoogleCalendar();
            }
        }
    );


    setTimeout(
        autoSyncGoogleCalendar,
        500
    );
}