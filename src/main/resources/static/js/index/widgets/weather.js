
let selectedWeatherDayIndex = -1;
// 날씨 관련 위젯
function renderWeatherWidget() {
    if (state.weatherLoading) {
        return `<p class="widget-desc">날씨 정보를 불러오는 중입니다.</p>`;
    }

    if (state.weatherError) {
        return `
            <p class="widget-desc">
                ${escapeHtml(state.weatherError)}
            </p>
        `;
    }

    if (!state.weather) {
        return `<p class="widget-desc">표시할 날씨 정보가 없습니다.</p>`;
    }

    const weekly =
        state.weather.weekly ?? [];

    const hasSelectedDay =
        Number.isInteger(selectedWeatherDayIndex)
        && selectedWeatherDayIndex >= 0
        && selectedWeatherDayIndex < weekly.length;

    const selectedDay =
        hasSelectedDay
            ? weekly[selectedWeatherDayIndex]
            : null;

    const displayWeatherText =
        selectedDay
            ? selectedDay.weatherText
            : state.weather.weatherText;

    const weatherThemeClass =
        getWeatherThemeClass(
            displayWeatherText
        );

    return `
        <div class="weather-current ${weatherThemeClass}">
            <div class="weather-main">
                <div class="weather-current-copy">
                    <div class="weather-current-heading">
                        <div class="weather-location">
                            ${escapeHtml(state.weather.location)}
                        </div>

                        <button
                            type="button"
                            class="weather-current-reset ${
        selectedDay
            ? ""
            : "is-active"
    }"
                            data-action="select-weather-day"
                            data-value="-1"
                        >
                            현재
                        </button>
                    </div>

                    <div class="weather-text">
                        ${
        selectedDay
            ? `
                                    ${escapeHtml(selectedDay.day)}요일 ·
                                    ${escapeHtml(selectedDay.weatherText)}
                                `
            : escapeHtml(state.weather.weatherText)
    }
                    </div>

                    <div class="weather-rain">
                        ${
        selectedDay
            ? `
                                    선택한 날짜 예보
                                `
            : `
                                    강수확률
                                    <strong>
                                        ${escapeHtml(state.weather.rainPercent)}%
                                    </strong>
                                `
    }
                    </div>
                </div>

                <div class="weather-current-side">
                    <span
                        class="weather-current-icon"
                        aria-hidden="true"
                    >
                        ${getWeatherIcon(displayWeatherText)}
                    </span>

                    <div class="weather-temp ${
                        selectedDay
                            ? "is-forecast"
                            : ""
                    }">
                    ${
                            selectedDay
                                ? `
                                    <div class="weather-forecast-high">
                                        ${escapeHtml(selectedDay.maxTemp)}°
                                    </div>
                    
                                    <div class="weather-forecast-low">
                                        최저 ${escapeHtml(selectedDay.minTemp)}°
                                    </div>
                                `
                                : `
                                    ${escapeHtml(state.weather.currentTemp)}℃
                                `
                        }
                    </div>
                </div>
            </div>
        </div>

        <ul class="weather-weekly">
            ${weekly.map((day, index) => {
        const isSelected =
            selectedWeatherDayIndex === index;

        return `
                    <li class="weather-day-item">
                        <button
                            type="button"
                            class="weather-day ${
            isSelected
                ? "is-selected"
                : ""
        }"
                            data-action="select-weather-day"
                            data-value="${index}"
                            aria-pressed="${String(isSelected)}"
                            title="${escapeHtml(day.day)}요일 날씨 보기"
                        >
                            <strong>
                                ${escapeHtml(day.day)}
                            </strong>

                            <span
                                class="weather-icon"
                                aria-hidden="true"
                            >
                                ${getWeatherIcon(day.weatherText)}
                            </span>

                            <span class="weather-range">
                                ${escapeHtml(day.minTemp)}° /
                                ${escapeHtml(day.maxTemp)}°
                            </span>
                        </button>
                    </li>
                `;
    }).join("")}
        </ul>
    `;
}

