//날씨 관련 위젯
function renderWeatherWidget() {
    if (state.weatherLoading) {
        return `<p class="widget-desc">날씨 정보를 불러오는 중입니다.</p>`;
    }

    if (state.weatherError) {
        return `<p class="widget-desc">${escapeHtml(state.weatherError)}</p>`;
    }

    if (!state.weather) {
        return `<p class="widget-desc">표시할 날씨 정보가 없습니다.</p>`;
    }

    return `
    <div class="weather-current">
        <div class="weather-main">
            <div>
                <div class="weather-location">${escapeHtml(state.weather.location)}</div>
                <div class="weather-text">${escapeHtml(state.weather.weatherText)}</div>
            </div>

            <div class="weather-temp">
                ${escapeHtml(state.weather.currentTemp)}℃
            </div>
        </div>

        <div class="weather-rain">
            강수확률 ${escapeHtml(state.weather.rainPercent)}%
        </div>
    </div>

    <ul class="weather-weekly">
        ${(state.weather.weekly ?? []).slice(1, 6).map((day) => `
            <li class="weather-day">
                <strong>${escapeHtml(day.day)}</strong>
                <span class="weather-icon">${getWeatherIcon(day.weatherText)}</span>
                <span class="weather-range">
                    ${escapeHtml(day.minTemp)}° / ${escapeHtml(day.maxTemp)}°
                </span>
            </li>
        `).join("")}
    </ul>
`;
}

//날씨 아이콘
function getWeatherIcon(weatherText) {
    const text = String(weatherText ?? "");

    if (text.includes("비")) {
        return "🌧️";
    }

    if (text.includes("흐림")) {
        return "☁️";
    }

    if (text.includes("구름")) {
        return "⛅";
    }

    if (text.includes("눈")) {
        return "❄️";
    }

    return "☀️";
}

//날씨 api함수
async function fetchWeather() {
    state.weatherLoading = true;
    state.weatherError = "";
    render();

    try {
        const response = await fetch("/api/weather");

        if (!response.ok) {
            throw new Error("날씨 정보를 불러오지 못했습니다.");
        }

        state.weather = await response.json();
    } catch (error) {
        state.weatherError = error.message;
    } finally {
        state.weatherLoading = false;
        render();
    }
}