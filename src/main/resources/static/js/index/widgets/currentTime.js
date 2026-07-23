let currentTimeTimerId = null;

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

            <div class="sun-time-list">
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

// 서울 현재 시간 갱신
function updateCurrentTimeWidget() {
    const now = new Date();

    const clockText = new Intl.DateTimeFormat("ko-KR", {
        timeZone: "Asia/Seoul",
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit",
        hour12: false
    }).format(now);

    const headerClockText = new Intl.DateTimeFormat("ko-KR", {
        timeZone: "Asia/Seoul",
        hour: "2-digit",
        minute: "2-digit",
        hour12: false
    }).format(now);

    const dateText = new Intl.DateTimeFormat("ko-KR", {
        timeZone: "Asia/Seoul",
        year: "numeric",
        month: "long",
        day: "numeric",
        weekday: "long"
    }).format(now);

    const widgetClock =
        document.querySelector("#current-time-clock");

    const widgetDate =
        document.querySelector("#current-time-date");

    const dashboardClock =
        document.querySelector("#dashboard-current-time");

    const dashboardDate =
        document.querySelector("#dashboard-current-date");

    if (widgetClock) {
        widgetClock.textContent = clockText;
    }

    if (widgetDate) {
        widgetDate.textContent = dateText;
    }

    if (dashboardClock) {
        dashboardClock.textContent = headerClockText;
    }

    if (dashboardDate) {
        dashboardDate.textContent = dateText;
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