// 주간 날씨 선택
function selectWeatherDay(index) {
    const weekly =
        state.weather?.weekly ?? [];

    if (index === -1) {
        selectedWeatherDayIndex = -1;
        refreshWidgetContent(5);
        return;
    }

    if (
        !Number.isInteger(index)
        || index < 0
        || index >= weekly.length
    ) {
        return;
    }

    // 선택한 날짜를 한 번 더 누르면 현재 날씨로 복귀
    selectedWeatherDayIndex =
        selectedWeatherDayIndex === index
            ? -1
            : index;

    refreshWidgetContent(5);
}

// 현재 날씨에 맞는 배경 테마
function getWeatherThemeClass(weatherText) {
    const text = String(weatherText ?? "");

    if (text.includes("눈")) {
        return "weather-theme-snow";
    }

    if (
        text.includes("비")
        || text.includes("소나기")
        || text.includes("뇌우")
    ) {
        return "weather-theme-rain";
    }

    if (
        text.includes("흐림")
        || text.includes("구름")
    ) {
        return "weather-theme-cloudy";
    }

    return "weather-theme-sunny";
}

function getWeatherIcon(weatherText) {
    const text = String(weatherText ?? "");

    if (
        text.includes("뇌우")
        || text.includes("번개")
    ) {
        return getThunderWeatherIcon();
    }

    if (text.includes("눈")) {
        return getSnowWeatherIcon();
    }

    if (
        text.includes("비")
        || text.includes("소나기")
    ) {
        return getRainWeatherIcon();
    }

    if (text.includes("흐림")) {
        return getCloudWeatherIcon();
    }

    if (text.includes("구름")) {
        return getPartlyCloudyWeatherIcon();
    }

    return getSunnyWeatherIcon();
}

function getSunnyWeatherIcon() {
    return `
        <svg viewBox="0 0 64 64" aria-hidden="true" class="weather-svg weather-svg-sunny">
            <circle cx="32" cy="32" r="12" fill="#FDB813"></circle>

            <g stroke="#FDB813" stroke-width="3" stroke-linecap="round">
                <line x1="32" y1="6" x2="32" y2="14"></line>
                <line x1="32" y1="50" x2="32" y2="58"></line>
                <line x1="6" y1="32" x2="14" y2="32"></line>
                <line x1="50" y1="32" x2="58" y2="32"></line>
                <line x1="13" y1="13" x2="19" y2="19"></line>
                <line x1="45" y1="45" x2="51" y2="51"></line>
                <line x1="13" y1="51" x2="19" y2="45"></line>
                <line x1="45" y1="19" x2="51" y2="13"></line>
            </g>
        </svg>
    `;
}

function getCloudWeatherIcon() {
    return `
        <svg viewBox="0 0 64 64" aria-hidden="true" class="weather-svg weather-svg-cloud">
            <g fill="#C9DDE8">
                <circle cx="25" cy="33" r="11"></circle>
                <circle cx="36" cy="27" r="14"></circle>
                <circle cx="47" cy="34" r="10"></circle>
                <rect x="17" y="34" width="38" height="12" rx="6"></rect>
            </g>
        </svg>
    `;
}

function getPartlyCloudyWeatherIcon() {
    return `
        <svg viewBox="0 0 64 64" aria-hidden="true" class="weather-svg weather-svg-partly">
            <circle cx="23" cy="22" r="10" fill="#FDB813"></circle>

            <g stroke="#FDB813" stroke-width="2.5" stroke-linecap="round">
                <line x1="23" y1="7" x2="23" y2="12"></line>
                <line x1="23" y1="32" x2="23" y2="37"></line>
                <line x1="8" y1="22" x2="13" y2="22"></line>
                <line x1="33" y1="22" x2="38" y2="22"></line>
                <line x1="12" y1="12" x2="16" y2="16"></line>
                <line x1="30" y1="30" x2="34" y2="34"></line>
                <line x1="12" y1="32" x2="16" y2="28"></line>
                <line x1="30" y1="14" x2="34" y2="10"></line>
            </g>

            <g fill="#7CC7F3">
                <circle cx="28" cy="37" r="11"></circle>
                <circle cx="39" cy="31" r="14"></circle>
                <circle cx="50" cy="38" r="10"></circle>
                <rect x="20" y="38" width="38" height="12" rx="6"></rect>
            </g>
        </svg>
    `;
}

