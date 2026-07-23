//캘린더 영역
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