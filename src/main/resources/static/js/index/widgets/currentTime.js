let currentTimeTimerId = null;

let selectedCurrentTimeZone =
    "Asia/Seoul";

let selectedCurrentTimeCity =
    "서울";

let selectedCurrentTimeCountry =
    "대한민국";

// 현재 선택된 시간대
function getSelectedCurrentTimeZone() {
    return selectedCurrentTimeZone;
}

// 현재 선택된 도시
function getSelectedCurrentTimeCity() {
    return selectedCurrentTimeCity;
}

// 시간대가 브라우저에서 사용 가능한지 확인
function isValidCurrentTimeZone(timeZone) {
    try {
        new Intl.DateTimeFormat(
            "ko-KR",
            {
                timeZone: timeZone
            }
        ).format();

        return true;
    } catch (error) {
        return false;
    }
}

// 현재시간 제목의 기준 도시 갱신
function updateCurrentTimeBasisBadge() {
    const title =
        document.querySelector(
            "[data-header-widget] .widget-title"
        );

    if (!title) {
        return;
    }

    title.dataset.currentTimeBasis =
        `${selectedCurrentTimeCity} 기준`;
}

// 세계시간에서 선택한 도시를 현재시간에 적용
function selectCurrentTimeCity(
    city,
    country,
    timeZone
) {
    if (
        !city
        || !timeZone
        || !isValidCurrentTimeZone(
            timeZone
        )
    ) {
        return;
    }

    selectedCurrentTimeCity =
        city;

    selectedCurrentTimeCountry =
        country || "";

    selectedCurrentTimeZone =
        timeZone;

    updateCurrentTimeWidget();
    updateCurrentTimeBasisBadge();

    if (
        typeof updateWorldTimeSelection
        === "function"
    ) {
        updateWorldTimeSelection();
    }
}


// 현재시간 위젯 화면
function renderCurrentTimeWidget() {
    const sunTime = state.sunTime ?? {
        sunrise: "--:--",
        sunset: "--:--"
    };
    return `
        <div class="current-time-widget">
            <div
                id="current-time-date"
                class="current-time-date"
            >
                ----년 --월 --일
            </div>

            <div
                id="current-time-clock"
                class="current-time-clock"
            >
                --:--:--
            </div>

            <div
                class="sun-time-list"
                title="일출·일몰은 서울 기준입니다."
                aria-label="서울 기준 일출과 일몰"
            >
                <div class="sun-time-item">
                    <span>일출</span>
                    <strong id="sunrise-time">${escapeHtml(sunTime.sunrise)}</strong>
                </div>

                <div class="sun-time-item">
                    <span>일몰</span>
                    <strong id="sunset-time">${escapeHtml(sunTime.sunset)}</strong>
                </div>
            </div>
        </div>
    `;
}

// 선택한 도시의 현재 시간 갱신
function updateCurrentTimeWidget() {
    const now = new Date();

    const clock =
        document.querySelector(
            "#current-time-clock"
        );

    const date =
        document.querySelector(
            "#current-time-date"
        );

    if (!clock || !date) {
        return;
    }

    try {
        clock.textContent =
            new Intl.DateTimeFormat(
                "ko-KR",
                {
                    timeZone:
                    selectedCurrentTimeZone,
                    hour: "2-digit",
                    minute: "2-digit",
                    second: "2-digit",
                    hourCycle: "h23"
                }
            ).format(now);

        date.textContent =
            new Intl.DateTimeFormat(
                "ko-KR",
                {
                    timeZone:
                    selectedCurrentTimeZone,
                    year: "numeric",
                    month: "long",
                    day: "numeric",
                    weekday: "short"
                }
            ).format(now);

        updateCurrentTimeBasisBadge();
    } catch (error) {
        clock.textContent =
            "--:--:--";

        date.textContent =
            "시간 정보를 표시할 수 없습니다.";
    }
}

// 타이머는 한 번만 생성
function startCurrentTimeClock() {
    updateCurrentTimeWidget();

    if (currentTimeTimerId !== null) {
        return;
    }

    currentTimeTimerId = window.setInterval(
        updateCurrentTimeWidget,
        1000
    );
}
//api 호출 함수
async function fetchSunTime() {
    try {
        const response = await fetch("/api/time/sun");

        if (!response.ok) {
            throw new Error("일출·일몰 정보를 불러오지 못했습니다.");
        }

        state.sunTime = await response.json();
        updateSunTimeWidget();

    } catch (error) {
        console.error(error);
    }
}

function updateSunTimeWidget() {
    const sunrise = document.querySelector("#sunrise-time");
    const sunset = document.querySelector("#sunset-time");

    if (!sunrise || !sunset) {
        return;
    }

    sunrise.textContent = state.sunTime.sunrise;
    sunset.textContent = state.sunTime.sunset;
}