function getRainWeatherIcon() {
    return `
        <svg viewBox="0 0 64 64" aria-hidden="true" class="weather-svg weather-svg-rain">
            <g fill="#55B7EA">
                <circle cx="25" cy="28" r="11"></circle>
                <circle cx="36" cy="22" r="14"></circle>
                <circle cx="47" cy="29" r="10"></circle>
                <rect x="17" y="29" width="38" height="12" rx="6"></rect>
            </g>

            <g fill="#2F9EDD">
                <path d="M24 46c0-3 4-6 4-9 0 3 4 6 4 9a4 4 0 1 1-8 0z"></path>
                <path d="M34 50c0-3 4-6 4-9 0 3 4 6 4 9a4 4 0 1 1-8 0z"></path>
                <path d="M44 46c0-3 4-6 4-9 0 3 4 6 4 9a4 4 0 1 1-8 0z"></path>
            </g>
        </svg>
    `;
}

function getSnowWeatherIcon() {
    return `
        <svg viewBox="0 0 64 64" aria-hidden="true" class="weather-svg weather-svg-snow">
            <g fill="#C9DDE8">
                <circle cx="25" cy="28" r="11"></circle>
                <circle cx="36" cy="22" r="14"></circle>
                <circle cx="47" cy="29" r="10"></circle>
                <rect x="17" y="29" width="38" height="12" rx="6"></rect>
            </g>

            <g stroke="#3FA9E6" stroke-width="2.2" stroke-linecap="round">
                <g transform="translate(24 49)">
                    <line x1="-4" y1="0" x2="4" y2="0"></line>
                    <line x1="0" y1="-4" x2="0" y2="4"></line>
                    <line x1="-3" y1="-3" x2="3" y2="3"></line>
                    <line x1="-3" y1="3" x2="3" y2="-3"></line>
                </g>
                <g transform="translate(36 52)">
                    <line x1="-4" y1="0" x2="4" y2="0"></line>
                    <line x1="0" y1="-4" x2="0" y2="4"></line>
                    <line x1="-3" y1="-3" x2="3" y2="3"></line>
                    <line x1="-3" y1="3" x2="3" y2="-3"></line>
                </g>
                <g transform="translate(48 49)">
                    <line x1="-4" y1="0" x2="4" y2="0"></line>
                    <line x1="0" y1="-4" x2="0" y2="4"></line>
                    <line x1="-3" y1="-3" x2="3" y2="3"></line>
                    <line x1="-3" y1="3" x2="3" y2="-3"></line>
                </g>
            </g>
        </svg>
    `;
}

function getThunderWeatherIcon() {
    return `
        <svg viewBox="0 0 64 64" aria-hidden="true" class="weather-svg weather-svg-thunder">
            <g fill="#55B7EA">
                <circle cx="25" cy="28" r="11"></circle>
                <circle cx="36" cy="22" r="14"></circle>
                <circle cx="47" cy="29" r="10"></circle>
                <rect x="17" y="29" width="38" height="12" rx="6"></rect>
            </g>

            <path
                d="M35 41h8l-6 9h6l-13 14 4-11h-6z"
                fill="#F4BD19"
            ></path>
        </svg>
    `;
}

// 날씨 API 함수
async function fetchWeather() {
    state.weatherLoading = true;
    state.weatherError = "";

    refreshWidgetContent(5);

    try {
        const response = await fetch("/api/weather");

        if (!response.ok) {
            throw new Error("날씨 정보를 불러오지 못했습니다.");
        }

        state.weather = await response.json();
        selectedWeatherDayIndex = -1;
    } catch (error) {
        state.weatherError = error.message;
    } finally {
        state.weatherLoading = false;

        refreshWidgetContent(5);
    }
}