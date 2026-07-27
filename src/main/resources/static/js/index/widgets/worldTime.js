// 세계시간 위젯 화면
function renderWorldTimeWidget() {
    const cities = Array.isArray(state.worldTimeCities)
        ? state.worldTimeCities
        : [];

    if (cities.length === 0) {
        return `
            <p class="widget-desc">
                표시할 도시가 없습니다.
            </p>
        `;
    }

    return `
        <div class="world-time-grid">
            ${cities.map((city) => `
                <article class="world-time-item">
                    <div class="world-time-place">
                        <strong>
                            ${escapeHtml(city.city)}
                        </strong>
                    
                        <span>
                            ${escapeHtml(city.country)}
                        </span>
                    </div>

                    <time
                        class="world-time-clock"
                        data-world-time-zone="${escapeHtml(city.timeZone)}"
                    >
                        --:--
                    </time>

                    <div class="world-time-zone">
                        <span>${escapeHtml(city.zoneLabel)}</span>
                        <span>${escapeHtml(city.utcOffset)}</span>
                    </div>
                </article>
            `).join("")}
        </div>
    `;
}

// 지정한 시간대의 시·분만 표시
function formatWorldTime(timeZone, now) {
    try {
        return new Intl.DateTimeFormat("ko-KR", {
            timeZone: timeZone,
            hour: "2-digit",
            minute: "2-digit",
            hour12: false
        }).format(now);
    } catch (error) {
        return "--:--";
    }
}

// 화면에 있는 세계시간 칸만 갱신
function updateWorldTimeWidget(now = new Date()) {
    document
        .querySelectorAll("[data-world-time-zone]")
        .forEach((clock) => {
            clock.textContent = formatWorldTime(
                clock.dataset.worldTimeZone,
                now
            );
        });
}

let worldTimeTimerId = null;

// 세계시간 타이머는 분이 바뀌는 시점에 맞춰 갱신
function startWorldTimeClock() {
    updateWorldTimeWidget();

    if (worldTimeTimerId !== null) {
        return;
    }

    const now = new Date();

    const delay =
        60000
        - now.getSeconds() * 1000
        - now.getMilliseconds();

    worldTimeTimerId = window.setTimeout(() => {
        updateWorldTimeWidget();

        worldTimeTimerId = window.setInterval(
            updateWorldTimeWidget,
            60000
        );
    }, delay);
}